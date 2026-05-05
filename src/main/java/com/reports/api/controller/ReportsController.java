package com.reports.api.controller;

import com.reports.api.dto.ExcelExportDescriptor;
import com.reports.api.dto.LoanOutstandingDashboardMetrics;
import com.reports.api.dto.LoanOutstandingSummaryRow;
import com.reports.api.dto.ReportItemResponse;
import com.reports.api.dto.ReportRunCreateRequest;
import com.reports.api.dto.ReportRunResponse;
import com.reports.api.dto.FilterOptionsResponse;
import com.reports.api.security.AuthenticatedUser;
import com.reports.api.service.FilterService;
import com.reports.api.service.OutstandingSummaryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/reports")
public class ReportsController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ReportService reportService;
    private final FilterService filterService;
    private final OutstandingSummaryService outstandingSummaryService;

    public ReportsController(
            ReportService reportService,
            FilterService filterService,
            OutstandingSummaryService outstandingSummaryService
    ) {
        this.reportService = reportService;
        this.filterService = filterService;
        this.outstandingSummaryService = outstandingSummaryService;
    }

    @GetMapping("/outstanding/summary")
    public List<LoanOutstandingSummaryRow> outstandingSummaryByBranch(
            Authentication authentication,
            @RequestParam(required = false) Long sbuId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long clusterId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(value = "branch_id", required = false) Long branchIdSnake,
            @RequestParam(required = false) String branchCode,
            @RequestParam(value = "branch_code", required = false) String branchCodeSnake
    ) {
        UUID userId = currentUserId(authentication);
        return outstandingSummaryService.summaryByBranch(
                userId,
                sbuId,
                zoneId,
                clusterId,
                regionId,
                unitId,
                firstNonNull(branchId, branchIdSnake),
                branchCodeOrNull(branchCode, branchCodeSnake));
    }

    @GetMapping("/outstanding/dashboard")
    public LoanOutstandingDashboardMetrics outstandingDashboard(
            Authentication authentication,
            @RequestParam(required = false) Long sbuId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long clusterId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(value = "branch_id", required = false) Long branchIdSnake,
            @RequestParam(required = false) String branchCode,
            @RequestParam(value = "branch_code", required = false) String branchCodeSnake
    ) {
        UUID userId = currentUserId(authentication);
        return outstandingSummaryService.dashboardMetrics(
                userId,
                sbuId,
                zoneId,
                clusterId,
                regionId,
                unitId,
                firstNonNull(branchId, branchIdSnake),
                branchCodeOrNull(branchCode, branchCodeSnake));
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

    @GetMapping("/{reportId}/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @PathVariable UUID reportId,
            Authentication authentication,
            @RequestParam(required = false) Long sbuId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long clusterId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(value = "branch_id", required = false) Long branchIdSnake,
            @RequestParam(required = false) String branchCode,
            @RequestParam(value = "branch_code", required = false) String branchCodeSnake
    ) {
        UUID userId = currentUserId(authentication);
        ExcelExportDescriptor export = reportService.openExcelExport(
                userId,
                reportId,
                sbuId,
                zoneId,
                clusterId,
                regionId,
                unitId,
                firstNonNull(branchId, branchIdSnake),
                branchCodeOrNull(branchCode, branchCodeSnake));
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.fileName() + "\"")
                .body(export.content());
    }

    @GetMapping("/filters")
    public FilterOptionsResponse getFilters(
            Authentication authentication,
            @RequestParam(required = false) Long sbuId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long clusterId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(value = "branch_id", required = false) Long branchIdSnake,
            @RequestParam(required = false) String branchCode,
            @RequestParam(value = "branch_code", required = false) String branchCodeSnake
    ) {
        UUID userId = currentUserId(authentication);
        return filterService.getFilterOptions(
                userId,
                sbuId,
                zoneId,
                clusterId,
                regionId,
                unitId,
                firstNonNull(branchId, branchIdSnake),
                branchCodeOrNull(branchCode, branchCodeSnake));
    }

    private static Long firstNonNull(Long a, Long b) {
        return a != null ? a : b;
    }

    /** Non-blank branch code for dim {@code branches.code}; null means no code filter. */
    private static String branchCodeOrNull(String primary, String secondary) {
        if (primary != null) {
            String t = primary.trim();
            if (!t.isEmpty()) {
                return t;
            }
        }
        if (secondary != null) {
            String t = secondary.trim();
            if (!t.isEmpty()) {
                return t;
            }
        }
        return null;
    }

    private UUID currentUserId(Authentication authentication) {
        return ((AuthenticatedUser) authentication).getUserId();
    }
}
