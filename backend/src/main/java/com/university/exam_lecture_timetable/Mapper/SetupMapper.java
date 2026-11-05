package com.university.exam_lecture_timetable.Mapper;

import com.university.exam_lecture_timetable.dto.response.AcademicYearResponseDto;
import com.university.exam_lecture_timetable.dto.response.ModuleResponseDto;
import com.university.exam_lecture_timetable.dto.response.SemesterResponseDto;
import com.university.exam_lecture_timetable.entity.*;
import org.springframework.stereotype.Component;

@Component

public class SetupMapper {

    public SemesterResponseDto semester(SemesterEntity s) {
        return new SemesterResponseDto(
                s.getSemesterId(),
                s.getSemesterName() + " (" + s.getAcademicYear() + ")");
    }

    public ModuleResponseDto module(ModuleEntity m) {
        return new ModuleResponseDto(
                m.getModuleId(),
                m.getModuleCode(),
                m.getModuleName(),
                m.getCurriculum(),
                m.getSemester().getSemesterId(),
                m.getSemester().getSemesterName(),
                m.getDepartment().getDepartmentId(),
                m.getDepartment().getDepartmentName());
    }

    public AcademicYearResponseDto academicYear(String year) {
        return new AcademicYearResponseDto(year);
    }
}
