package com.identity.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.dto.request.RoleRequestDTO;
import com.identity.dto.response.RoleResponseDTO;
import com.identity.entity.Permission;
import com.identity.entity.Role;
import com.identity.exception.BadRequestException;
import com.identity.exception.ConflictException;
import com.identity.exception.ResourceNotFoundException;
import com.identity.repository.AccountRepository;
import com.identity.repository.PermissionRepository;
import com.identity.repository.RoleRepository;
import com.identity.service.RoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final AccountRepository accountRepository;

	@Override
	public RoleResponseDTO createRole(RoleRequestDTO request) {
		String name = request.getName().trim().toUpperCase();
		if (roleRepository.existsByName(name)) {
			throw new ConflictException("Role already exists: " + name);
		}
		Role role = Role.builder()
				.name(name)
				.description(request.getDescription())
				.permissions(loadPermissions(request.getPermissionIds()))
				.build();
		return toResponse(roleRepository.save(role));
	}

	@Override
	public RoleResponseDTO updateRole(Long id, RoleRequestDTO request) {
		Role role = find(id);
		String name = request.getName().trim().toUpperCase();
		if (!name.equals(role.getName()) && roleRepository.existsByName(name)) {
			throw new ConflictException("Role already exists: " + name);
		}
		role.setName(name);
		role.setDescription(request.getDescription());
		role.setPermissions(loadPermissions(request.getPermissionIds()));
		return toResponse(role);
	}

	@Override
	public void deleteRole(Long id) {
		Role role = find(id);
		if (accountRepository.existsByRoles_Id(id)) {
			throw new ConflictException("Role is still assigned to at least one account");
		}
		roleRepository.delete(role);
	}

	@Override
	@Transactional(readOnly = true)
	public RoleResponseDTO getRoleById(Long id) {
		return toResponse(find(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoleResponseDTO> getAllRoles() {
		return roleRepository.findAll().stream().map(this::toResponse).toList();
	}

	private Role find(Long id) {
		return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role not found with id " + id));
	}

	private Set<Permission> loadPermissions(Set<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return new HashSet<>();
		}
		List<Permission> found = permissionRepository.findAllById(ids);
		if (found.size() != ids.size()) {
			throw new BadRequestException("One or more permission ids do not exist");
		}
		return new HashSet<>(found);
	}

	private RoleResponseDTO toResponse(Role r) {
		return RoleResponseDTO.builder()
				.id(r.getId())
				.name(r.getName())
				.description(r.getDescription())
				.permissions(r.getPermissions().stream().map(Permission::getName).collect(Collectors.toCollection(TreeSet::new)))
				.build();
	}
}
