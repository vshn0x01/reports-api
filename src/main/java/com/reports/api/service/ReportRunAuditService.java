package com.reports.api.service;

import com.reports.api.dto.FilterItem;
import com.reports.api.model.ReportRun;
import com.reports.api.model.ReportRunFilter;
import com.reports.api.model.ReportRunFilterId;
import com.reports.api.repository.ReportRunFilterRepository;
import com.reports.api.repository.ReportRunRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportRunAuditService {

    private final ReportRunRepository reportRunRepository;
    private final ReportRunFilterRepository reportRunFilterRepository;

    public ReportRunAuditService(
            ReportRunRepository reportRunRepository,
            ReportRunFilterRepository reportRunFilterRepository
    ) {
        this.reportRunRepository = reportRunRepository;
        this.reportRunFilterRepository = reportRunFilterRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReportRun start(UUID userId, UUID reportId, List<FilterItem> filters) {
        ReportRun run = new ReportRun();
        run.setUserId(userId);
        run.setReportId(reportId);
        run.setStatus("queued");
        run = reportRunRepository.save(run);

        for (FilterItem filter : filters) {
            ReportRunFilterId id = new ReportRunFilterId();
            id.setReportRunId(run.getId());
            id.setFilterKey(filter.key());
            id.setFilterValue(filter.value());
            reportRunFilterRepository.save(new ReportRunFilter(id));
        }
        return run;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(UUID runId, String fileName, long fileSizeBytes) {
        ReportRun run = reportRunRepository.findById(runId).orElseThrow();
        run.setStatus("success");
        run.setCompletedAt(OffsetDateTime.now());
        run.setFileUrl(fileName);
        run.setFileSizeBytes(fileSizeBytes);
        run.setErrorMessage(null);
        reportRunRepository.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID runId, String errorMessage) {
        ReportRun run = reportRunRepository.findById(runId).orElseThrow();
        run.setStatus("failed");
        run.setCompletedAt(OffsetDateTime.now());
        run.setErrorMessage(trimTo(errorMessage, 1000));
        reportRunRepository.save(run);
    }

    private static String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
