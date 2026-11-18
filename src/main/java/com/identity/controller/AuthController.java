package com.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.identity.dto.request.AccountRequestDTO;
import com.identity.dto.request.LoginRequestDTO;
import com.identity.dto.request.RefreshTokenRequestDTO;
import com.identity.dto.response.AccountResponseDTO;
import com.identity.dto.response.TokenResponseDTO;
import com.identity.service.AccountService;
import com.identity.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final AccountService accountService;

	/** Public self-registration: always gets the default USER role (roleId sent by the client is ignored). */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponseDTO register(@Valid @RequestBody AccountRequestDTO request) {
		request.setRoleId(null);
		return accountService.createAccount(request);
	}

	@PostMapping("/login")
	public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public TokenResponseDTO refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
		return authService.refresh(request.getRefreshToken());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
		authService.logout(request.getRefreshToken());
		return ResponseEntity.noContent().build();
	}
}
