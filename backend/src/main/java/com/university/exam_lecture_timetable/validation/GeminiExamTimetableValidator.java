package com.university.exam_lecture_timetable.validation;

import com.university.exam_lecture_timetable.dto.ExamSchedulingInput;
import com.university.exam_lecture_timetable.service.ExamSchedulingInputService;
import com.university.exam_lecture_timetable.service.ExamTimetableDraftService;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Exam;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Slot;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Venue;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.Draft;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.ScheduledExam;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

public final class GeminiExamTimetableValidator {

    private GeminiExamTimetableValidator() {}

    public static Draft validate(
            ExamSchedulingInputService.DraftInput loaded, List<DrawingExamRule.Assignment> drawing,
            JsonNode response
    ) {
        var input = loaded.input();
        JsonNode entries = response.path("exams");
        String status = response.path("status").asText("");
        if ("UNSCHEDULED".equals(status) && entries.isArray() && entries.isEmpty()) {
            throw invalid("Gemini did not find a complete schedule.");
        }
        if (!"SCHEDULED".equals(status) || !response.path("reason").isNull()
                || !entries.isArray() || entries.size() != input.exams().size()) {
            throw invalid("Expected one scheduled entry per exam.");
        }

        var exams = index(input.exams(), Exam::requirementId);
        var slots = index(input.timeslots(), Slot::timeslotId);
        Map<Long, TimedExam> placed = new HashMap<>();

        for (JsonNode entry : entries) {
            Long id = positiveId(entry.path("requirementId"));
            Exam exam = exams.get(id);
            if (exam == null || placed.containsKey(id)) {
                throw invalid("Unknown or repeated requirement ID " + id + ".");
            }
            var ids = readSlotIds(entry.path("timeslotIds"));
            placed.put(id, new TimedExam(exam, readWindow(ids, slots, exam), ids));
        }

        for (var conflict : input.studentConflicts()) {
            if (placed.get(conflict.firstRequirementId()).window()
                    .overlaps(placed.get(conflict.secondRequirementId()).window())) {
                throw invalid("Student conflict between requirements " + conflict.firstRequirementId()
                        + " and " + conflict.secondRequirementId() + ".");
            }
        }
        var fixedRooms = checkDrawing(drawing, placed);
        var scheduled = assignRooms(input, slots, placed, fixedRooms);
        return new Draft(input.periodId(), "DRAFT", loaded.membershipComplete(), true, scheduled, List.of());
    }

    private static List<Long> readSlotIds(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            throw invalid("Each exam needs at least one timeslot.");
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode id : node) {
            ids.add(positiveId(id));
        }
        return List.copyOf(ids);
    }

    private static Window readWindow(List<Long> ids, Map<Long, Slot> slots, Exam exam) {
        Window window = null;
        for (Long id : ids) {
            Slot slot = slots.get(id);
            if (slot == null) {
                throw invalid("Unknown timeslot " + id + ".");
            }
            var next = Window.from(slot);
            if (window != null && (!window.date().equals(next.date()) || !window.end().equals(next.start()))) {
                throw invalid("Nonconsecutive slots for requirement " + exam.requirementId() + ".");
            }
            window = window == null ? next : new Window(window.date(), window.start(), next.end());
        }
        if (Duration.between(window.start(), window.end()).toMinutes() < exam.durationMinutes()) {
            throw invalid("Not enough time for requirement " + exam.requirementId() + ".");
        }
        return window;
    }

    private static Map<Long, Venue> checkDrawing(
            List<DrawingExamRule.Assignment> drawing, Map<Long, TimedExam> placed
    ) {
        Map<Long, Venue> fixedRooms = new HashMap<>();
        LocalDate drawingDate = null;
        for (var assignment : drawing) {
            Long id = assignment.exam().requirementId();
            Window window = placed.get(id).window();
            if ((drawingDate != null && !drawingDate.equals(window.date()))
                    || !window.start().equals(assignment.start())
                    || !window.end().equals(assignment.start().plusHours(3))) {
                throw invalid("ME1201 C-23 must use its two fixed sessions on the same day.");
            }
            fixedRooms.put(id, assignment.venue());
            drawingDate = window.date();
        }
        return fixedRooms;
    }

    private static List<ScheduledExam> assignRooms(
            ExamSchedulingInput input, Map<Long, Slot> slots, Map<Long, TimedExam> placed, Map<Long, Venue> fixedRooms
    ) {
        List<Venue> venues = input.venues().stream()
                .sorted(Comparator.comparingInt(Venue::examCapacity).reversed().thenComparing(Venue::venueId))
                .toList();
        Map<Long, List<Window>> bookings = new HashMap<>();
        for (Venue venue : venues) {
            var unavailable = venue.unavailableTimeslotIds().stream().map(slots::get).map(Window::from).toList();
            bookings.put(venue.venueId(), new ArrayList<>(unavailable));
        }

        var order = placed.values().stream()
                .sorted(Comparator.<TimedExam>comparingInt(entry ->
                            fixedRooms.containsKey(entry.exam().requirementId()) ? 0 : 1)
                        .thenComparing(Comparator.comparingInt((TimedExam entry) -> entry.exam().studentCount()).reversed())
                        .thenComparing(Comparator.comparingInt((TimedExam entry) -> entry.exam().durationMinutes()).reversed())
                        .thenComparing(entry -> entry.exam().requirementId()))
                .toList();
        List<ScheduledExam> scheduled = new ArrayList<>();
        for (TimedExam entry : order) {
            Exam exam = entry.exam();
            Window window = entry.window();
            Venue fixed = fixedRooms.get(exam.requirementId());
            List<Venue> available = (fixed == null ? venues : List.of(fixed)).stream()
                    .filter(room -> bookings.get(room.venueId()).stream().noneMatch(window::overlaps))
                    .toList();
            List<Venue> rooms = ExamTimetableDraftService.chooseRooms(available, exam.studentCount());
            if (rooms.isEmpty()) {
                throw invalid("No venue allocation found for " + exam.moduleCode() + " (requirement "
                        + exam.requirementId() + ") at " + window.date() + " " + window.start()
                        + ". Needed " + exam.studentCount() + " seats. Try different times or use the Java draft engine.");
            }

            for (Venue room : rooms) {
                bookings.get(room.venueId()).add(window);
            }
            scheduled.add(new ScheduledExam(exam.requirementId(), exam.moduleCode(), exam.moduleName(),
                    exam.curriculum(), exam.durationMinutes(), exam.studentCount(), window.date().toString(),
                    window.start().toString(), window.end().toString(), entry.timeslotIds(),
                    ExamTimetableDraftService.allocateGroups(exam, rooms)));
        }
        return scheduled.stream().sorted(Comparator.comparing(ScheduledExam::date)
                .thenComparing(ScheduledExam::startTime).thenComparing(ScheduledExam::requirementId)).toList();
    }

    private static long positiveId(JsonNode node) {
        if (!node.isIntegralNumber() || !node.canConvertToLong() || node.asLong() <= 0) {
            throw invalid("IDs must be positive whole numbers.");
        }
        return node.asLong();
    }

    private static <T> Map<Long, T> index(List<T> items, Function<T, Long> key) {
        return items.stream().collect(Collectors.toMap(key, Function.identity()));
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Gemini timetable rejected: " + reason);
    }

    private record TimedExam(Exam exam, Window window, List<Long> timeslotIds) {}

    private record Window(LocalDate date, LocalTime start, LocalTime end) {
        private static Window from(Slot slot) {
            return new Window(LocalDate.parse(slot.date()), LocalTime.parse(slot.startTime()),
                    LocalTime.parse(slot.endTime()));
        }

        private boolean overlaps(Window other) {
            return date.equals(other.date()) && start.isBefore(other.end()) && other.start().isBefore(end);
        }
    }
}
