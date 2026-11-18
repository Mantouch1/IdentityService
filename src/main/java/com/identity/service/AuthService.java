package com.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.dto.request.AuditLogrequestDTO;
import com.identity.dto.request.LoginRequestDTO;
import com.identity.dto.response.TokenResponseDTO;
import com.identity.entity.Account;
import com.identity.entity.AccountStatus;
import com.identity.entity.RefreshToken;
import com.identity.repository.AccountRepository;
import com.identity.repository.RefreshTokenRepository;
import com.identity.security.JwtService;
import com.identity.util.RequestUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final AuthenticationManager authenticationManager;
	private final AccountRepository accountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtService jwtService;
	private final AuditLogService auditLogService;

	@Value("${app.jwt.refresh-expiration-days}")
	private long refreshExpirationDays;

	/** noRollbackFor: the LOGIN_FAILED audit row must be committed even though we rethrow. */
	@Transactional(noRollbackFor = AuthenticationException.class)
	public TokenResponseDTO login(LoginRequestDTO request) {
		String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
		} catch (AuthenticationException e) {
			Long accountId = accountRepository.findByEmail(email).map(Account::getId).orElse(null);
			audit(accountId, "LOGIN_FAILED (" + email + ")");
			throw e;
		}
		Account account = accountRepository.findByEmail(email).orElseThrow();
		audit(account.getId(), "LOGIN_SUCCESS");
		return issueTokens(account);
	}

	/** Refresh-token rotation: the presented token is revoked and a new pair is issued. */
	@Transactional(noRollbackFor = AuthenticationException.class)
	public TokenResponseDTO refresh(String rawRefreshToken) {
		RefreshToken stored = refreshTokenRepository.findByToken(hash(rawRefreshToken))
				.orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
		Account account = stored.getAccount();
		if (stored.isRevoked()) {
			// a revoked token is being replayed: assume theft, kill every session of this account
			refreshTokenRepository.revokeAllByAccountId(account.getId());
			audit(account.getId(), "REFRESH_TOKEN_REUSE_DETECTED");
			throw new BadCredentialsException("Invalid refresh token");
		}
		if (stored.getExpiryDate().isBefore(LocalDateTime.now()) || account.getStatus() != AccountStatus.ACTIVE) {
			throw new BadCredentialsException("Invalid refresh token");
		}
		stored.setRevoked(true);
		audit(account.getId(), "TOKEN_REFRESHED");
		return issueTokens(account);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenRepository.findByToken(hash(rawRefreshToken)).ifPresent(t -> {
			t.setRevoked(true);
			audit(t.getAccount().getId(), "LOGOUT");
		});
	}

	private TokenResponseDTO issueTokens(Account account) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		refreshTokenRepository.save(RefreshToken.builder()
				.token(hash(rawRefresh))
				.expiryDate(LocalDateTime.now().plusDays(refreshExpirationDays))
				.account(account)
				.build());
		return new TokenResponseDTO(jwtService.generateAccessToken(account), rawRefresh, "Bearer",
				jwtService.getAccessExpirationSeconds());
	}

	private void audit(Long accountId, String action) {
		auditLogService.addLog(AuditLogrequestDTO.builder()
				.accountId(accountId).action(action).ipAddress(RequestUtils.clientIp()).build());
	}

	private static String hash(String raw) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
