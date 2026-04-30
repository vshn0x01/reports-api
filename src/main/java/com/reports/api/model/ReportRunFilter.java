package com.reports.api.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_run_filters")
public class ReportRunFilter {
    @EmbeddedId
    private ReportRunFilterId id;

    public ReportRunFilter() {
    }

    public ReportRunFilter(ReportRunFilterId id) {
        this.id = id;
    }
}
