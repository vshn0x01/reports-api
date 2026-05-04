package com.reports.api.service;

import com.reports.api.dto.LoanOutstandingSummaryRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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
}
