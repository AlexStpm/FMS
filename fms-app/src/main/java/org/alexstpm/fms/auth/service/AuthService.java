package org.alexstpm.fms.auth.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.auth.dto.LoginRequest;
import org.alexstpm.fms.auth.dto.RefreshRequest;
import org.alexstpm.fms.auth.dto.TokenResponse;
import org.alexstpm.fms.security.dto.TokenPair;
import org.alexstpm.fms.security.model.RefreshToken;
import org.alexstpm.fms.security.repository.RefreshTokenRepository;
import org.alexstpm.fms.security.service.TokenService;
import org.alexstpm.fms.user.model.User;
import org.alexstpm.fms.user.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager
                .authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                request.email(),
                                request.password()));
        if (!(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("Unexpected principal type");
        }
        TokenPair tokenPair = tokenService.issuePair(user);
        saveRefreshToken(user, tokenPair.refreshToken());
        return toResponse(tokenPair, "Bearer");
    }

    @Transactional
    public TokenResponse loginWithGoogle(String email) {
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Google email is missing");
        }

        User user = userService.requireByEmail(email);

        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        TokenPair tokenPair = tokenService.issuePair(user);
        saveRefreshToken(user, tokenPair.refreshToken());

        return toResponse(tokenPair, "Bearer");
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        Jwt jwt = tokenService.parseRefreshToken(request.refreshToken());
        String jti = jwt.getId();
        if (jti == null || jti.isBlank()) throw new JwtException("Refresh token missing jti");
        RefreshToken refreshToken = refreshTokenRepository.findByJtiForUpdate(jti)
                .orElseThrow(() -> new JwtException("Refresh token not found"));
        if (refreshToken.isRevoked()) throw new BadJwtException("Refresh token is revoked");
        if (!MessageDigest.isEqual(
                refreshToken.getTokenHash().getBytes(StandardCharsets.US_ASCII),
                hash(request.refreshToken()).getBytes(StandardCharsets.US_ASCII))) {
            throw new JwtException("Refresh token does not match stored token");
        }
        if (!refreshToken.getExpiresAt().isAfter(Instant.now())) {
            throw new JwtException("Refresh token is expired");
        }
        String email = jwt.getSubject();
        if (email == null || email.isBlank()) throw new JwtException("Refresh token has no subject");
        if (!refreshToken.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new JwtException("Refresh token user does not match");
        }
        User user = userService.requireByEmail(email);
        if (!user.isEnabled()) throw new DisabledException("User is disabled");
        refreshToken.revoke();
        TokenPair tokenPair = tokenService.issuePair(user);
        saveRefreshToken(user, tokenPair.refreshToken());
        return toResponse(tokenPair, "Bearer");
    }

    private void saveRefreshToken(User user, String rawToken) {
        Jwt jwt = tokenService.parseRefreshToken(rawToken);

        RefreshToken entity = RefreshToken.builder()
                .jti(jwt.getId())
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(jwt.getExpiresAt())
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private TokenResponse toResponse(TokenPair tokenPair, String tokenType) {
        return new TokenResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenType,
                tokenPair.expiresIn());
    }
}
