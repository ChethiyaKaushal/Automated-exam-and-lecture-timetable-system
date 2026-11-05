package com.university.exam_lecture_timetable.controller;

import com.university.exam_lecture_timetable.dto.request.ChangePasswordRequest;
import com.university.exam_lecture_timetable.dto.request.ForgotPasswordRequest;
import com.university.exam_lecture_timetable.dto.request.LoginRequest;
import com.university.exam_lecture_timetable.dto.request.ResetPasswordRequest;
import com.university.exam_lecture_timetable.dto.response.ApiResponse;
import com.university.exam_lecture_timetable.dto.response.AuthResponse;
import com.university.exam_lecture_timetable.dto.response.MessageResponse;
import com.university.exam_lecture_timetable.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * User authentication endpoint (Login)
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", authResponse));
    }

    /**
     * Password change for authenticated users
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            org.springframework.security.core.Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(
                new MessageResponse("Password has been changed successfully")
        ));
    }

    /**
     * Password reset request (Forgot Password)
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                new MessageResponse("If the email is registered in our system, password reset instructions have been sent.")
        ));
    }

    /**
     * Complete password reset using token
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                new MessageResponse("Password has been reset successfully. You may now log in with your new password.")
        ));
    }

    /**
     * User logout endpoint
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success(
                new MessageResponse("Logged out successfully. Token invalidated.")
        ));
    }
}
