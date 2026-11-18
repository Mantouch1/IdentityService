package com.identity.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.entity.Account;
import com.identity.entity.AccountStatus;
import com.identity.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final AccountRepository accountRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Account a = accountRepository.findByEmail(email.trim().toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
		List<GrantedAuthority> authorities = new ArrayList<>();
		a.getRoles().forEach(r -> {
			authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
			r.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));
		});
		return User.withUsername(a.getEmail())
				.password(a.getPasswordHash())
				.authorities(authorities)
				.disabled(a.getStatus() != AccountStatus.ACTIVE)
				.build();
	}
}
