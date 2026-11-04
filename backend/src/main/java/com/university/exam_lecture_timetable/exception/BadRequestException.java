package com.university.exam_lecture_timetable.exception;

import java.util.List;

public class BadRequestException extends RuntimeException {

    private final List<String> details;

    public BadRequestException(String message) {
        super(message);
        this.details = List.of(message);
    }

    public BadRequestException(String message, List<String> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
