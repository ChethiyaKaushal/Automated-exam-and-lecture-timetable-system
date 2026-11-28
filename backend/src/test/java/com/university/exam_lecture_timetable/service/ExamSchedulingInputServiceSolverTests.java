package com.university.exam_lecture_timetable.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.university.exam_lecture_timetable.dto.solver.SolverInput;
import com.university.exam_lecture_timetable.dto.solver.SolverInput.ConflictPair;
import com.university.exam_lecture_timetable.dto.solver.SolverInput.ConflictType;
import com.university.exam_lecture_timetable.entity.CalendarPeriodEntity;
import com.university.exam_lecture_timetable.entity.DepartmentEntity;
import com.university.exam_lecture_timetable.entity.EventRequirementEntity;
import com.university.exam_lecture_timetable.entity.ExamRequirementEntity;
import com.university.exam_lecture_timetable.entity.ModuleEntity;
import com.university.exam_lecture_timetable.entity.RequirementType;
import com.university.exam_lecture_timetable.entity.SemesterEntity;
import com.university.exam_lecture_timetable.entity.TimeslotEntity;
import com.university.exam_lecture_timetable.entity.TimetableStatus;
import com.university.exam_lecture_timetable.entity.VenueAvailabilityEntity;
import com.university.exam_lecture_timetable.entity.VenueEntity;
import com.university.exam_lecture_timetable.entity.VenueType;
import com.university.exam_lecture_timetable.exception.ExamTimetableReviewException;
import com.university.exam_lecture_timetable.repository.CalendarPeriodRepository;
import com.university.exam_lecture_timetable.repository.EventAllocationRepository;
import com.university.exam_lecture_timetable.repository.EventAllocationRepository.BookedSlot;
import com.university.exam_lecture_timetable.repository.ExamRequirementRepository;
import com.university.exam_lecture_timetable.repository.StudentGroupRepository;
import com.university.exam_lecture_timetable.repository.StudentGroupRepository.ModuleGroupMember;
import com.university.exam_lecture_timetable.repository.TimeslotRepository;
import com.university.exam_lecture_timetable.repository.VenueAvailabilityRepository;
import com.university.exam_lecture_timetable.repository.VenueRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ExamSchedulingInputServiceSolverTests {

    private final CalendarPeriodRepository periods = mock(CalendarPeriodRepository.class);
    private final ExamRequirementRepository exams = mock(ExamRequirementRepository.class);
    private final StudentGroupRepository groups = mock(StudentGroupRepository.class);
    private final TimeslotRepository timeslots = mock(TimeslotRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final VenueAvailabilityRepository availability = mock(VenueAvailabilityRepository.class);
    private final EventAllocationRepository allocations = mock(EventAllocationRepository.class);
    private final ExamConflictService conflicts = mock(ExamConflictService.class);

    private ExamSchedulingInputService service;
    private List<TimeslotEntity> slots;

    @BeforeEach
    void setUp() {
        service = new ExamSchedulingInputService(
                periods, exams, groups, timeslots, venues, availability, allocations, conflicts);

        var semester = new SemesterEntity();
        semester.setSemesterId(1L);
        var period = new CalendarPeriodEntity();
        period.setPeriodId(1L);
        period.setSemesters(Set.of(semester));
        when(periods.findById(1L)).thenReturn(java.util.Optional.of(period));

        // 103 needs a computer center.
        var is1402 = exam(101, module(11, "IS1402", "C-23", semester), 180, 2, null);
        var is1402old = exam(102, module(12, "IS1402", "C-18", semester), 180, null, null);
        var ee1102 = exam(103, module(13, "EE1102", "C-23", semester), 120, 0, VenueType.COMPUTER_CENTER);
        when(exams.findActiveByPeriodAndType(1L, RequirementType.EXAM)).thenReturn(List.of(is1402, is1402old, ee1102));

        when(groups.findMembersByModuleIds(any())).thenReturn(List.of(
                new Member(11L, 21L, 200, null), new Member(11L, 22L, 180, null),
                new Member(12L, 31L, 25, null),
                new Member(13L, 41L, 110, null)));

        slots = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            var slot = new TimeslotEntity();
            slot.setTimeslotId(i + 1L);
            slot.setDate(LocalDate.of(2026, 12, 1));
            slot.setStartTime(LocalTime.of(9, 0).plusMinutes(30L * i));
            slot.setEndTime(LocalTime.of(9, 30).plusMinutes(30L * i));
            slots.add(slot);
        }
        when(timeslots.findByPeriod_PeriodIdOrderByDateAscTimeslotAsc(1L)).thenReturn(slots);

        when(venues.findByIsAvailableForExamsTrue()).thenReturn(List.of(
                venue(2, "NCC", VenueType.LECTURE_HALL, 140, 1),
                venue(1, "CC1", VenueType.COMPUTER_CENTER, 120, 2),
                venue(3, "NO-CAP", VenueType.LECTURE_HALL, null, 1)));

        var blocked = new VenueAvailabilityEntity(venues.findByIsAvailableForExamsTrue().getFirst(), slots.get(2), false);
        when(availability.findByTimeslot_Period_PeriodIdAndIsAvailableFalse(1L)).thenReturn(List.of(blocked));
        when(allocations.findBookedSlots(eq(1L), any(), eq(TimetableStatus.ARCHIVED)))
                .thenReturn(List.<BookedSlot>of(new Booked(2L, 5L), new Booked(2L, 3L)));
        when(conflicts.findConflictPairs(any(), any())).thenReturn(List.of(
                new ConflictPair(101L, 103L, ConflictType.DIFFERENT_DAY, true, true)));
    }

    @Test
    void mapsExamsWithNewFields() {
        SolverInput input = service.loadSolverInput(1L);

        var first = input.exams().getFirst();
        assertEquals(101L, first.requirementId());
        assertEquals(5L, first.departmentId());
        assertEquals(1L, first.semesterId());
        assertEquals(2, first.examGapDays());
        assertNull(first.requiredVenueType());
        assertEquals(380, first.studentCount());
        assertEquals(List.of(new SolverInput.Group(21L, 200), new SolverInput.Group(22L, 180)), first.groups());

        assertEquals(0, input.exams().get(1).examGapDays(), "null gap days becomes 0");
        assertEquals(VenueType.COMPUTER_CENTER, input.exams().get(2).requiredVenueType());
    }

    @Test
    void venuesIncludeTypePriorityAndAllUnavailableSlots() {
        SolverInput input = service.loadSolverInput(1L);

        assertEquals(List.of(1L, 2L), input.venues().stream().map(SolverInput.Venue::venueId).toList(),
                "sorted by id, venue without exam capacity left out");
        var ncc = input.venues().get(1);
        assertEquals(VenueType.LECTURE_HALL, ncc.venueType());
        assertEquals(1, ncc.priority());
        // slot 3 is marked unavailable AND booked; slot 5 is booked by another timetable.
        assertEquals(List.of(3L, 5L), ncc.unavailableTimeslotIds());
        assertEquals(List.of(), input.venues().getFirst().unavailableTimeslotIds());
    }

    @Test
    void bookedSlotsIgnoreTheExamsBeingScheduled() {
        service.loadSolverInput(1L);
        verify(allocations).findBookedSlots(1L, Set.of(101L, 102L, 103L), TimetableStatus.ARCHIVED);
    }

    @Test
    void conflictPairsAreBuiltFromModulesOfThePeriod() {
        SolverInput input = service.loadSolverInput(1L);
        verify(conflicts).findConflictPairs(Map.of(11L, List.of(101L), 12L, List.of(102L), 13L, List.of(103L)), Set.of(1L));
        assertEquals(1, input.conflictPairs().size());
    }

    @Test
    void examWithRequiredVenueTypeGetsAllVenuesOfThatTypeAsFixedVenues() {
        SolverInput input = service.loadSolverInput(1L);
        // Only 103 has a requiredVenueType (COMPUTER_CENTER); CC1 is the only computer center.
        assertEquals(List.of(new SolverInput.FixedAssignment(103L, List.of(1L))), input.fixedAssignments());
    }

    @Test
    void requiredVenueTypeWithoutAnyVenueOfThatTypeIsRejected() {
        var drawing = new SolverInput.Exam(9L, 9L, "ME1201", "Engineering Drawing", "C-23", null, null, 180, 0,
                VenueType.DRAWING_OFFICE, 40, List.of(new SolverInput.Group(9L, 40)));
        var hall = new SolverInput.Venue(1L, "NCC", VenueType.LECTURE_HALL, 140, 1, List.of());
        var error = assertThrows(ExamTimetableReviewException.class,
                () -> ExamSchedulingInputService.buildFixedAssignments(List.of(drawing), List.of(hall)));
        assertTrue(error.getMessage().contains("DRAWING_OFFICE"));
    }

    @Test
    void serializesWithTheFieldNamesThePythonSolverExpects() throws Exception {
        String json = JsonMapper.builder().build().writeValueAsString(service.loadSolverInput(1L));
        for (String field : List.of("\"periodId\"", "\"requirementId\"", "\"examGapDays\"", "\"requiredVenueType\":null",
                "\"requiredVenueType\":\"COMPUTER_CENTER\"", "\"venueType\":\"LECTURE_HALL\"", "\"unavailableTimeslotIds\"",
                "\"conflictType\":\"DIFFERENT_DAY\"", "\"firstHasProperStudents\":true", "\"sharedExamGroups\":[]",
                "\"startTime\":\"09:00\"", "\"date\":\"2026-12-01\"",
                "\"fixedAssignments\":[{\"requirementId\":103,\"venueIds\":[1]}]")) {
            assertTrue(json.contains(field), () -> "missing " + field + " in " + json);
        }
        // Lets you check the payload against ai-service/app/schemas/solver_input.py by hand.
        Files.writeString(Path.of("target", "solver-input-sample.json"), json);
    }

    // --- builders ---------------------------------------------------------------------

    private static ModuleEntity module(long id, String code, String curriculum, SemesterEntity semester) {
        var department = new DepartmentEntity();
        department.setDepartmentId(5L);
        var module = new ModuleEntity();
        module.setModuleId(id);
        module.setModuleCode(code);
        module.setModuleName("Module " + code);
        module.setCurriculum(curriculum);
        module.setDepartment(department);
        module.setSemester(semester);
        return module;
    }

    private static ExamRequirementEntity exam(long id, ModuleEntity module, int duration, Integer gap, VenueType type) {
        var requirement = new EventRequirementEntity();
        requirement.setRequirementId(id);
        requirement.setModule(module);
        requirement.setRequirementType(RequirementType.EXAM);
        var exam = new ExamRequirementEntity();
        exam.setRequirementId(id);
        exam.setRequirement(requirement);
        exam.setExamDurationMinutes(duration);
        exam.setExamGapDays(gap);
        exam.setRequiredVenueType(type);
        return exam;
    }

    private static VenueEntity venue(long id, String name, VenueType type, Integer capacity, int priority) {
        var venue = new VenueEntity();
        venue.setVenueId(id);
        venue.setVenueName(name);
        venue.setVenueType(type);
        venue.setExamCapacity(capacity);
        venue.setPriority(priority);
        return venue;
    }

    private record Member(Long getModuleId, Long getGroupId, Integer getGroupSize, Long getStudentId)
            implements ModuleGroupMember {}

    private record Booked(Long getVenueId, Long getTimeslotId) implements BookedSlot {}

}