package com.identity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.identity.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findByAccountIdOrderByTimestampDesc(Long accountId);

	List<AuditLog> findAllByOrderByTimestampDesc();
}
