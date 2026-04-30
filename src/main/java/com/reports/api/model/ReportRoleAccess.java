package com.reports.api.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_role_access")
public class ReportRoleAccess {
    @EmbeddedId
    private ReportRoleAccessId id;

    public ReportRoleAccessId getId() {
        return id;
    }
}
