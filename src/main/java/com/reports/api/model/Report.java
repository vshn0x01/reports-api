package com.reports.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column
    private String description;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 20)
    private String cadence;

    @Column(name = "output_format", nullable = false, length = 10)
    private String outputFormat;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * Native SELECT (or WITH … SELECT) run for Excel export; one statement only.
     * Trusted DB admin content — see export validation in {@link com.reports.api.service.ReportService}.
     */
    @Column(name = "export_sql", columnDefinition = "text")
    private String exportSql;

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getCadence() {
        return cadence;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getExportSql() {
        return exportSql;
    }
}
