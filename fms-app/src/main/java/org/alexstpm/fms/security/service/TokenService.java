package org.alexstpm.fms.security.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.security.dto.JwtProperties;
import org.alexstpm.fms.security.dto.TokenPair;
import org.alexstpm.fms.security.model.TokenType;
import org.alexstpm.fms.user.model.User;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    public TokenPair issuePair(User user) {
        String accessToken = createToken(user, TokenType.ACCESS, jwtProperties.accessTtl());
        String refreshToken = createToken(user, TokenType.REFRESH, jwtProperties.refreshTtl());
        return new TokenPair(accessToken, refreshToken, jwtProperties.accessTtl().toSeconds());
    }

    public Jwt parseAccessToken(String token) {
        return parse(token, TokenType.ACCESS);
    }

    public Jwt parseRefreshToken(String token) {
        return parse(token, TokenType.REFRESH);
    }

    private String createToken(User user, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .claim("type", type.name())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private Jwt parse(String token, TokenType expectedType) {
        Jwt jwt = jwtDecoder.decode(token);
        if (!Objects.equals(jwt.getClaimAsString("type"), expectedType.name())) {
            throw new JwtException("Expected " + expectedType.name() + " token");
        }
        return jwt;
    }
}
