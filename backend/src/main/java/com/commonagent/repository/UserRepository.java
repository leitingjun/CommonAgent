package com.commonagent.repository;

import com.commonagent.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTenantIdAndUserId(String tenantId, String userId);
    boolean existsByTenantIdAndUserId(String tenantId, String userId);
}
