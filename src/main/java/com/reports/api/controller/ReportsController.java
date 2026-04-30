package com.reports.api.controller;

import com.reports.api.dto.ReportItemResponse;
import com.reports.api.dto.ReportRunCreateRequest;
import com.reports.api.dto.ReportRunResponse;
import com.reports.api.security.AuthenticatedUser;
import com.reports.api.service.ReportService;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportsController {
    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportItemResponse> listReports(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        return reportService.listAccessibleReports(userId).stream().map(ReportItemResponse::from).toList();
    }

    @PostMapping("/{reportId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportRunResponse createRun(
            @PathVariable UUID reportId,
            @RequestBody @Valid ReportRunCreateRequest request,
            Authentication authentication
    ) {
        UUID userId = currentUserId(authentication);
        return ReportRunResponse.from(reportService.createReportRun(userId, reportId, request));
    }

    @GetMapping("/runs")
    public List<ReportRunResponse> listRuns(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        return reportService.listMyRuns(userId).stream().map(ReportRunResponse::from).toList();
    }

    @GetMapping("/runs/{runId}/download")
    public ResponseEntity<Resource> downloadRun(@PathVariable UUID runId, Authentication authentication) {
        UUID userId = currentUserId(authentication);
        Path path = reportService.resolveDownload(userId, runId);
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }

    private UUID currentUserId(Authentication authentication) {
        return ((AuthenticatedUser) authentication).getUserId();
    }
}
