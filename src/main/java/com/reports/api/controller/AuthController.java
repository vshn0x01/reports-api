package com.reports.api.controller;

import com.reports.api.dto.LoginRequest;
import com.reports.api.dto.TokenResponse;
import com.reports.api.dto.UserDetailsResponse;
import com.reports.api.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest request) {
        return new TokenResponse(authService.login(request.username(), request.password()));
    }

    /**
     * Starts the Microsoft Entra ID (Office 365) browser sign-in flow. After success, the browser is redirected to
     * {@code app.oauth2.redirect.success-uri} with a {@code token} query parameter (same JWT as password login).
     */
    @GetMapping("/oauth2/azure")
    public void startMicrosoftLogin(
            HttpServletResponse response,
            @Value("${spring.security.oauth2.client.registration.azure.client-id:}") String azureClientId
    ) throws IOException {
        if (azureClientId == null || azureClientId.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Microsoft sign-in is not configured");
        }
        response.sendRedirect("/oauth2/authorization/azure");
    }

    @GetMapping("/me")
    public UserDetailsResponse me(@AuthenticationPrincipal UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Could not validate credentials");
        }
        return authService.getUserDetails(userId);
    }
}
