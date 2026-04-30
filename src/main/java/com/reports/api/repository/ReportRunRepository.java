package com.reports.api.repository;

import com.reports.api.model.ReportRun;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRunRepository extends JpaRepository<ReportRun, UUID> {
    List<ReportRun> findByUserIdOrderByRequestedAtDesc(UUID userId);

    Optional<ReportRun> findByIdAndUserId(UUID id, UUID userId);
}
