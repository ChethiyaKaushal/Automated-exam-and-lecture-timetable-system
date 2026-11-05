package com.university.exam_lecture_timetable.service.impl;

import com.university.exam_lecture_timetable.dto.request.ChangePasswordRequest;
import com.university.exam_lecture_timetable.dto.request.ForgotPasswordRequest;
import com.university.exam_lecture_timetable.dto.request.LoginRequest;
import com.university.exam_lecture_timetable.dto.request.ResetPasswordRequest;
import com.university.exam_lecture_timetable.dto.response.AuthResponse;
import com.university.exam_lecture_timetable.dto.response.UserResponse;
import com.university.exam_lecture_timetable.entity.PasswordResetTokenEntity;
import com.university.exam_lecture_timetable.entity.UserEntity;
import com.university.exam_lecture_timetable.exception.AccountStatusException;
import com.university.exam_lecture_timetable.exception.InvalidPasswordException;
import com.university.exam_lecture_timetable.exception.InvalidTokenException;
import com.university.exam_lecture_timetable.exception.ResourceNotFoundException;
import com.university.exam_lecture_timetable.repository.PasswordResetTokenRepository;
import com.university.exam_lecture_timetable.repository.UserRepository;
import com.university.exam_lecture_timetable.security.JwtTokenProvider;
import com.university.exam_lecture_timetable.security.TokenBlacklistService;
import com.university.exam_lecture_timetable.security.UserPrincipal;
import com.university.exam_lecture_timetable.service.AuthService;
import com.university.exam_lecture_timetable.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;


@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;
    private final long resetTokenExpirationMinutes;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           TokenBlacklistService tokenBlacklistService,
                           EmailService emailService,
                           @Value("${app.jwt.reset-token-expiration-minutes:30}") long resetTokenExpirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.emailService = emailService;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Authenticate credentials using Spring Security AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Fetch user from DB to verify account flags and construct response
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (!user.isEnabled()) {
            throw new AccountStatusException("Account is disabled. Please contact the administrator.");
        }
        if (!user.isAccountNonLocked()) {
            throw new AccountStatusException("Account is locked. Please contact the administrator.");
        }

        String jwt = tokenProvider.generateToken(authentication);

        return new AuthResponse(
                jwt,
                tokenProvider.getJwtExpirationMs(),
                UserResponse.fromEntity(user)
        );
    }

    @Override
    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        if (!request.isPasswordConfirmationMatching()) {
            throw new InvalidPasswordException("New password and confirmation password do not match");
        }

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password provided is incorrect");
        }

        // Check if new password is identical to old password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password cannot be the same as the current password");
        }

        // Hash and update new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);

        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", userEmail);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            // Mitigate email enumeration attack: return generic success without revealing existence
            logger.info("Password reset requested for non-existent email: {}", request.getEmail());
            return;
        }

        UserEntity user = userOptional.get();

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            logger.warn("Password reset requested for disabled/locked account: {}", request.getEmail());
            return;
        }

        // Invalidate prior unused tokens
        passwordResetTokenRepository.markAllUnusedTokensAsUsedForUser(user);

        // Generate cryptographically secure token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);

        // Hash token for safe database storage (SHA-256)
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes);
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity(user, tokenHash, expiresAt);
        passwordResetTokenRepository.save(resetToken);

        // Send raw token to user via email/mock service
        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        logger.info("Generated password reset token for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword( ResetPasswordRequest request) {
        if (!request.isPasswordConfirmationMatching()) {
            throw new InvalidPasswordException("New password and confirmation password do not match");
        }

        String tokenHash = hashToken(request.getToken());

        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Password reset token has expired or has already been used");
        }

        UserEntity user = resetToken.getUser();

        // Update password with BCrypt hash
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Mark token as used to prevent replay
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        logger.info("Password reset completed successfully for user: {}", user.getEmail());
    }

    @Override
    public void logout(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (tokenProvider.validateToken(token)) {
                Date expiration = tokenProvider.getExpirationDateFromToken(token);
                tokenBlacklistService.blacklistToken(token, expiration);
            }
        }
        SecurityContextHolder.clearContext();
        logger.debug("Logged out and invalidated security context");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in runtime", e);
        }
    }
}
