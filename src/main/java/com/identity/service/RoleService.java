package com.identity.service;

import java.util.List;

import com.identity.dto.request.RoleRequestDTO;
import com.identity.dto.response.RoleResponseDTO;

public interface RoleService {

	RoleResponseDTO createRole(RoleRequestDTO request);

	RoleResponseDTO updateRole(Long id, RoleRequestDTO request);

	void deleteRole(Long id);

	RoleResponseDTO getRoleById(Long id);

	List<RoleResponseDTO> getAllRoles();
}
