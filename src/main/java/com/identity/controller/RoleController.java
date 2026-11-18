package com.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.identity.dto.request.RoleRequestDTO;
import com.identity.dto.response.RoleResponseDTO;
import com.identity.service.RoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class RoleController {

	private final RoleService roleService;

	@GetMapping
	public List<RoleResponseDTO> getAll() {
		return roleService.getAllRoles();
	}

	@GetMapping("/{id}")
	public RoleResponseDTO getById(@PathVariable Long id) {
		return roleService.getRoleById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoleResponseDTO create(@Valid @RequestBody RoleRequestDTO request) {
		return roleService.createRole(request);
	}

	@PutMapping("/{id}")
	public RoleResponseDTO update(@PathVariable Long id, @Valid @RequestBody RoleRequestDTO request) {
		return roleService.updateRole(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		roleService.deleteRole(id);
		return ResponseEntity.noContent().build();
	}
}
