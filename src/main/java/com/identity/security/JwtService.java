package com.identity.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.identity.entity.Account;
import com.identity.entity.Permission;
import com.identity.entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final long accessExpirationSeconds;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-expiration-seconds}") long accessExpirationSeconds) {
		if (secret.length() < 32) {
			throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 characters");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessExpirationSeconds = accessExpirationSeconds;
	}

	public long getAccessExpirationSeconds() {
		return accessExpirationSeconds;
	}

	public String generateAccessToken(Account account) {
		Date now = new Date();
		return Jwts.builder()
				.subject(account.getEmail())
				.claim("uid", account.getId())
				.claim("username", account.getUsername())
				.claim("roles", account.getRoles().stream().map(Role::getName)
						.collect(Collectors.toCollection(TreeSet::new)))
				.claim("permissions", account.getRoles().stream().flatMap(r -> r.getPermissions().stream())
						.map(Permission::getName).collect(Collectors.toCollection(TreeSet::new)))
				.issuedAt(now)
				.expiration(new Date(now.getTime() + accessExpirationSeconds * 1000))
				.signWith(key)
				.compact();
	}

	/** Returns the claims if the signature is valid and the token is not expired, else throws JwtException. */
	public Claims parse(String token) throws JwtException {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	public List<String> extractList(Claims claims, String name) {
		Object value = claims.get(name);
		if (value instanceof Collection<?> c) {
			return c.stream().map(String::valueOf).toList();
		}
		return List.of();
	}
}
