package com.identity.service;

import java.util.List;

import com.identity.dto.request.PermissionRequestDTO;
import com.identity.dto.response.PermissionResponseDTO;

public interface PermissionService {

	PermissionResponseDTO createPermission(PermissionRequestDTO request);

	void deletePermission(Long id);

	List<PermissionResponseDTO> getAllPermissions();
}
