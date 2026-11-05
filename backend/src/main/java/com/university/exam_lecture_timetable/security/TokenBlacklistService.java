package com.university.exam_lecture_timetable.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    // Map storing token or token identifier -> expiration date
    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Revoke the given token until its expiration time.
     */
    public void blacklistToken(String token, Date expirationDate) {
        if (token != null && expirationDate != null) {
            blacklistedTokens.put(token, expirationDate);
            logger.debug("Token blacklisted until: {}", expirationDate);
        }
    }

    /**
     * Checks if a token is currently revoked.
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        Date expiration = blacklistedTokens.get(token);
        if (expiration == null) {
            return false;
        }
        if (expiration.before(new Date())) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    /**
     * Periodically clean up expired tokens from memory every 15 minutes.
     */
    @Scheduled(fixedRate = 900000)
    public void purgeExpiredTokens() {
        Date now = new Date();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));
        logger.debug("Purged expired tokens from blacklist cache");
    }
}
