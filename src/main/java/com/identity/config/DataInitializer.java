package com.identity.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.identity.entity.Account;
import com.identity.entity.Permission;
import com.identity.entity.Role;
import com.identity.repository.AccountRepository;
import com.identity.repository.PermissionRepository;
import com.identity.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds the default permissions and roles (only the missing ones, nothing is overwritten)
 * and, optionally, a bootstrap admin account.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private static final Map<String, String> PERMISSIONS = new LinkedHashMap<>();
	static {
		PERMISSIONS.put("TENDER_READ", "Read OPEN tenders");
		PERMISSIONS.put("TENDER_READ_ALL", "Read tenders in every status");
		PERMISSIONS.put("TENDER_WRITE", "Create and update tenders");
		PERMISSIONS.put("TENDER_DELETE", "Delete tenders");
		PERMISSIONS.put("TENDER_STATUS_MANAGE", "Create, update and delete tender statuses");
		PERMISSIONS.put("NOTIFICATION_SEND", "Send notifications (NotificationService)");
		PERMISSIONS.put("NOTIFICATION_READ", "Read the notification history (NotificationService)");
		PERMISSIONS.put("DOCUMENT_READ", "Read and download tender files (DocumentService)");
		PERMISSIONS.put("DOCUMENT_WRITE", "Upload tender files (DocumentService)");
		PERMISSIONS.put("DOCUMENT_DELETE", "Delete tender files (DocumentService)");
		PERMISSIONS.put("AUDIT_WRITE", "Publish events to the reporting/audit service");
		PERMISSIONS.put("REPORT_READ", "Read audit events and reports (ReportingService)");
		PERMISSIONS.put("SCHEDULER_MANAGE", "Manage and trigger scheduled jobs (SchedulerService)");
		PERMISSIONS.put("SEARCH_INDEX", "Rebuild the search index (SearchService)");
		PERMISSIONS.put("ACCOUNT_MANAGE", "Manage accounts");
		PERMISSIONS.put("ROLE_MANAGE", "Manage roles and permissions");
		PERMISSIONS.put("AUDIT_READ", "Read audit logs");
	}

	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;
	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.admin.username:admin}")
	private String adminUsername;
	@Value("${app.admin.email:}")
	private String adminEmail;
	@Value("${app.admin.password:}")
	private String adminPassword;
	@Value("${app.service-account.email:}")
	private String serviceEmail;
	@Value("${app.service-account.password:}")
	private String servicePassword;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		PERMISSIONS.forEach((name, desc) -> {
			if (!permissionRepository.existsByName(name)) {
				permissionRepository.save(Permission.builder().name(name).description(desc).build());
			}
		});

		// ADMIN always owns every permission (missing ones are added at each startup).
		seedRole("ADMIN", "Full access to everything", PERMISSIONS.keySet(), true);
		seedRole("SECURITY_ADMIN", "Manages accounts, roles and audit, no access to tender content",
				Set.of("ACCOUNT_MANAGE", "ROLE_MANAGE", "AUDIT_READ"), false);
		seedRole("MANAGER", "Manages tenders, statuses and files",
				Set.of("TENDER_READ_ALL", "TENDER_WRITE", "TENDER_DELETE", "TENDER_STATUS_MANAGE",
						"NOTIFICATION_SEND", "NOTIFICATION_READ", "DOCUMENT_READ", "DOCUMENT_WRITE", "DOCUMENT_DELETE",
						"REPORT_READ"), false);
		seedRole("EDITOR", "Creates and edits tenders and files, cannot delete",
				Set.of("TENDER_READ_ALL", "TENDER_WRITE", "DOCUMENT_READ", "DOCUMENT_WRITE"), false);
		seedRole("AUDITOR", "Read-only supervisor: all tenders, audit logs and reports",
				Set.of("TENDER_READ_ALL", "AUDIT_READ", "REPORT_READ"), false);
		// technical role for service-to-service calls (never for people)
		seedRole("SERVICE", "Technical account used by the microservices",
				Set.of("TENDER_READ_ALL", "NOTIFICATION_SEND", "AUDIT_WRITE", "SEARCH_INDEX"), false);
		seedRole("USER", "Reads open tenders only", Set.of("TENDER_READ"), false);

		if (!serviceEmail.isBlank() && !servicePassword.isBlank()
				&& !accountRepository.existsByEmail(serviceEmail.trim().toLowerCase())) {
			Role serviceRole = roleRepository.findByName("SERVICE").orElseThrow();
			accountRepository.save(Account.builder()
					.username("service")
					.email(serviceEmail.trim().toLowerCase())
					.passwordHash(passwordEncoder.encode(servicePassword))
					.firstName("Service")
					.roles(new HashSet<>(List.of(serviceRole)))
					.build());
			log.info("Technical service account created: {}", serviceEmail);
		}

		if (adminEmail.isBlank() || adminPassword.isBlank()) {
			log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set: no bootstrap admin created");
			return;
		}
		String email = adminEmail.trim().toLowerCase();
		if (!accountRepository.existsByEmail(email)) {
			Role admin = roleRepository.findByName("ADMIN").orElseThrow();
			accountRepository.save(Account.builder()
					.username(adminUsername)
					.email(email)
					.passwordHash(passwordEncoder.encode(adminPassword))
					.firstName("Admin")
					.roles(new HashSet<>(List.of(admin)))
					.build());
			log.info("Bootstrap admin account created: {}", email);
		}
	}

	private void seedRole(String name, String description, Set<String> permissionNames, boolean additive) {
		Set<Permission> perms = new HashSet<>();
		permissionNames.forEach(p -> permissionRepository.findByName(p).ifPresent(perms::add));
		Role existing = roleRepository.findByName(name).orElse(null);
		if (existing == null) {
			roleRepository.save(Role.builder().name(name).description(description).permissions(perms).build());
		} else if (additive) {
			existing.getPermissions().addAll(perms);
		}
		// other existing roles are never modified: an admin may have customised them
	}
}
