package com.reports.api.service;

import com.reports.api.dto.FilterItem;
import com.reports.api.dto.ReportRunCreateRequest;
import com.reports.api.model.Report;
import com.reports.api.model.ReportRun;
import com.reports.api.model.ReportRunFilter;
import com.reports.api.model.ReportRunFilterId;
import com.reports.api.repository.ReportRepository;
import com.reports.api.repository.ReportRunFilterRepository;
import com.reports.api.repository.ReportRunRepository;
import com.reports.api.repository.UserRoleRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReportService {
    private static final Set<String> ALLOWED_FILTER_KEYS = Set.of(
            "sbu", "zone", "cluster", "unit", "branch", "date_from", "date_to"
    );

    private final UserRoleRepository userRoleRepository;
    private final ReportRepository reportRepository;
    private final ReportRunRepository reportRunRepository;
    private final ReportRunFilterRepository reportRunFilterRepository;

    public ReportService(
            UserRoleRepository userRoleRepository,
            ReportRepository reportRepository,
            ReportRunRepository reportRunRepository,
            ReportRunFilterRepository reportRunFilterRepository
    ) {
        this.userRoleRepository = userRoleRepository;
        this.reportRepository = reportRepository;
        this.reportRunRepository = reportRunRepository;
        this.reportRunFilterRepository = reportRunFilterRepository;
    }

    public List<Report> listAccessibleReports(UUID userId) {
        List<Short> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return reportRepository.findActiveReportsByRoleIds(roleIds);
    }

    @Transactional
    public ReportRun createReportRun(UUID userId, UUID reportId, ReportRunCreateRequest payload) {
        List<Short> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            throw new ResponseStatusException(FORBIDDEN, "No role assigned");
        }

        Report report = reportRepository.findAccessibleReport(reportId, roleIds)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report not found"));

        for (FilterItem filter : payload.getFilters()) {
            if (!ALLOWED_FILTER_KEYS.contains(filter.key())) {
                throw new ResponseStatusException(BAD_REQUEST, "Unsupported filter key: " + filter.key());
            }
        }

        ReportRun run = new ReportRun();
        run.setUserId(userId);
        run.setReportId(report.getId());
        run.setStatus("queued");
        run = reportRunRepository.save(run);

        for (FilterItem filter : payload.getFilters()) {
            ReportRunFilterId id = new ReportRunFilterId();
            id.setReportRunId(run.getId());
            id.setFilterKey(filter.key());
            id.setFilterValue(filter.value());
            reportRunFilterRepository.save(new ReportRunFilter(id));
        }

        run.setStatus("success");
        run.setFileUrl(Path.of("storage", "reports", report.getCode() + "." + report.getOutputFormat()).toString());
        return reportRunRepository.save(run);
    }

    public List<ReportRun> listMyRuns(UUID userId) {
        return reportRunRepository.findByUserIdOrderByRequestedAtDesc(userId);
    }

    public Path resolveDownload(UUID userId, UUID runId) {
        ReportRun run = reportRunRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report run not found"));
        if (!"success".equals(run.getStatus()) || run.getFileUrl() == null) {
            throw new ResponseStatusException(CONFLICT, "Report is not available for download yet");
        }
        Path path = Path.of(run.getFileUrl());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(NOT_FOUND, "Report file is missing on server");
        }
        return path;
    }
}
