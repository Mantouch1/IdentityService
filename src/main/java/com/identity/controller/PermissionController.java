package com.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.identity.dto.request.PermissionRequestDTO;
import com.identity.dto.response.PermissionResponseDTO;
import com.identity.service.PermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class PermissionController {

	private final PermissionService permissionService;

	@GetMapping
	public List<PermissionResponseDTO> getAll() {
		return permissionService.getAllPermissions();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PermissionResponseDTO create(@Valid @RequestBody PermissionRequestDTO request) {
		return permissionService.createPermission(request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		permissionService.deletePermission(id);
		return ResponseEntity.noContent().build();
	}
}
