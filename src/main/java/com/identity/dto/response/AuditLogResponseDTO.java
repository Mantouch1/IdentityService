package com.identity.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponseDTO {

	private Long id;
	private Long accountId;
	private String action;
	private LocalDateTime timestamp;
	private String ipAddress;
}
