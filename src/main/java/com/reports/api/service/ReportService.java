package com.reports.api.service;

import com.reports.api.dto.ExcelExportDescriptor;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ReportService {
    private static final Set<String> ALLOWED_FILTER_KEYS = Set.of(
            "sbu", "zone", "cluster", "region", "unit", "branch", "date_from", "date_to"
    );

    /** Single SELECT / WITH … SELECT; (?is) allows multiline. */
    private static final Pattern EXPORT_SQL_SHAPE =
            Pattern.compile("(?is)^\\s*(with\\s+.+|select\\s+.+)$");

    private final UserRoleRepository userRoleRepository;
    private final ReportRepository reportRepository;
    private final ReportRunRepository reportRunRepository;
    private final ReportRunFilterRepository reportRunFilterRepository;
    private final ReportRunAuditService reportRunAuditService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final long exportMaxRows;
    private final Path exportStorageDir;

    public ReportService(
            UserRoleRepository userRoleRepository,
            ReportRepository reportRepository,
            ReportRunRepository reportRunRepository,
            ReportRunFilterRepository reportRunFilterRepository,
            ReportRunAuditService reportRunAuditService,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            @Value("${app.report.export.max-rows:100000}") long exportMaxRows,
            @Value("${app.report.export.storage-dir:storage/reports}") String exportStorageDir
    ) {
        this.userRoleRepository = userRoleRepository;
        this.reportRepository = reportRepository;
        this.reportRunRepository = reportRunRepository;
        this.reportRunFilterRepository = reportRunFilterRepository;
        this.reportRunAuditService = reportRunAuditService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.exportMaxRows = exportMaxRows;
        this.exportStorageDir = Paths.get(exportStorageDir).toAbsolutePath().normalize();
    }

    /**
     * Builds an Excel (.xlsx) in memory from {@link Report#getExportSql()} for an authorized user.
     * Buffered (not streamed) so 401/403 JSON errors do not conflict with the HTTP response body.
     * The inner {@code export_sql} is wrapped with {@link BranchScopeSql} for scope and filters.
     */
    public ExcelExportDescriptor openExcelExport(
            UUID userId,
            UUID reportId,
            Long sbuId,
            Long zoneId,
            Long clusterId,
            Long regionId,
            Long unitId,
            Long branchId,
            String branchCode
    ) {
        List<Short> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            throw new ResponseStatusException(FORBIDDEN, "No role assigned");
        }
        Report report = reportRepository.findAccessibleReport(reportId, roleIds)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report not found"));
        String innerSql = validateAndNormalizeExportSql(report.getExportSql());
        String sql = BranchScopeSql.wrapExportSelect(innerSql);
        MapSqlParameterSource exportParams = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("sbuId", sbuId)
                .addValue("zoneId", zoneId)
                .addValue("clusterId", clusterId)
                .addValue("regionId", regionId)
                .addValue("unitId", unitId)
                .addValue("branchId", branchId)
                .addValue("branchCode", branchCode);
        List<FilterItem> auditFilters = buildExportAuditFilters(
                sbuId, zoneId, clusterId, regionId, unitId, branchId, branchCode);
        String fileName = sanitizeFileStem(report.getCode()) + ".xlsx";
        ReportRun run = reportRunAuditService.start(userId, report.getId(), auditFilters);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            streamQueryToXlsx(sql, exportParams, buffer);
            byte[] content = buffer.toByteArray();
            Path storedPath = storeExportFile(run.getId(), fileName, content);
            byte[] persistedContent = Files.readAllBytes(storedPath);
            reportRunAuditService.markSuccess(run.getId(), storedPath.toString(), persistedContent.length);
            return new ExcelExportDescriptor(fileName, persistedContent);
        } catch (IOException e) {
            reportRunAuditService.markFailed(run.getId(), e.getMessage());
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Export failed");
        } catch (RuntimeException e) {
            reportRunAuditService.markFailed(run.getId(), e.getMessage());
            throw e;
        }
    }

    private static List<FilterItem> buildExportAuditFilters(
            Long sbuId,
            Long zoneId,
            Long clusterId,
            Long regionId,
            Long unitId,
            Long branchId,
            String branchCode
    ) {
        List<FilterItem> filters = new ArrayList<>();
        addFilter(filters, "sbu_id", sbuId);
        addFilter(filters, "zone_id", zoneId);
        addFilter(filters, "cluster_id", clusterId);
        addFilter(filters, "region_id", regionId);
        addFilter(filters, "unit_id", unitId);
        addFilter(filters, "branch_id", branchId);
        if (branchCode != null && !branchCode.isBlank()) {
            addFilter(filters, "branch_code", branchCode.trim());
        }
        return filters;
    }

    private static void addFilter(List<FilterItem> filters, String key, Object value) {
        if (value != null) {
            filters.add(new FilterItem(key, String.valueOf(value)));
        }
    }

    private Path storeExportFile(UUID runId, String fileName, byte[] content) throws IOException {
        Files.createDirectories(exportStorageDir);
        String safeName = runId + "_" + fileName;
        Path outputPath = exportStorageDir.resolve(safeName).normalize();
        Files.write(outputPath, content);
        return outputPath;
    }

    private String validateAndNormalizeExportSql(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Report has no export_sql configured");
        }
        String sql = raw.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.indexOf(';') >= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "export_sql must be a single statement");
        }
        if (!EXPORT_SQL_SHAPE.matcher(sql).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "export_sql must be one SELECT or WITH … SELECT");
        }
        return sql;
    }

    private static String sanitizeFileStem(String code) {
        if (code == null || code.isBlank()) {
            return "report";
        }
        String s = code.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return s.length() > 120 ? s.substring(0, 120) : s;
    }

    private void streamQueryToXlsx(String sql, MapSqlParameterSource params, OutputStream out) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Sheet sheet = wb.createSheet("data");
            namedParameterJdbcTemplate.query(sql, params, rs -> {
                fillSheetFromResultSet(sheet, rs, exportMaxRows);
                return null;
            });
            wb.write(out);
            wb.dispose();
        }
    }

    private static void fillSheetFromResultSet(Sheet sheet, ResultSet rs, long maxRows) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        Row header = sheet.createRow(0);
        for (int c = 1; c <= cols; c++) {
            header.createCell(c - 1).setCellValue(meta.getColumnLabel(c));
        }
        int rowIdx = 1;
        long n = 0;
        while (rs.next()) {
            if (++n > maxRows) {
                break;
            }
            Row row = sheet.createRow(rowIdx++);
            for (int c = 1; c <= cols; c++) {
                setCellFromObject(row.createCell(c - 1), rs.getObject(c));
            }
        }
    }

    private static void setCellFromObject(Cell cell, Object val) {
        if (val == null) {
            return;
        }
        if (val instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (val instanceof Boolean b) {
            cell.setCellValue(b);
            return;
        }
        if (val instanceof java.sql.Timestamp ts) {
            cell.setCellValue(ts.toLocalDateTime());
            return;
        }
        if (val instanceof java.sql.Date d) {
            cell.setCellValue(d.toLocalDate());
            return;
        }
        if (val instanceof java.util.Date d) {
            cell.setCellValue(d);
            return;
        }
        cell.setCellValue(val.toString());
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
