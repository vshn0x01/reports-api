package com.reports.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record LoanOutstandingSummaryRow(
        /** Matches {@code main.tdw_loan_outstanding_information.branch_id} (often a branch code). */
        @JsonProperty("branch_id") String branchId,
        @JsonProperty("branch_name") String branchName,
        long loans,
        BigDecimal outstanding,
        @JsonProperty("od_loans") long odLoans,
        @JsonProperty("od_amount") BigDecimal odAmount
) {
}
