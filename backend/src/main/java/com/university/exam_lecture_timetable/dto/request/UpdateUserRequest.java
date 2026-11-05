package com.university.exam_lecture_timetable.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public class UpdateUserRequest {
    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    private String fullName;

    private Long lecturerId;
    private Long studentId;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(Long lecturerId) {
        this.lecturerId = lecturerId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
