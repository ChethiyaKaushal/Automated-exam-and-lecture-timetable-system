package com.university.exam_lecture_timetable.controller;

import com.university.exam_lecture_timetable.dto.request.CreateSetupRequestDto;
import com.university.exam_lecture_timetable.dto.request.ModuleFilterRequestDto;
import com.university.exam_lecture_timetable.dto.request.SaveRequirementsRequest;
import com.university.exam_lecture_timetable.dto.respose.AcademicYearResponseDto;
import com.university.exam_lecture_timetable.dto.respose.ModuleResponseDto;
import com.university.exam_lecture_timetable.dto.respose.SemesterResponseDto;
import com.university.exam_lecture_timetable.dto.respose.SetupResponse;
import com.university.exam_lecture_timetable.service.TimetableSetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timetablesetup")
public class TimeTableSetupController {
    private final TimetableSetupService service;

    public TimeTableSetupController(TimetableSetupService service) {
        this.service = service;
    }

    @GetMapping("/semesters")
    public ResponseEntity<List<SemesterResponseDto>> getSemestersByAcademicYear(
            @RequestParam String academicYear
    ) {
        return ResponseEntity.ok(
                service.getSemestersByAcademicYear(academicYear)
        );
    }

    @PostMapping("/filtermodule")
    public List<ModuleResponseDto> filter(@Valid @RequestBody ModuleFilterRequestDto request){
        return service.filter(request);
    }

    @PostMapping("/setup")
    public ResponseEntity<SetupResponse> setup(@Valid @RequestBody CreateSetupRequestDto request){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/requirements")
    public ResponseEntity<Void> requirements(@Valid @RequestBody SaveRequirementsRequest request){
        service.saveExamRequirements(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/academic-years")
    public ResponseEntity<List<AcademicYearResponseDto>> getAcademicYears() {
        return ResponseEntity.ok(
                service.getAcademicYears()
        );
    }
}

