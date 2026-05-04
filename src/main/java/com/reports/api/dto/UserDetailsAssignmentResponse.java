package com.reports.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserDetailsAssignmentResponse(
        @JsonProperty("role_code") String roleCode,
        @JsonProperty("role_name") String roleName,
        @JsonProperty("scope_type") String scopeType,
        @JsonProperty("scope_id") Long scopeId,
        @JsonProperty("scope_code") String scopeCode,
        @JsonProperty("scope_name") String scopeName
) {
}
