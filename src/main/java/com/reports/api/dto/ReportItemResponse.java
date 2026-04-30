package com.reports.api.dto;

import com.reports.api.model.Report;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportItemResponse(
        UUID id,
        String code,
        String name,
        String description,
        String category,
        String cadence,
        String output_format,
        boolean is_active,
        OffsetDateTime created_at
) {
    public static ReportItemResponse from(Report report) {
        return new ReportItemResponse(
                report.getId(),
                report.getCode(),
                report.getName(),
                report.getDescription(),
                report.getCategory(),
                report.getCadence(),
                report.getOutputFormat(),
                report.isActive(),
                report.getCreatedAt()
        );
    }
}
