package com.identity.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponseDTO {

	private Long id;
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private String phone;
	private String status;
	private Set<String> roles;
	/** Effective permissions (union of the permissions of all roles). */
	private Set<String> permissions;
	private LocalDateTime createdAt;
}
