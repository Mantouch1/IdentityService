package com.identity.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.dto.request.AuditLogrequestDTO;
import com.identity.dto.response.AuditLogResponseDTO;
import com.identity.entity.Account;
import com.identity.entity.AuditLog;
import com.identity.repository.AccountRepository;
import com.identity.repository.AuditLogRepository;
import com.identity.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

	private final AuditLogRepository auditLogRepository;
	private final AccountRepository accountRepository;

	@Override
	public AuditLogResponseDTO addLog(AuditLogrequestDTO request) {
		Account account = request.getAccountId() == null ? null
				: accountRepository.findById(request.getAccountId()).orElse(null);
		AuditLog saved = auditLogRepository.save(AuditLog.builder()
				.action(request.getAction())
				.ipAddress(request.getIpAddress())
				.account(account)
				.build());
		return toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLogResponseDTO> getLogsByAccount(Long accountId) {
		return auditLogRepository.findByAccountIdOrderByTimestampDesc(accountId).stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLogResponseDTO> getAllLogs() {
		return auditLogRepository.findAllByOrderByTimestampDesc().stream().map(this::toResponse).toList();
	}

	private AuditLogResponseDTO toResponse(AuditLog l) {
		return AuditLogResponseDTO.builder()
				.id(l.getId())
				.accountId(l.getAccount() != null ? l.getAccount().getId() : null)
				.action(l.getAction())
				.timestamp(l.getTimestamp())
				.ipAddress(l.getIpAddress())
				.build();
	}
}
