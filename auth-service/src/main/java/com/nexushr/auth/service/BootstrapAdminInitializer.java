package com.nexushr.auth.service;

import com.nexushr.auth.entity.Role;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService auditService;
    private final String fullName;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthAuditService auditService,
        @Value("${app.bootstrap-admin.full-name:}") String fullName,
        @Value("${app.bootstrap-admin.email:}") String email,
        @Value("${app.bootstrap-admin.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < 16 || password.length() > 72) {
            throw new IllegalStateException(
                "Bootstrap administrator password must contain 16 to 72 characters"
            );
        }
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException(
                "Bootstrap administrator email is already assigned to a non-admin user"
            );
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.SUPER_ADMIN);
        user.setActive(true);
        userRepository.save(user);
        auditService.record("BOOTSTRAP_ADMIN_CREATED", user.getEmail(), user.getEmail());
    }
}
