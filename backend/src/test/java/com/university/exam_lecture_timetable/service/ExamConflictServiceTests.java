package com.university.exam_lecture_timetable.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.university.exam_lecture_timetable.dto.solver.SolverInput.ConflictPair;
import com.university.exam_lecture_timetable.dto.solver.SolverInput.ConflictType;
import com.university.exam_lecture_timetable.entity.EnrollmentType;
import com.university.exam_lecture_timetable.entity.RegistrationStatus;
import com.university.exam_lecture_timetable.repository.StudentModuleEnrollmentRepository;
import com.university.exam_lecture_timetable.repository.StudentModuleEnrollmentRepository.EnrollmentRow;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExamConflictServiceTests {

    private static final EnrollmentType P = EnrollmentType.PROPER;
    private static final EnrollmentType R = EnrollmentType.REPEAT;

    // module 10 -> requirement 101, module 20 -> requirement 201, module 30 -> requirement 301
    private static final Map<Long, List<Long>> REQUIREMENTS = Map.of(
            10L, List.of(101L), 20L, List.of(201L), 30L, List.of(301L));

    @Test
    void properInBothModulesMeansDifferentDay() {
        var pairs = ExamConflictService.buildPairs(REQUIREMENTS, List.of(row(1, 10, P), row(1, 20, P)));
        assertEquals(List.of(new ConflictPair(101L, 201L, ConflictType.DIFFERENT_DAY, true, true)), pairs);
    }

    @Test
    void properAndRepeatMeansNoTimeOverlap() {
        var pairs = ExamConflictService.buildPairs(REQUIREMENTS, List.of(row(1, 10, P), row(1, 20, R)));
        assertEquals(List.of(new ConflictPair(101L, 201L, ConflictType.NO_TIME_OVERLAP, true, false)), pairs);
    }

    @Test
    void repeatAndRepeatMeansNoTimeOverlap() {
        var pairs = ExamConflictService.buildPairs(REQUIREMENTS, List.of(row(1, 10, R), row(1, 20, R)));
        assertEquals(List.of(new ConflictPair(101L, 201L, ConflictType.NO_TIME_OVERLAP, false, false)), pairs);
    }

    @Test
    void oneProperProperStudentMakesThePairDifferentDay() {
        var rows = List.of(
                row(1, 10, P), row(1, 20, R),   // proper + repeat
                row(2, 10, R), row(2, 20, P),   // repeat + proper
                row(3, 10, P), row(3, 20, P));  // proper + proper -> wins
        var pairs = ExamConflictService.buildPairs(REQUIREMENTS, rows);
        assertEquals(List.of(new ConflictPair(101L, 201L, ConflictType.DIFFERENT_DAY, true, true)), pairs);
    }

    @Test
    void noCommonStudentMeansNoPair() {
        var pairs = ExamConflictService.buildPairs(REQUIREMENTS, List.of(row(1, 10, P), row(2, 20, P)));
        assertTrue(pairs.isEmpty());
    }

    @Test
    void modulesOutsideTheExamPeriodAreIgnored() {
        var pairs = ExamConflictService.buildPairs(REQUIREMENTS, List.of(row(1, 10, P), row(1, 99, P)));
        assertTrue(pairs.isEmpty());
    }

    @Test
    void everyPairOfAStudentsModulesIsReported() {
        var rows = List.of(row(1, 10, P), row(1, 20, P), row(1, 30, R));
        var pairs = ExamConflictService.buildPairs(REQUIREMENTS, rows);
        assertEquals(List.of(
                new ConflictPair(101L, 201L, ConflictType.DIFFERENT_DAY, true, true),
                new ConflictPair(101L, 301L, ConflictType.NO_TIME_OVERLAP, true, false),
                new ConflictPair(201L, 301L, ConflictType.NO_TIME_OVERLAP, true, false)), pairs);
    }

    // --- swap ------------------------------------------------------------------------

    @Test
    void canonicalKeepsPairThatIsAlreadyInOrder() {
        var pair = ConflictPair.canonical(5L, 9L, ConflictType.NO_TIME_OVERLAP, true, false);
        assertEquals(new ConflictPair(5L, 9L, ConflictType.NO_TIME_OVERLAP, true, false), pair);
    }

    @Test
    void canonicalSwapsIdsAndFlagsTogether() {
        var pair = ConflictPair.canonical(9L, 5L, ConflictType.NO_TIME_OVERLAP, true, false);
        assertEquals(new ConflictPair(5L, 9L, ConflictType.NO_TIME_OVERLAP, false, true), pair);
    }

    @Test
    void canonicalRejectsAnExamConflictingWithItself() {
        assertThrows(IllegalArgumentException.class,
                () -> ConflictPair.canonical(5L, 5L, ConflictType.DIFFERENT_DAY, true, true));
    }

    @Test
    void lowerModuleIdWithHigherRequirementIdIsSwapped() {
        // Module 10 has requirement 900, module 20 has requirement 100: the IDs run the other way.
        var requirements = Map.of(10L, List.of(900L), 20L, List.of(100L));
        var pairs = ExamConflictService.buildPairs(requirements, List.of(row(1, 10, P), row(1, 20, R)));
        // Module 10 (PROPER) is now the SECOND exam, so secondHasProperStudents must be true.
        assertEquals(List.of(new ConflictPair(100L, 900L, ConflictType.NO_TIME_OVERLAP, false, true)), pairs);
    }

    // --- database filtering ------------------------------------------------------------

    @Test
    void onlyEnrollmentsInThePeriodSemestersCount() {
        var repository = mock(StudentModuleEnrollmentRepository.class);
        when(repository.findEnrollmentsForModules(any(), eq(RegistrationStatus.CONFIRMED))).thenReturn(List.of(
                row(1, 10, P, 1), row(1, 20, P, 1),     // semester 1: in scope
                row(2, 10, P, 7), row(2, 30, P, 7)));   // semester 7: out of scope
        var service = new ExamConflictService(repository);

        var pairs = service.findConflictPairs(REQUIREMENTS, Set.of(1L));
        assertEquals(List.of(new ConflictPair(101L, 201L, ConflictType.DIFFERENT_DAY, true, true)), pairs);
    }

    private static EnrollmentRow row(long student, long module, EnrollmentType type) {
        return row(student, module, type, 1);
    }

    private static EnrollmentRow row(long student, long module, EnrollmentType type, long semester) {
        return new Row(student, module, semester, type);
    }

    private record Row(Long getStudentId, Long getModuleId, Long getSemesterId, EnrollmentType getEnrollmentType)
            implements EnrollmentRow {}
}