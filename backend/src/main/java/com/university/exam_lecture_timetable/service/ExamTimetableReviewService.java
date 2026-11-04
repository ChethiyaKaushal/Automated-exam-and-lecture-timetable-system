package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Exam;
import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Slot;
import com.university.exam_lecture_timetable.service.ExamSchedulingInput.Venue;
import com.university.exam_lecture_timetable.service.ExamTimetableDraftService.Draft;
import com.university.exam_lecture_timetable.service.ExamTimetableDraftService.ScheduledExam;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ExamTimetableReviewService {

    private final ExamSchedulingInputService inputs;

    public ExamTimetableReviewService(ExamSchedulingInputService inputs) {
        this.inputs = inputs;
    }

    public Review review(Long periodId, List<StartAssignment> assignments) {
        return review(inputs.loadDraft(periodId), assignments);
    }

    static Review review(ExamSchedulingInputService.DraftInput loaded, List<StartAssignment> assignments) {
        var input = loaded.input();
        List<Issue> issues = new ArrayList<>();
        var exams = input.exams().stream().collect(Collectors.toMap(Exam::requirementId, Function.identity()));
        List<Slot> slots = input.timeslots();
        Map<Long, Integer> slotIndexes = new HashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            slotIndexes.put(slots.get(i).timeslotId(), i);
        }

        Map<Long, Placement> placements = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for (StartAssignment assignment : assignments) {
            if (assignment == null || assignment.requirementId() == null) {
                issues.add(error("INVALID_ASSIGNMENT", "Each edit needs an exam and a start time."));
                continue;
            }
            Long id = assignment.requirementId();
            Exam exam = exams.get(id);
            if (exam == null || !seen.add(id)) {
                issues.add(error("UNKNOWN_OR_REPEATED_EXAM", "Exam " + id + " is inactive, unknown or repeated.", id));
                continue;
            }
            if (assignment.startTimeslotId() == null) {
                issues.add(error("MISSING_EXAM", label(exam) + " has no assigned time.", id));
                continue;
            }
            Integer index = slotIndexes.get(assignment.startTimeslotId());
            Window window = index == null ? null : windowAt(slots, index, exam.durationMinutes());
            if (window == null) {
                issues.add(error("EXAM_DOES_NOT_FIT", label(exam) + " needs " + exam.durationMinutes()
                        + " uninterrupted minutes at the selected start. Choose another slot in this period.", id));
            } else {
                placements.put(id, new Placement(exam, window));
            }
        }
        for (Exam exam : input.exams()) {
            if (!seen.contains(exam.requirementId())) {
                issues.add(error("MISSING_EXAM", label(exam) + " has no assigned time.", exam.requirementId()));
            }
        }

        for (var conflict : input.studentConflicts()) {
            var first = placements.get(conflict.firstRequirementId());
            var second = placements.get(conflict.secondRequirementId());
            if (first == null || second == null || !first.window().day().equals(second.window().day())) {
                continue;
            }
            String pair = label(first.exam()) + " and " + label(second.exam());
            Long a = first.exam().requirementId();
            Long b = second.exam().requirementId();
            if (first.window().overlaps(second.window())) {
                issues.add(error("STUDENT_OR_GROUP_CLASH", pair + " overlap and share a student or group.", a, b));
            } else {
                issues.add(new Issue("WARNING", "SAME_DAY_WORKLOAD", pair
                        + " share a student or group and are on the same day. Consider spacing them out.", List.of(a, b)));
            }
        }

        var fixed = checkDrawing(input, placements, issues);
        List<ScheduledExam> scheduled = allocateRooms(input, placements, fixed, issues);
        if (!loaded.membershipComplete()) {
            issues.add(new Issue("WARNING", "STUDENT_CLASHES_UNVERIFIED",
                    "Individual student clashes and workload cannot be fully checked. "
                            + "Review reported repeat-student clashes manually before saving this draft.", List.of()));
        }
        boolean valid = issues.stream().noneMatch(issue -> "ERROR".equals(issue.severity()));
        return new Review(valid, loaded.membershipComplete(), List.copyOf(issues), scheduled);
    }

    private static Window windowAt(List<Slot> slots, int index, int duration) {
        Slot first = slots.get(index);
        LocalDate day = LocalDate.parse(first.date());
        LocalTime start = LocalTime.parse(first.startTime());
        LocalTime end = start;
        List<Long> ids = new ArrayList<>();
        for (int i = index; i < slots.size(); i++) {
            Slot next = slots.get(i);
            if (!next.date().equals(first.date()) || !LocalTime.parse(next.startTime()).equals(end)) {
                break;
            }
            end = LocalTime.parse(next.endTime());
            ids.add(next.timeslotId());
            long minutes = Duration.between(start, end).toMinutes();
            if (minutes == duration) {
                return new Window(day, start, end, List.copyOf(ids));
            }
            if (minutes > duration) {
                break;
            }
        }
        return null;
    }

    private static Map<Long, Venue> checkDrawing(
            ExamSchedulingInput input, Map<Long, Placement> placements, List<Issue> issues
    ) {
        Map<Long, Venue> fixed = new HashMap<>();
        LocalDate drawingDay = null;
        for (var assignment : DrawingExamRule.assignments(input)) {
            Long id = assignment.exam().requirementId();
            fixed.put(id, assignment.venue());
            var placement = placements.get(id);
            if (placement == null) {
                continue;
            }
            var window = placement.window();
            if (!window.start().equals(assignment.start())
                    || (drawingDay != null && !drawingDay.equals(window.day()))) {
                issues.add(error("DRAWING_SESSION", label(assignment.exam()) + " must start at " + assignment.start()
                        + " on the same day as the other Drawing groups, using " + assignment.venue().venueName() + ".", id));
            }
            drawingDay = window.day();
        }
        return fixed;
    }

    private static List<ScheduledExam> allocateRooms(
            ExamSchedulingInput input, Map<Long, Placement> placements, Map<Long, Venue> fixed, List<Issue> issues
    ) {
        List<Venue> venues = input.venues().stream()
                .sorted(Comparator.comparingInt(Venue::examCapacity).reversed().thenComparing(Venue::venueId))
                .toList();
        Map<Long, Window> slotWindows = new HashMap<>();
        for (Slot s : input.timeslots()) {
            slotWindows.put(s.timeslotId(), new Window(LocalDate.parse(s.date()),
                    LocalTime.parse(s.startTime()), LocalTime.parse(s.endTime()), List.of(s.timeslotId())));
        }
        Map<Long, List<Window>> bookings = new HashMap<>();
        for (Venue room : venues) {
            bookings.put(room.venueId(), new ArrayList<>(room.unavailableTimeslotIds().stream()
                    .map(slotWindows::get).filter(java.util.Objects::nonNull).toList()));
        }
        var order = placements.values().stream()
                .sorted(Comparator.<Placement>comparingInt(entry -> fixed.containsKey(entry.exam().requirementId()) ? 0 : 1)
                        .thenComparing(Comparator.comparingInt((Placement entry) -> entry.exam().studentCount()).reversed())
                        .thenComparing(Comparator.comparingInt((Placement entry) -> entry.exam().durationMinutes()).reversed())
                        .thenComparing(entry -> entry.exam().requirementId()))
                .toList();
        List<ScheduledExam> result = new ArrayList<>();
        for (Placement entry : order) {
            Exam exam = entry.exam();
            Window window = entry.window();
            Venue room = fixed.get(exam.requirementId());
            var free = (room == null ? venues : List.of(room)).stream()
                    .filter(venue -> bookings.get(venue.venueId()).stream().noneMatch(window::overlaps)).toList();
            var chosen = ExamTimetableDraftService.chooseRooms(free, exam.studentCount());
            if (chosen.isEmpty()) {
                issues.add(error("VENUES_UNAVAILABLE", label(exam) + " needs " + exam.studentCount()
                        + " seats on " + window.day() + " at " + window.start()
                        + ". No suitable rooms remain for the full exam. Choose another time.", exam.requirementId()));
                continue;
            }
            for (Venue venue : chosen) {
                bookings.get(venue.venueId()).add(window);
            }
            result.add(new ScheduledExam(exam.requirementId(), exam.moduleCode(), exam.moduleName(), exam.curriculum(),
                    exam.durationMinutes(), exam.studentCount(), window.day().toString(), window.start().toString(),
                    window.end().toString(), window.ids(), ExamTimetableDraftService.allocateGroups(exam, chosen)));
        }
        return result.stream().sorted(Comparator.comparing(ScheduledExam::date)
                .thenComparing(ScheduledExam::startTime).thenComparing(ScheduledExam::requirementId)).toList();
    }

    static List<StartAssignment> starts(Draft draft) {
        return java.util.stream.Stream.concat(
                draft.exams().stream().map(exam -> new StartAssignment(exam.requirementId(), exam.timeslotIds().getFirst())),
                draft.unscheduledExams().stream().map(exam -> new StartAssignment(exam.requirementId(), null)))
                .toList();
    }

    private static String label(Exam exam) {
        return exam.moduleCode() + " " + exam.moduleName() + " (" + exam.curriculum() + ")";
    }

    private static Issue error(String code, String description, Long... ids) {
        return new Issue("ERROR", code, description, List.of(ids));
    }

    public record StartAssignment(Long requirementId, Long startTimeslotId) {}

    public record Issue(String severity, String code, String description, List<Long> requirementIds) {}

    public record Review(boolean valid, boolean studentMembershipsComplete, List<Issue> issues,
                         List<ScheduledExam> exams) {}

    private record Placement(Exam exam, Window window) {}

    private record Window(LocalDate day, LocalTime start, LocalTime end, List<Long> ids) {
        private boolean overlaps(Window other) {
            return day.equals(other.day()) && start.isBefore(other.end()) && other.start().isBefore(end);
        }
    }
}
