package com.identity.service;

import java.util.List;

import com.identity.dto.request.AuditLogrequestDTO;
import com.identity.dto.response.AuditLogResponseDTO;

public interface AuditLogService {

	AuditLogResponseDTO addLog(AuditLogrequestDTO request);

	List<AuditLogResponseDTO> getLogsByAccount(Long accountId);

	List<AuditLogResponseDTO> getAllLogs();
}
