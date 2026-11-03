package com.nexushr.auth.repository;

import com.nexushr.auth.entity.User;
import com.nexushr.auth.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);
}