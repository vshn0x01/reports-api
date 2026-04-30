package com.reports.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReportRoleAccessId implements Serializable {
    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "role_id")
    private Short roleId;

    public UUID getReportId() {
        return reportId;
    }

    public Short getRoleId() {
        return roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportRoleAccessId that)) {
            return false;
        }
        return Objects.equals(reportId, that.reportId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, roleId);
    }
}
