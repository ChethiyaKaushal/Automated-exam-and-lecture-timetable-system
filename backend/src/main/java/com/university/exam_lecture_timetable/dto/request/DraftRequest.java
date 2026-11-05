package com.university.exam_lecture_timetable.dto.request;

import java.util.List;

public record DraftRequest(List<StartAssignment> assignments, Boolean manualStudentReview) {}
