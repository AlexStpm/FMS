package org.alexstpm.fms.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.security.service.TokenService;
import org.alexstpm.fms.user.model.User;
import org.alexstpm.fms.user.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                authenticate(token, request);
            } catch (JwtException | AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                logger.debug("JWT authentication failed ", ex);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || header.isBlank() || header.substring(7).trim().isBlank()) {
            return null;
        }
        return header.substring(7).trim();
    }

    private void authenticate(String token, HttpServletRequest request) {
        Jwt jwt = tokenService.parseAccessToken(token);
        String email = jwt.getSubject();
        if (email == null || email.isBlank()) throw new JwtException("Access token has no subject");;
        User user = userService.requireByEmail(email);
        if (!user.isEnabled()) throw new DisabledException("User is disabled");
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                user.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
