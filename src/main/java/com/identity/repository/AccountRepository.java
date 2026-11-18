package com.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.identity.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByRoles_Id(Long roleId);
}
