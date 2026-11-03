package com.nexushr.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.auth.entity.Role;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthAuditService auditService;

    @Test
    void createsBootstrapAdminWhenLegacyEmployeesExistWithoutSuperAdmin() {
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@example.test"))
            .thenReturn(false);
        when(passwordEncoder.encode("strong-password-123"))
            .thenReturn("encoded-password");

        var initializer = new BootstrapAdminInitializer(
            userRepository,
            passwordEncoder,
            auditService,
            "Development Administrator",
            "Admin@example.test",
            "strong-password-123"
        );

        initializer.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.SUPER_ADMIN, captor.getValue().getRole());
        assertEquals("admin@example.test", captor.getValue().getEmail());
        assertEquals("encoded-password", captor.getValue().getPasswordHash());
        verify(auditService).record(
            "BOOTSTRAP_ADMIN_CREATED", "admin@example.test", "admin@example.test"
        );
    }

    @Test
    void doesNotCreateAnotherSuperAdmin() {
        when(userRepository.existsByRole(Role.SUPER_ADMIN)).thenReturn(true);
        var initializer = new BootstrapAdminInitializer(
            userRepository,
            passwordEncoder,
            auditService,
            "Development Administrator",
            "admin@example.test",
            "strong-password-123"
        );

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any(User.class));
    }
}
