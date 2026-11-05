package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.dto.ExamSchedulingInput;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.Draft;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.GroupAllocation;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.RoomAllocation;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.ScheduledExam;
import com.university.exam_lecture_timetable.dto.response.ExamTimetableDraft.UnscheduledExam;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import com.university.exam_lecture_timetable.validation.DrawingExamRule;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Exam;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Group;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Slot;
import com.university.exam_lecture_timetable.dto.ExamSchedulingInput.Venue;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ExamTimetableDraftService {

    private final ExamSchedulingInputService inputService;

    public ExamTimetableDraftService(ExamSchedulingInputService inputService) {
        this.inputService = inputService;
    }

    public Draft generate(Long periodId) {
        return generate(inputService.loadDraft(periodId));
    }

    Draft generate(ExamSchedulingInputService.DraftInput loaded) {
        ExamSchedulingInput input = loaded.input();
        Long periodId = input.periodId();
        List<Window> slots = input.timeslots().stream().map(Window::fromSlot).toList();
        checkNonOverlappingSlots(slots);

        Map<Integer, List<Window>> windowsByDuration = new HashMap<>();
        Map<Long, Window> scheduledWindows = new HashMap<>();
        Map<Long, Set<Long>> conflicts = conflictIndex(input);
        Map<Long, List<Window>> roomBookings = roomBookings(input, slots);
        List<Venue> venues = input.venues().stream()
                .sorted(Comparator.comparingInt(Venue::examCapacity).reversed().thenComparing(Venue::venueId))
                .toList();
        List<Exam> exams = input.exams().stream()
                .sorted(Comparator.comparingInt(Exam::studentCount).reversed()
                        .thenComparing(Comparator.comparingInt(Exam::durationMinutes).reversed())
                        .thenComparing(Exam::requirementId))
                .toList();

        var drawing = DrawingExamRule.assignments(input);
        Set<Long> drawingIds = new HashSet<>();
        for (var assignment : drawing) {
            drawingIds.add(assignment.exam().requirementId());
        }
        Map<Long, Placement> drawingPlacements = placeDrawing(slots, drawing, conflicts, roomBookings);
        drawingPlacements.forEach((id, placement) -> reserve(id, placement, scheduledWindows, roomBookings));

        List<ScheduledExam> scheduled = new ArrayList<>();
        List<UnscheduledExam> unscheduled = new ArrayList<>();
        for (Exam exam : exams) {
            boolean fixedDrawing = drawingIds.contains(exam.requirementId());
            List<Window> windows = fixedDrawing ? List.of() : windowsByDuration.computeIfAbsent(
                    exam.durationMinutes(), minutes -> candidateWindows(slots, minutes));
            Placement placement = fixedDrawing ? drawingPlacements.get(exam.requirementId())
                    : findPlacement(exam, windows, venues, conflicts, scheduledWindows, roomBookings);
            if (placement == null) {
                String reason = fixedDrawing ? "DO1 and DO2 cannot host both Drawing sessions on the same day."
                        : windows.isEmpty() ? "No consecutive slots fit the exam duration."
                        : "No placement found with the available rooms and recorded conflicts.";
                unscheduled.add(new UnscheduledExam(exam.requirementId(), exam.moduleCode(), exam.moduleName(),
                        exam.curriculum(), reason));
                continue;
            }

            Window window = placement.window();
            if (!fixedDrawing) {
                reserve(exam.requirementId(), placement, scheduledWindows, roomBookings);
            }
            scheduled.add(new ScheduledExam(exam.requirementId(), exam.moduleCode(), exam.moduleName(),
                    exam.curriculum(), exam.durationMinutes(), exam.studentCount(), window.date().toString(),
                    window.start().toString(), window.end().toString(), window.timeslotIds(), placement.rooms()));
        }

        scheduled.sort(Comparator.comparing(ScheduledExam::date)
                .thenComparing(ScheduledExam::startTime)
                .thenComparing(ScheduledExam::requirementId));
        return new Draft(periodId, "DRAFT", loaded.membershipComplete(), unscheduled.isEmpty(),
                List.copyOf(scheduled), List.copyOf(unscheduled));
    }

    private static void reserve(
            Long requirementId, Placement placement, Map<Long, Window> scheduledWindows,
            Map<Long, List<Window>> roomBookings
    ) {
        scheduledWindows.put(requirementId, placement.window());
        for (RoomAllocation room : placement.rooms()) {
            roomBookings.get(room.venueId()).add(placement.window());
        }
    }

    private static Map<Long, Placement> placeDrawing(
            List<Window> slots, List<DrawingExamRule.Assignment> drawing,
            Map<Long, Set<Long>> conflicts, Map<Long, List<Window>> roomBookings
    ) {
        if (drawing.isEmpty()) {
            return Map.of();
        }

        Map<LocalDate, Map<LocalTime, Window>> windowsByDay = new LinkedHashMap<>();
        for (Window window : candidateWindows(slots, 180)) {
            if (window.end().equals(window.start().plusHours(3))) {
                windowsByDay.computeIfAbsent(window.date(), ignored -> new HashMap<>()).put(window.start(), window);
            }
        }
        for (var day : windowsByDay.values()) {
            Map<Long, Placement> placements = new HashMap<>();
            for (var assignment : drawing) {
                Exam exam = assignment.exam();
                Venue room = assignment.venue();
                Window window = day.get(assignment.start());
                if (window == null || room.examCapacity() < exam.studentCount()
                        || roomBookings.get(room.venueId()).stream().anyMatch(window::overlaps)
                        || conflicts.getOrDefault(exam.requirementId(), Set.of()).stream()
                                .map(placements::get).anyMatch(other -> other != null && window.overlaps(other.window()))) {
                    break;
                }
                placements.put(exam.requirementId(), new Placement(window, allocateGroups(exam, List.of(room))));
            }
            if (placements.size() == drawing.size()) {
                return placements;
            }
        }
        return Map.of();
    }

    private static void checkNonOverlappingSlots(List<Window> slots) {
        for (int i = 1; i < slots.size(); i++) {
            if (slots.get(i - 1).overlaps(slots.get(i))) {
                throw new ExamTimetableReviewException("The draft scheduler needs a grid of nonoverlapping timeslots.");
            }
        }
    }

    private static Map<Long, Set<Long>> conflictIndex(ExamSchedulingInput input) {
        Map<Long, Set<Long>> conflicts = new HashMap<>();
        for (var pair : input.studentConflicts()) {
            conflicts.computeIfAbsent(pair.firstRequirementId(), ignored -> new HashSet<>())
                    .add(pair.secondRequirementId());
            conflicts.computeIfAbsent(pair.secondRequirementId(), ignored -> new HashSet<>())
                    .add(pair.firstRequirementId());
        }
        return conflicts;
    }

    private static Map<Long, List<Window>> roomBookings(ExamSchedulingInput input, List<Window> slots) {
        Map<Long, Window> slotsById = new HashMap<>();
        for (Window slot : slots) {
            slotsById.put(slot.timeslotIds().getFirst(), slot);
        }
        Map<Long, List<Window>> bookings = new HashMap<>();
        for (Venue venue : input.venues()) {
            List<Window> blocked = new ArrayList<>();
            for (Long id : venue.unavailableTimeslotIds()) {
                Window slot = slotsById.get(id);
                if (slot != null) {
                    blocked.add(slot);
                }
            }
            bookings.put(venue.venueId(), blocked);
        }
        return bookings;
    }

    private static List<Window> candidateWindows(List<Window> slots, int durationMinutes) {
        List<Window> windows = new ArrayList<>();
        Duration required = Duration.ofMinutes(durationMinutes);
        for (int i = 0; i < slots.size(); i++) {
            Window first = slots.get(i);
            LocalTime end = first.start();
            List<Long> ids = new ArrayList<>();
            for (int j = i; j < slots.size(); j++) {
                Window next = slots.get(j);
                if (!next.date().equals(first.date()) || !next.start().equals(end)) {
                    break;
                }
                ids.add(next.timeslotIds().getFirst());
                end = next.end();
                if (Duration.between(first.start(), end).compareTo(required) >= 0) {
                    windows.add(new Window(first.date(), first.start(), end, List.copyOf(ids)));
                    break;
                }
            }
        }
        return windows;
    }

    private static Placement findPlacement(
            Exam exam, List<Window> windows, List<Venue> venues, Map<Long, Set<Long>> conflicts,
            Map<Long, Window> scheduledWindows, Map<Long, List<Window>> roomBookings
    ) {
        Set<Long> related = conflicts.getOrDefault(exam.requirementId(), Set.of());
        for (Window window : windows) {
            boolean clashes = related.stream()
                    .map(scheduledWindows::get)
                    .anyMatch(other -> other != null && window.overlaps(other));
            if (clashes) {
                continue;
            }

            List<Venue> available = venues.stream()
                    .filter(venue -> roomBookings.get(venue.venueId()).stream().noneMatch(window::overlaps))
                    .toList();
            List<Venue> chosen = chooseRooms(available, exam.studentCount());
            if (!chosen.isEmpty()) {
                return new Placement(window, allocateGroups(exam, chosen));
            }
        }
        return null;
    }

    public static List<Venue> chooseRooms(List<Venue> available, int students) {
        // Largest rooms give a lower bound on how many rooms this window needs.
        int roomCount = 0;
        long capacity = 0;
        while (roomCount < available.size() && capacity < students) {
            capacity += available.get(roomCount++).examCapacity();
        }
        if (capacity < students) {
            return List.of();
        }
        if (roomCount == available.size()) {
            return available;
        }

        // For that room count, choose the smallest capacity total that still fits.
        List<Map<Long, List<Venue>>> byCount = new ArrayList<>();
        for (int i = 0; i <= roomCount; i++) {
            byCount.add(new LinkedHashMap<>());
        }
        byCount.getFirst().put(0L, List.of());
        for (Venue room : available) {
            for (int count = roomCount; count > 0; count--) {
                for (var previous : byCount.get(count - 1).entrySet()) {
                    long seats = previous.getKey() + room.examCapacity();
                    if (seats <= capacity && !byCount.get(count).containsKey(seats)) {
                        List<Venue> rooms = new ArrayList<>(previous.getValue());
                        rooms.add(room);
                        byCount.get(count).put(seats, List.copyOf(rooms));
                    }
                }
            }
        }
        return byCount.get(roomCount).entrySet().stream()
                .filter(entry -> entry.getKey() >= students)
                .min(Map.Entry.comparingByKey())
                .orElseThrow()
                .getValue();
    }

    public static List<RoomAllocation> allocateGroups(Exam exam, List<Venue> venues) {
        List<Group> groups = exam.groups();
        List<RoomAllocation> rooms = new ArrayList<>();
        int groupIndex = 0;
        int groupRemaining = groups.getFirst().studentCount();
        for (Venue venue : venues) {
            int seatsLeft = venue.examCapacity();
            List<GroupAllocation> assignments = new ArrayList<>();
            while (seatsLeft > 0 && groupIndex < groups.size()) {
                Group group = groups.get(groupIndex);
                int assigned = Math.min(seatsLeft, groupRemaining);
                assignments.add(new GroupAllocation(group.groupId(), assigned));
                seatsLeft -= assigned;
                groupRemaining -= assigned;
                if (groupRemaining == 0) {
                    groupIndex++;
                    if (groupIndex < groups.size()) {
                        groupRemaining = groups.get(groupIndex).studentCount();
                    }
                }
            }
            rooms.add(new RoomAllocation(venue.venueId(), venue.venueName(), roomLabel(exam, rooms.size()),
                    venue.examCapacity(),
                    venue.examCapacity() - seatsLeft, List.copyOf(assignments)));
        }
        return List.copyOf(rooms);
    }

    static String roomLabel(Exam exam, int index) {
        if ("ME1201".equals(exam.moduleCode()) && "C-23".equals(exam.curriculum())) {
            return exam.moduleName().substring("Engineering Drawing (Group ".length(), exam.moduleName().length() - 1);
        }
        if (index >= 52) {
            return "Room " + (index + 1);
        }
        return (char) ('A' + index / 2) + (index % 2 == 0 ? "-I" : "-II");
    }

    private record Window(LocalDate date, LocalTime start, LocalTime end, List<Long> timeslotIds) {
        private static Window fromSlot(Slot slot) {
            return new Window(LocalDate.parse(slot.date()), LocalTime.parse(slot.startTime()),
                    LocalTime.parse(slot.endTime()), List.of(slot.timeslotId()));
        }

        private boolean overlaps(Window other) {
            return date.equals(other.date()) && start.isBefore(other.end()) && other.start().isBefore(end);
        }
    }

    private record Placement(Window window, List<RoomAllocation> rooms) {}
}
