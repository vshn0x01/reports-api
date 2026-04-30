package com.reports.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReportRunFilterId implements Serializable {
    @Column(name = "report_run_id")
    private UUID reportRunId;

    @Column(name = "filter_key")
    private String filterKey;

    @Column(name = "filter_value")
    private String filterValue;

    public void setReportRunId(UUID reportRunId) {
        this.reportRunId = reportRunId;
    }

    public void setFilterKey(String filterKey) {
        this.filterKey = filterKey;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportRunFilterId that)) {
            return false;
        }
        return Objects.equals(reportRunId, that.reportRunId)
                && Objects.equals(filterKey, that.filterKey)
                && Objects.equals(filterValue, that.filterValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportRunId, filterKey, filterValue);
    }
}
