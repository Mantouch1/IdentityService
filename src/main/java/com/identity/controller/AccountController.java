package com.identity.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.identity.dto.request.AccountRequestDTO;
import com.identity.dto.response.AccountResponseDTO;
import com.identity.service.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accountService;

	/** Called by other microservices (Feign) with the user's token: who am I, which roles and permissions? */
	@GetMapping("/me")
	public AccountResponseDTO me(Principal principal) {
		return accountService.getAccountByEmail(principal.getName());
	}

	@GetMapping
	@PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
	public List<AccountResponseDTO> getAll() {
		return accountService.getAllAccounts();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
	public AccountResponseDTO getById(@PathVariable Long id) {
		return accountService.getAccountById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
	public AccountResponseDTO create(@Valid @RequestBody AccountRequestDTO request) {
		return accountService.createAccount(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
	public AccountResponseDTO update(@PathVariable Long id, @RequestBody AccountRequestDTO request) {
		return accountService.updateAccount(id, request);
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
	public ResponseEntity<Void> changeStatus(@PathVariable Long id, @RequestParam String value) {
		accountService.changeStatus(id, value);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		accountService.deleteAccount(id);
		return ResponseEntity.noContent().build();
	}
}
