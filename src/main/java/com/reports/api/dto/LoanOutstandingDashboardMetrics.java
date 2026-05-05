package com.reports.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Single-row KPIs for the outstanding loan dashboard. Uses primitive doubles for JSON
 * (plain numbers, no scientific notation from BigDecimal).
 */
@JsonPropertyOrder({
        "fiod",
        "total_loans",
        "regular_loans",
        "od_loans",
        "pct_regular_loans",
        "pct_od_loans",
        "total_outstanding",
        "od_outstanding",
        "pct_od_exposure"
})
public record LoanOutstandingDashboardMetrics(
        @JsonProperty("total_loans") long totalLoans,
        @JsonProperty("regular_loans") long regularLoans,
        @JsonProperty("od_loans") long odLoans,
        @JsonProperty("pct_regular_loans") double pctRegularLoans,
        @JsonProperty("pct_od_loans") double pctOdLoans,
        @JsonProperty("total_outstanding") double totalOutstanding,
        @JsonProperty("od_outstanding") double odOutstanding,
        @JsonProperty("pct_od_exposure") double pctOdExposure,
        int fiod
) {}
