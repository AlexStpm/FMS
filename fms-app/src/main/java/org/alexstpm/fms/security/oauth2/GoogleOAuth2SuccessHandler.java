package org.alexstpm.fms.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.auth.dto.TokenResponse;
import org.alexstpm.fms.auth.service.AuthService;
import org.alexstpm.fms.common.error.ApiError;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            writeUnauthorized(request, response);
            return;
        }
        String email = oidcUser.getAttribute("email");
        Boolean verified = oidcUser.getAttribute("email_verified");
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(verified)) {
            writeUnauthorized(request, response);
            return;
        }
        try {
            TokenResponse tokens = authService.loginWithGoogle(email);
            clearOAuthSession(request);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), tokens);
        } catch (AuthenticationException ex) {
            writeUnauthorized(request,response);
        }
    }
    
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        clearOAuthSession(request);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiError("GOOGLE_LOGIN_FAILED", "Google account is not authorized"));
    }

    private void clearOAuthSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();

        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
