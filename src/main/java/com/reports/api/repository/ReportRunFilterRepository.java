package com.reports.api.repository;

import com.reports.api.model.ReportRunFilter;
import com.reports.api.model.ReportRunFilterId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRunFilterRepository extends JpaRepository<ReportRunFilter, ReportRunFilterId> {
}
