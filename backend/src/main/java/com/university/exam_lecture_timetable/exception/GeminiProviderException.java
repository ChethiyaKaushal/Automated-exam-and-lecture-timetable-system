package com.university.exam_lecture_timetable.exception;

public final class GeminiProviderException extends RuntimeException {
    private final int httpStatus;
    private final String code;

    public GeminiProviderException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int httpStatus() { return httpStatus; }
    public String code() { return code; }
}
