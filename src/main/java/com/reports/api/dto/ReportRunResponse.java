package com.reports.api.dto;

import com.reports.api.model.ReportRun;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportRunResponse(
        UUID id,
        UUID report_id,
        String status,
        OffsetDateTime requested_at,
        OffsetDateTime completed_at,
        String file_url,
        Long file_size_bytes,
        String error_message
) {
    public static ReportRunResponse from(ReportRun run) {
        return new ReportRunResponse(
                run.getId(),
                run.getReportId(),
                run.getStatus(),
                run.getRequestedAt(),
                run.getCompletedAt(),
                run.getFileUrl(),
                run.getFileSizeBytes(),
                run.getErrorMessage()
        );
    }
}
