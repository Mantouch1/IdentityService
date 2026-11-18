package com.identity.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.identity.dto.response.AuditLogResponseDTO;
import com.identity.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('AUDIT_READ')")
public class AuditLogController {

	private final AuditLogService auditLogService;

	@GetMapping
	public List<AuditLogResponseDTO> get(@RequestParam(required = false) Long accountId) {
		return accountId == null ? auditLogService.getAllLogs() : auditLogService.getLogsByAccount(accountId);
	}
}
