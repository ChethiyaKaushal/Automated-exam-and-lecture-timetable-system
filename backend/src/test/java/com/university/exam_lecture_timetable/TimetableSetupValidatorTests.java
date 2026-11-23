
package com.university.exam_lecture_timetable;

import com.university.exam_lecture_timetable.dto.request.SaveRequirementsRequest;
import com.university.exam_lecture_timetable.dto.request.SaveRequirementsRequest.ModuleRequirement;
import com.university.exam_lecture_timetable.entity.*;
import com.university.exam_lecture_timetable.exception.BadRequestException;
import com.university.exam_lecture_timetable.exception.ConflictException;
import com.university.exam_lecture_timetable.repository.*;
import com.university.exam_lecture_timetable.validation.TimetableSetupValidator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TimetableSetupValidatorTests {

    private final ModuleRepository modules = mock(ModuleRepository.class);
    private final CalendarPeriodRepository periods = mock(CalendarPeriodRepository.class);
    private final EventRequirementRepository requirements = mock(EventRequirementRepository.class);

    private final TimetableSetupValidator validator =
            new TimetableSetupValidator(
                    mock(SemesterRepository.class),
                    modules,
                    periods,
                    requirements
            );

    @Test
    void resolvesAllSemestersOfASharedPeriod() {
        SemesterEntity first = semester(1L);
        SemesterEntity second = semester(2L);

        CalendarPeriodEntity period = period(10L, first, second);
        ModuleEntity module = module(20L, second);

        when(periods.findAllById(List.of(10L))).thenReturn(List.of(period));
        when(modules.findByScope(Set.of(1L, 2L))).thenReturn(List.of(module));

        var result = validator.validateExamRequirements(request(List.of(10L)));

        assertEquals(10L, result.periodBySemester().get(1L));
        assertEquals(10L, result.periodBySemester().get(2L));
        assertSame(module, result.eligibleModules().get(20L));
    }

    @Test
    void rejectsAmbiguousPeriodsForTheSameSemester() {
        SemesterEntity semester = semester(1L);

        when(periods.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(
                        period(10L, semester),
                        period(11L, semester)
                ));

        when(modules.findByScope(any())).thenReturn(List.of());

        assertThrows(
                BadRequestException.class,
                () -> validator.validateExamRequirements(request(List.of(10L, 11L)))
        );
    }

    @Test
    void rejectsExistingExamThroughModuleRelationship() {
        SemesterEntity semester = semester(1L);
        ModuleEntity module = module(20L, semester);

        when(periods.findAllById(List.of(10L)))
                .thenReturn(List.of(period(10L, semester)));

        when(modules.findByScope(any())).thenReturn(List.of(module));

        EventRequirementEntity existing = new EventRequirementEntity();
        existing.setModule(module);
        existing.setRequirementType(RequirementType.EXAM);

        when(requirements.findByPeriod_PeriodId(10L))
                .thenReturn(List.of(existing));

        assertThrows(
                ConflictException.class,
                () -> validator.validateExamRequirements(request(List.of(10L)))
        );
    }

    @Test
    void rejectsNullModuleInsteadOfThrowingNullPointerException() {
        when(periods.findAllById(List.of(10L)))
                .thenReturn(List.of(period(10L, semester(1L))));

        when(modules.findByScope(any())).thenReturn(List.of());

        var request = new SaveRequirementsRequest(
                List.of(10L),
                Arrays.asList((ModuleRequirement) null),
                List.of()
        );

        assertThrows(
                BadRequestException.class,
                () -> validator.validateExamRequirements(request)
        );
    }

    private SaveRequirementsRequest request(List<Long> periodIds) {
        return new SaveRequirementsRequest(
                periodIds,
                List.of(
                        new ModuleRequirement(
                                20L,
                                true,
                                120,
                                2,
                                VenueType.LECTURE_HALL
                        )
                ),
                List.of()
        );
    }

    private SemesterEntity semester(Long id) {
        SemesterEntity semester = new SemesterEntity();
        semester.setSemesterId(id);
        return semester;
    }

    private CalendarPeriodEntity period(
            Long id,
            SemesterEntity... semesters
    ) {
        CalendarPeriodEntity period = new CalendarPeriodEntity();
        period.setPeriodId(id);
        period.setSemesters(Set.of(semesters));
        return period;
    }

    private ModuleEntity module(Long id, SemesterEntity semester) {
        ModuleEntity module = new ModuleEntity();
        module.setModuleId(id);
        module.setSemester(semester);
        return module;
    }
}
