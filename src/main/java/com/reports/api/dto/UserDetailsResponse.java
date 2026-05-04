package com.reports.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record UserDetailsResponse(
        UUID id,
        String username,
        @JsonProperty("full_name") String fullName,
        String email,
        boolean active,
        List<UserDetailsAssignmentResponse> assignments
) {
}
