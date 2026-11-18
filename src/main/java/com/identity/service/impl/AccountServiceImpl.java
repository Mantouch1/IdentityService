package com.identity.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.dto.request.AccountRequestDTO;
import com.identity.dto.request.AuditLogrequestDTO;
import com.identity.dto.response.AccountResponseDTO;
import com.identity.entity.Account;
import com.identity.entity.AccountStatus;
import com.identity.entity.Permission;
import com.identity.entity.Role;
import com.identity.exception.BadRequestException;
import com.identity.exception.ConflictException;
import com.identity.exception.IdentityServiceException;
import com.identity.exception.ResourceNotFoundException;
import com.identity.repository.AccountRepository;
import com.identity.repository.RefreshTokenRepository;
import com.identity.repository.RoleRepository;
import com.identity.service.AccountService;
import com.identity.service.AuditLogService;
import com.identity.util.RequestUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

	static final String DEFAULT_ROLE = "USER";

	private final AccountRepository accountRepository;
	private final RoleRepository roleRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditLogService auditLogService;

	@Override
	public AccountResponseDTO createAccount(AccountRequestDTO request) {
		String email = normalizeEmail(request.getEmail());
		String username = request.getUsername().trim();
		if (accountRepository.existsByEmail(email)) {
			throw new ConflictException("Email already in use: " + email);
		}
		if (accountRepository.existsByUsername(username)) {
			throw new ConflictException("Username already in use: " + username);
		}
		Role role = request.getRoleId() != null ? findRole(request.getRoleId())
				: roleRepository.findByName(DEFAULT_ROLE)
						.orElseThrow(() -> new IdentityServiceException("Default role USER is missing"));

		Account account = Account.builder()
				.username(username)
				.email(email)
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.phone(request.getPhone())
				.roles(new HashSet<>(Set.of(role)))
				.build();
		Account saved = accountRepository.save(account);
		audit(saved, "ACCOUNT_CREATED by " + RequestUtils.currentUser());
		return toResponse(saved);
	}

	@Override
	public AccountResponseDTO updateAccount(Long id, AccountRequestDTO request) {
		Account account = find(id);

		if (hasText(request.getEmail())) {
			String email = normalizeEmail(request.getEmail());
			if (!email.equals(account.getEmail()) && accountRepository.existsByEmail(email)) {
				throw new ConflictException("Email already in use: " + email);
			}
			account.setEmail(email);
		}
		if (hasText(request.getUsername())) {
			String username = request.getUsername().trim();
			if (!username.equals(account.getUsername()) && accountRepository.existsByUsername(username)) {
				throw new ConflictException("Username already in use: " + username);
			}
			account.setUsername(username);
		}
		if (request.getFirstName() != null) {
			account.setFirstName(request.getFirstName());
		}
		if (request.getLastName() != null) {
			account.setLastName(request.getLastName());
		}
		if (request.getPhone() != null) {
			account.setPhone(request.getPhone());
		}
		if (hasText(request.getPassword())) {
			if (request.getPassword().length() < 8 || request.getPassword().length() > 72) {
				throw new BadRequestException("Password must be between 8 and 72 characters");
			}
			account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
			refreshTokenRepository.revokeAllByAccountId(id); // force re-login everywhere
		}
		if (request.getRoleId() != null) {
			account.setRoles(new HashSet<>(Set.of(findRole(request.getRoleId()))));
		}
		audit(account, "ACCOUNT_UPDATED by " + RequestUtils.currentUser());
		return toResponse(account);
	}

	@Override
	public void deleteAccount(Long id) {
		Account account = find(id);
		account.setStatus(AccountStatus.DISABLED);
		refreshTokenRepository.revokeAllByAccountId(id);
		audit(account, "ACCOUNT_DELETED (soft) by " + RequestUtils.currentUser());
	}

	@Override
	@Transactional(readOnly = true)
	public AccountResponseDTO getAccountById(Long id) {
		return toResponse(find(id));
	}

	@Override
	@Transactional(readOnly = true)
	public AccountResponseDTO getAccountByEmail(String email) {
		return toResponse(accountRepository.findByEmail(normalizeEmail(email))
				.orElseThrow(() -> new ResourceNotFoundException("Account not found")));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AccountResponseDTO> getAllAccounts() {
		return accountRepository.findAll().stream().map(this::toResponse).toList();
	}

	@Override
	public void changeStatus(Long id, String status) {
		AccountStatus newStatus;
		try {
			newStatus = AccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new BadRequestException("Invalid status, expected one of ACTIVE, SUSPENDED, DISABLED");
		}
		Account account = find(id);
		account.setStatus(newStatus);
		if (newStatus != AccountStatus.ACTIVE) {
			refreshTokenRepository.revokeAllByAccountId(id);
		}
		audit(account, "ACCOUNT_STATUS_" + newStatus + " by " + RequestUtils.currentUser());
	}

	// ---------- helpers ----------

	private Account find(Long id) {
		return accountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found with id " + id));
	}

	private Role findRole(Long id) {
		return roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found with id " + id));
	}

	private void audit(Account account, String action) {
		auditLogService.addLog(AuditLogrequestDTO.builder()
				.accountId(account.getId())
				.action(action)
				.ipAddress(RequestUtils.clientIp())
				.build());
	}

	private static boolean hasText(String s) {
		return s != null && !s.isBlank();
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private AccountResponseDTO toResponse(Account a) {
		return AccountResponseDTO.builder()
				.id(a.getId())
				.username(a.getUsername())
				.email(a.getEmail())
				.firstName(a.getFirstName())
				.lastName(a.getLastName())
				.phone(a.getPhone())
				.status(a.getStatus().name())
				.roles(a.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(TreeSet::new)))
				.permissions(a.getRoles().stream().flatMap(r -> r.getPermissions().stream())
						.map(Permission::getName).collect(Collectors.toCollection(TreeSet::new)))
				.createdAt(a.getCreatedAt())
				.build();
	}
}
