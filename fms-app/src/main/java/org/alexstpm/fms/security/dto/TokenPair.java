package org.alexstpm.fms.security.dto;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
