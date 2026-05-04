package com.reports.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    private final String failureRedirectBase;

    public OAuth2LoginFailureHandler(@Value("${app.oauth2.redirect.failure-uri}") String failureRedirectBase) {
        this.failureRedirectBase = failureRedirectBase;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        String location = UriComponentsBuilder.fromUriString(failureRedirectBase)
                .queryParam("reason", "oauth")
                .build(true)
                .toUriString();
        response.sendRedirect(location);
    }
}
