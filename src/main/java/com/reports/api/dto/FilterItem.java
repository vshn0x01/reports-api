package com.reports.api.dto;

import jakarta.validation.constraints.NotBlank;

public record FilterItem(
        @NotBlank String key,
        @NotBlank String value
) {
}
