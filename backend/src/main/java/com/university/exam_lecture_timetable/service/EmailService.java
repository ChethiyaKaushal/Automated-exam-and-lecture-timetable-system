package com.university.exam_lecture_timetable.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendTemporaryPasswordEmail(String toEmail, String temporaryPassword);
}
