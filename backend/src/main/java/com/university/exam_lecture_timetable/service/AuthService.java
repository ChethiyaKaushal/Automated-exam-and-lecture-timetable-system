package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.dto.request.ChangePasswordRequest;
import com.university.exam_lecture_timetable.dto.request.ForgotPasswordRequest;
import com.university.exam_lecture_timetable.dto.request.LoginRequest;
import com.university.exam_lecture_timetable.dto.request.ResetPasswordRequest;
import com.university.exam_lecture_timetable.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    void changePassword(String userEmail, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(String bearerToken);
}
