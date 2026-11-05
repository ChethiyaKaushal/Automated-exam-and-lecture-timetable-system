package com.university.exam_lecture_timetable.service.impl;

import com.university.exam_lecture_timetable.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DevEmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(DevEmailServiceImpl.class);

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        logger.info("=================================================================");
        logger.info("[DEV EMAIL DISPATCH] Password Reset Notification");
        logger.info("To: {}", toEmail);
        logger.info("Reset Token: {}", resetToken);
        logger.info("Reset Link Example: https://timetable.university.edu/reset-password?token={}", resetToken);
        logger.info("Token Validity: Configured expiration window");
        logger.info("=================================================================");
    }

    @Override
    public void sendTemporaryPasswordEmail(String toEmail, String temporaryPassword) {
        logger.info("=================================================================");
        logger.info("[DEV EMAIL DISPATCH] Account Creation & Credentials Notification");
        logger.info("To: {}", toEmail);
        logger.info("Temporary Initial Password: {}", temporaryPassword);
        logger.info("Note: mustChangePassword flag is set to true");
        logger.info("=================================================================");
    }
}
