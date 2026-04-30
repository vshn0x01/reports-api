package com.reports.api.repository;

import com.reports.api.model.Report;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    @Query("""
            select distinct r
            from Report r, ReportRoleAccess ra
            where ra.id.reportId = r.id
              and r.active = true
              and ra.id.roleId in :roleIds
            """)
    List<Report> findActiveReportsByRoleIds(List<Short> roleIds);

    @Query("""
            select r
            from Report r, ReportRoleAccess ra
            where ra.id.reportId = r.id
              and r.id = :reportId
              and r.active = true
              and ra.id.roleId in :roleIds
            """)
    Optional<Report> findAccessibleReport(UUID reportId, List<Short> roleIds);
}
