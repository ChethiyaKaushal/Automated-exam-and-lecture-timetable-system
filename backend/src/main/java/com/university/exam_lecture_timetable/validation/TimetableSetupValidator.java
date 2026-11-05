package com.university.exam_lecture_timetable.validation;

import com.university.exam_lecture_timetable.dto.request.CreateSetupRequestDto;
import com.university.exam_lecture_timetable.dto.request.ModuleFilterRequestDto;
import com.university.exam_lecture_timetable.dto.request.SaveRequirementsRequest;
import com.university.exam_lecture_timetable.entity.CalendarPeriodEntity;
import com.university.exam_lecture_timetable.entity.ModuleEntity;
import com.university.exam_lecture_timetable.entity.RequirementType;
import com.university.exam_lecture_timetable.exception.BadRequestException;
import com.university.exam_lecture_timetable.exception.ConflictException;
import com.university.exam_lecture_timetable.repository.CalendarPeriodRepository;
import com.university.exam_lecture_timetable.repository.EventRequirementRepository;
import com.university.exam_lecture_timetable.repository.ModuleRepository;
import com.university.exam_lecture_timetable.repository.SemesterRepository;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * All validation rules for the timetable setup flow.
 * Throws {@link BadRequestException} / {@link ConflictException}, which
 * GlobalExceptionHandler turns into 400 / 409 ApiError responses.
 */
@Component
public class TimetableSetupValidator {

    private static final Set<String> EVENT_TYPES = Set.of("EXAM", "LECTURE");

    private final SemesterRepository semesters;
    private final ModuleRepository modules;
    private final CalendarPeriodRepository periods;
    private final EventRequirementRepository requirements;

    public TimetableSetupValidator(
            SemesterRepository semesters,
            ModuleRepository modules,
            CalendarPeriodRepository periods,
            EventRequirementRepository requirements) {
        this.semesters = semesters;
        this.modules = modules;
        this.periods = periods;
        this.requirements = requirements;
    }

    // ---- results handed back to the service ----

    public record ValidatedSetup(List<Long> semesterIds, String eventType) {}

    public record ValidatedRequirements(
            Map<Long, ModuleEntity> eligibleModules,
            Map<Long, Long> periodBySemester) {}

    // ---- module filter ----

    public List<Long> validateFilter(ModuleFilterRequestDto request) {
        List<Long> semesterIds = distinctIds(request.semesterIds(), "semesterIds");
        verifySemestersExist(semesterIds);
        return semesterIds;
    }

    // ---- create setup ----

    public ValidatedSetup validateCreate(CreateSetupRequestDto request) {
        if (request.startingDate() == null || request.endingDate() == null) {
            throw new BadRequestException("Start and end dates are required");
        }
        if (request.endingDate().isBefore(request.startingDate())) {
            throw new BadRequestException("End date precedes start date");
        }

        String type = normalizeEventType(request.eventType());

        List<Long> semesterIds = distinctIds(request.semesterIds(), "semesterIds");
        verifySemestersExist(semesterIds);

        return new ValidatedSetup(semesterIds, type);
    }

    // ---- save exam requirements ----

    public ValidatedRequirements validateExamRequirements(SaveRequirementsRequest request) {
        List<Long> periodIds = distinctIds(request.periodIds(), "periodIds");
        List<CalendarPeriodEntity> selectedPeriods = periods.findAllById(periodIds);

        if (selectedPeriods.size() != periodIds.size()) {
            throw new BadRequestException("Unknown calendar period");
        }
        if (selectedPeriods.stream().anyMatch(p -> !"EXAM".equalsIgnoreCase(p.getPeriodType()))) {
            throw new BadRequestException("Only EXAM periods supported here");
        }
        if (request.modules() == null || request.modules().isEmpty()) {
            throw new BadRequestException("At least one module is required");
        }

        Set<Long> semesterIds = selectedPeriods.stream()
                .flatMap(p -> p.getSemesters().stream()).map(s -> s.getSemesterId())
                .collect(Collectors.toSet());

        Map<Long, ModuleEntity> eligible = modules.findByScope(semesterIds).stream()
                .collect(Collectors.toMap(ModuleEntity::getModuleId, m -> m));

        Map<Long, Long> periodBySemester = new HashMap<>();
        for (CalendarPeriodEntity period : selectedPeriods) {
            for (var semester : period.getSemesters()) {
                if (periodBySemester.putIfAbsent(semester.getSemesterId(), period.getPeriodId()) != null) {
                    throw new BadRequestException("Multiple periods selected for the same semester");
                }
            }
        }

        Set<Long> seen = new HashSet<>();

        for (var input : request.modules()) {
            if (input == null || input.moduleId() == null || input.hasExam() == null) {
                throw new BadRequestException("Module ID and hasExam are required");
            }
            Long moduleId = input.moduleId();

            if (!seen.add(moduleId) || !eligible.containsKey(moduleId)) {
                throw new BadRequestException("Duplicate or out-of-scope module: " + moduleId);
            }

            if (!Boolean.TRUE.equals(input.hasExam())) {
                continue;
            }

            if (input.examDurationMinutes() == null || input.examDurationMinutes() <= 0) {
                throw new BadRequestException("Positive exam duration required");
            }

            Long periodId = periodBySemester.get(eligible.get(moduleId).getSemester().getSemesterId());
            if (periodId == null) {
                throw new BadRequestException("Missing period for module semester");
            }

            ensureExamRequirementAbsent(periodId, moduleId);
        }

        return new ValidatedRequirements(eligible, periodBySemester);
    }

    // ---- reusable rules ----

    private String normalizeEventType(String eventType) {
        if (eventType == null) {
            throw new BadRequestException("eventType must be EXAM or LECTURE");
        }
        String type = eventType.toUpperCase(Locale.ROOT);
        if (!EVENT_TYPES.contains(type)) {
            throw new BadRequestException("eventType must be EXAM or LECTURE");
        }
        return type;
    }

    private List<Long> distinctIds(List<Long> ids, String fieldName) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException(fieldName + " must be non-empty and contain no nulls");
        }
        return ids.stream().distinct().toList();
    }

    private void verifySemestersExist(List<Long> semesterIds) {
        if (semesters.findAllById(semesterIds).size() != semesterIds.size()) {
            throw new BadRequestException("Unknown semester");
        }
    }

    private void ensureExamRequirementAbsent(Long periodId, Long moduleId) {
        boolean exists = requirements.findByPeriod_PeriodId(periodId).stream()
                .anyMatch(r -> r.getModule().getModuleId().equals(moduleId)
                        && r.getRequirementType() == RequirementType.EXAM);

        if (exists) {
            throw new ConflictException("Exam requirement already exists for module " + moduleId);
        }
    }
}
