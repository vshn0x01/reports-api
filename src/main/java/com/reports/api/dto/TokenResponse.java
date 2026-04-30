package com.reports.api.dto;

public record TokenResponse(
        String access_token,
        String token_type
) {
    public TokenResponse(String accessToken) {
        this(accessToken, "bearer");
    }
}
