package com.reports.api.service;

import com.reports.api.dto.LoanOutstandingDashboardMetrics;
import com.reports.api.dto.LoanOutstandingSummaryRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OutstandingSummaryService {

    /** Uses {@link BranchScopeSql} — same joins/filters as Excel export for outstanding-style reports. */
    private static final String SUMMARY_BY_BRANCH_SQL = """
            select
              t.branch_id,
              t.branch_name,
              count(distinct t.loan_id) as loans,
              sum(t.pos) as outstanding,
              count(case when coalesce(t.pos_od, 0) != 0 then 1 end) as od_loans,
              sum(case when coalesce(t.pos_od, 0) != 0 then t.pos_od end) as od_amount
            from main.tdw_loan_outstanding_information t
            """
            + BranchScopeSql.JOIN_BRANCH_DIM_FROM_ALIAS_T
            + BranchScopeSql.WHERE_FILTERS_AND_USER_SCOPE
            + """
            group by t.branch_id, t.branch_name
            order by t.branch_name
            """;

    private static final String DASHBOARD_METRICS_SQL = """
            select
              count(distinct t.loan_id) as total_loans,
              sum(case when t.pos_od is null or t.pos_od = 0 then 1 else 0 end) as regular_loans,
              sum(case when t.pos_od is not null and t.pos_od != 0 then 1 else 0 end) as od_loans,
              coalesce(round(
                (100.0 * sum(case when t.pos_od is null or t.pos_od = 0 then 1 else 0 end)
                  / nullif(count(distinct t.loan_id), 0))::numeric,
                2), 0) as pct_regular_loans,
              coalesce(round(
                (100.0 * sum(case when t.pos_od is not null and t.pos_od != 0 then 1 else 0 end)
                  / nullif(count(distinct t.loan_id), 0))::numeric,
                2), 0) as pct_od_loans,
              sum(t.pos) as total_outstanding,
              sum(case when t.pos_od is not null and t.pos_od != 0 then t.pos else 0 end) as od_outstanding,
              coalesce(round(
                (100.0 * sum(case when t.pos_od is not null and t.pos_od != 0 then t.pos else 0 end)
                  / nullif(sum(t.pos), 0))::numeric,
                2), 0) as pct_od_exposure,
              0 as fiod
            from main.tdw_loan_outstanding_information t
            """
            + BranchScopeSql.JOIN_BRANCH_DIM_FROM_ALIAS_T
            + BranchScopeSql.WHERE_FILTERS_AND_USER_SCOPE;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OutstandingSummaryService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LoanOutstandingSummaryRow> summaryByBranch(
            UUID userId,
            Long sbuId,
            Long zoneId,
            Long clusterId,
            Long regionId,
            Long unitId,
            Long branchId,
            String branchCode
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("sbuId", sbuId)
                .addValue("zoneId", zoneId)
                .addValue("clusterId", clusterId)
                .addValue("regionId", regionId)
                .addValue("unitId", unitId)
                .addValue("branchId", branchId)
                .addValue("branchCode", branchCode);
        return jdbcTemplate.query(
                SUMMARY_BY_BRANCH_SQL,
                params,
                (rs, rowNum) -> {
                    BigDecimal od = rs.getBigDecimal("od_amount");
                    return new LoanOutstandingSummaryRow(
                            rs.getString("branch_id"),
                            rs.getString("branch_name"),
                            rs.getLong("loans"),
                            rs.getBigDecimal("outstanding"),
                            rs.getLong("od_loans"),
                            od != null ? od : BigDecimal.ZERO
                    );
                });
    }

    /**
     * Single-row dashboard KPIs; same scope/filters as {@link #summaryByBranch}.
     * Cached per user + filter dimensions for fast repeat loads.
     */
    @Cacheable(value = "outstandingDashboard", keyGenerator = "outstandingDashboardKeyGenerator")
    public LoanOutstandingDashboardMetrics dashboardMetrics(
            UUID userId,
            Long sbuId,
            Long zoneId,
            Long clusterId,
            Long regionId,
            Long unitId,
            Long branchId,
            String branchCode
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("sbuId", sbuId)
                .addValue("zoneId", zoneId)
                .addValue("clusterId", clusterId)
                .addValue("regionId", regionId)
                .addValue("unitId", unitId)
                .addValue("branchId", branchId)
                .addValue("branchCode", branchCode);
        return jdbcTemplate.query(DASHBOARD_METRICS_SQL, params, rs -> {
            if (!rs.next()) {
                return emptyDashboardMetrics();
            }
            return mapDashboardRow(rs);
        });
    }

    private static LoanOutstandingDashboardMetrics mapDashboardRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new LoanOutstandingDashboardMetrics(
                rs.getLong("total_loans"),
                rs.getLong("regular_loans"),
                rs.getLong("od_loans"),
                pctDisplay(rs.getBigDecimal("pct_regular_loans")),
                pctDisplay(rs.getBigDecimal("pct_od_loans")),
                money(rs.getBigDecimal("total_outstanding")),
                money(rs.getBigDecimal("od_outstanding")),
                pctDisplay(rs.getBigDecimal("pct_od_exposure")),
                rs.getInt("fiod")
        );
    }

    private static LoanOutstandingDashboardMetrics emptyDashboardMetrics() {
        return new LoanOutstandingDashboardMetrics(0L, 0L, 0L, 0d, 0d, 0d, 0d, 0d, 0);
    }

    private static double pctDisplay(BigDecimal v) {
        if (v == null) {
            return 0d;
        }
        return v.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double money(BigDecimal v) {
        if (v == null) {
            return 0d;
        }
        return v.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
