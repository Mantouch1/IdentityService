package com.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Used for create (validated with @Valid in the controller) and for partial update
 * (null / blank fields are ignored by the service).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequestDTO {

	@NotBlank
	@Size(min = 3, max = 50)
	private String username;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 8, max = 72)
	private String password;

	private String firstName;
	private String lastName;
	private String phone;

	/** Optional: defaults to the USER role. Ignored on public registration. */
	private Long roleId;
}
