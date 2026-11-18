package com.identity.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.identity.dto.request.PermissionRequestDTO;
import com.identity.dto.response.PermissionResponseDTO;
import com.identity.entity.Permission;
import com.identity.exception.ConflictException;
import com.identity.exception.ResourceNotFoundException;
import com.identity.repository.PermissionRepository;
import com.identity.repository.RoleRepository;
import com.identity.service.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionServiceImpl implements PermissionService {

	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;

	@Override
	public PermissionResponseDTO createPermission(PermissionRequestDTO request) {
		String name = request.getName().trim().toUpperCase();
		if (permissionRepository.existsByName(name)) {
			throw new ConflictException("Permission already exists: " + name);
		}
		return toResponse(permissionRepository.save(
				Permission.builder().name(name).description(request.getDescription()).build()));
	}

	@Override
	public void deletePermission(Long id) {
		Permission p = permissionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Permission not found with id " + id));
		if (roleRepository.existsByPermissions_Id(id)) {
			throw new ConflictException("Permission is still used by at least one role");
		}
		permissionRepository.delete(p);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PermissionResponseDTO> getAllPermissions() {
		return permissionRepository.findAll().stream().map(this::toResponse).toList();
	}

	private PermissionResponseDTO toResponse(Permission p) {
		return PermissionResponseDTO.builder().id(p.getId()).name(p.getName()).description(p.getDescription()).build();
	}
}
