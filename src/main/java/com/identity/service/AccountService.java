package com.identity.service;

import java.util.List;

import com.identity.dto.request.AccountRequestDTO;
import com.identity.dto.response.AccountResponseDTO;

public interface AccountService {

	AccountResponseDTO createAccount(AccountRequestDTO request);

	AccountResponseDTO updateAccount(Long id, AccountRequestDTO request);

	/** Soft delete: the account is disabled and its refresh tokens are revoked (audit logs are kept). */
	void deleteAccount(Long id);

	AccountResponseDTO getAccountById(Long id);

	AccountResponseDTO getAccountByEmail(String email);

	List<AccountResponseDTO> getAllAccounts();

	void changeStatus(Long id, String status);
}
