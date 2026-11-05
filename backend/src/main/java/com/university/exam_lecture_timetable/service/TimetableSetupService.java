package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.Mapper.SetupMapper;
import com.university.exam_lecture_timetable.dto.request.CreateSetupRequestDto;
import com.university.exam_lecture_timetable.dto.request.ModuleFilterRequestDto;
import com.university.exam_lecture_timetable.dto.response.AcademicYearResponseDto;
import com.university.exam_lecture_timetable.dto.response.ModuleResponseDto;
import com.university.exam_lecture_timetable.dto.response.SemesterResponseDto;
import com.university.exam_lecture_timetable.entity.ExamRequirementEntity;
import com.university.exam_lecture_timetable.repository.CalendarPeriodRepository;
import com.university.exam_lecture_timetable.repository.EventRequirementRepository;
import com.university.exam_lecture_timetable.repository.ModuleRepository;
import com.university.exam_lecture_timetable.repository.SemesterRepository;
import com.university.exam_lecture_timetable.validation.TimetableSetupValidator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

import com.university.exam_lecture_timetable.entity.CalendarPeriodEntity;

import com.university.exam_lecture_timetable.entity.EventRequirementEntity;
import com.university.exam_lecture_timetable.entity.RequirementType;
import com.university.exam_lecture_timetable.dto.request.SaveRequirementsRequest;
import com.university.exam_lecture_timetable.dto.response.SetupResponse;

@Service
public class TimetableSetupService {
    private final SemesterRepository semesters;
    private final ModuleRepository modules;
    private final CalendarPeriodRepository periods;
    private final EventRequirementRepository requirements;
    private final SetupMapper mapper;
    private final TimetableSetupValidator validator;

    public TimetableSetupService(
            SemesterRepository semesters,
            ModuleRepository modules,
            CalendarPeriodRepository periods,
            EventRequirementRepository requirements,
            SetupMapper mapper,
            TimetableSetupValidator validator
            ) {

        this.semesters = semesters;
        this.modules = modules;
        this.periods = periods;
        this.requirements = requirements;
        this.mapper = mapper;
        this.validator = validator;

    }

    // get all semesters
    @Transactional(readOnly = true)
    public List<SemesterResponseDto> semesters() {
        return semesters.findAll().stream().map(mapper::semester).toList();
    }

    // find modules according to selected semesters
    @Transactional(readOnly = true)
    public List<ModuleResponseDto> filter(ModuleFilterRequestDto request) {
        List<Long> semesterIds = validator.validateFilter(request);
        return modules.findByScope(semesterIds).stream().map(mapper::module).toList();
    }

    @Transactional
    public SetupResponse create(CreateSetupRequestDto request) {
        var valid = validator.validateCreate(request);

        CalendarPeriodEntity period = new CalendarPeriodEntity();

        for (Long semesterId : valid.semesterIds()) {
            period.getSemesters().add(semesters.getReferenceById(semesterId));
        }
        period.setPeriodType(valid.eventType());
        period.setStartingDate(request.startingDate());
        period.setEndDate(request.endingDate());
        return new SetupResponse(periods.save(period).getPeriodId(), valid.eventType());
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponseDto> getAcademicYears() {
        return semesters.findDistinctAcademicYears()
                .stream()
                .map(mapper::academicYear)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SemesterResponseDto> getSemestersByAcademicYear(
            String academicYear
    ) {
        return semesters
                .findByAcademicYearOrderBySemesterNameAsc(academicYear)
                .stream()
                .map(mapper::semester)
                .toList();
    }

    @Transactional
    public void saveExamRequirements(SaveRequirementsRequest request) {
        var valid = validator.validateExamRequirements(request);

        for (var input : request.modules()) {
            if (!Boolean.TRUE.equals(input.hasExam())) {
                continue;
            }

            var module = valid.eligibleModules().get(input.moduleId());
            Long periodId = valid.periodBySemester().get(module.getSemester().getSemesterId());

            EventRequirementEntity requirement = new EventRequirementEntity();
            requirement.setModule(module);
            requirement.setPeriod(periods.getReferenceById(periodId));
            requirement.setRequirementType(RequirementType.EXAM);

            ExamRequirementEntity exam = new ExamRequirementEntity();
            exam.setRequirement(requirement);
            exam.setHasExam(true);
            exam.setExamDurationMinutes(input.examDurationMinutes());
            requirement.setExamRequirement(exam);

            requirements.save(requirement);
        }
    }


}
