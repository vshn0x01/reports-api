package com.reports.api.security;

import com.reports.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * After Microsoft Entra ID (OIDC) login, maps the identity to a local user by email and redirects with the same API JWT used for password login.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final String successRedirectBase;
    private final String failureRedirectBase;

    public OAuth2LoginSuccessHandler(
            AuthService authService,
            @Value("${app.oauth2.redirect.success-uri}") String successRedirectBase,
            @Value("${app.oauth2.redirect.failure-uri}") String failureRedirectBase
    ) {
        this.authService = authService;
        this.successRedirectBase = successRedirectBase;
        this.failureRedirectBase = failureRedirectBase;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String email = extractEmail(authentication);
        var token = authService.tryIssueTokenForMicrosoftEmail(email);
        if (token.isEmpty()) {
            response.sendRedirect(appendQuery(failureRedirectBase, "reason", "account"));
            return;
        }
        String location = UriComponentsBuilder.fromUriString(successRedirectBase)
                .queryParam("token", token.get())
                .build()
                .toUriString();
        response.sendRedirect(location);
    }

    private static String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidc) {
            if (StringUtils.hasText(oidc.getEmail())) {
                return oidc.getEmail();
            }
            if (StringUtils.hasText(oidc.getPreferredUsername())) {
                return oidc.getPreferredUsername();
            }
        }
        if (principal instanceof OAuth2User oauth2) {
            Object email = oauth2.getAttribute("email");
            if (email instanceof String s && StringUtils.hasText(s)) {
                return s;
            }
            Object preferred = oauth2.getAttribute("preferred_username");
            if (preferred instanceof String s && StringUtils.hasText(s)) {
                return s;
            }
        }
        return null;
    }

    private static String appendQuery(String base, String name, String value) {
        return UriComponentsBuilder.fromUriString(base)
                .queryParam(name, value)
                .build(true)
                .toUriString();
    }
}
