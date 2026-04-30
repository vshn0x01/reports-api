package com.reports.api.repository;

import com.reports.api.model.UserRole;
import com.reports.api.model.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    @Query("select ur.id.roleId from UserRole ur where ur.id.userId = :userId")
    List<Short> findRoleIdsByUserId(UUID userId);
}
