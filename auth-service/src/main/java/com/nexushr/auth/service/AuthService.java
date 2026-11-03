package com.nexushr.auth.service;

import com.nexushr.auth.dto.*;
import com.nexushr.auth.entity.*;
import com.nexushr.auth.repository.*;
import com.nexushr.auth.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 14;
    private static final long INVITATION_VALIDITY_DAYS = 7;
    private static final long PASSWORD_RESET_TOKEN_VALIDITY_MINUTES = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserInvitationRepository invitationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetRateLimiter passwordResetRateLimiter;
    private final AuthAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean passwordResetEmailEnabled;
    private final String frontendBaseUrl;
    private final long passwordResetTokenValidityMinutes;

    public AuthService(
        UserRepository userRepository,
        UserInvitationRepository invitationRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordResetRateLimiter passwordResetRateLimiter,
        AuthAuditService auditService,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        ApplicationEventPublisher eventPublisher,
        @Value("${app.auth.password-reset.email-enabled:false}")
        boolean passwordResetEmailEnabled,
        @Value("${app.auth.password-reset.frontend-base-url:http://localhost:3000}")
        String frontendBaseUrl,
        @Value("${app.auth.password-reset.token-ttl-minutes:30}")
        long passwordResetTokenValidityMinutes
    ) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetRateLimiter = passwordResetRateLimiter;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.passwordResetEmailEnabled = passwordResetEmailEnabled;
        this.frontendBaseUrl = frontendBaseUrl;
        if (passwordResetTokenValidityMinutes <= 0 ||
            passwordResetTokenValidityMinutes > 60) {
            throw new IllegalArgumentException(
                "Password reset token lifetime must be between 1 and 60 minutes"
            );
        }
        this.passwordResetTokenValidityMinutes = passwordResetTokenValidityMinutes;
    }

    @Transactional
    public InvitationResponse createInvitation(
        InvitationRequest request, String inviterEmail, boolean canInvitePrivilegedRoles
    ) {
        String email = normalizeEmail(request.email());
        Role role = request.role();

        if (!canInvitePrivilegedRoles &&
            (role == Role.SUPER_ADMIN || role == Role.HR_ADMIN ||
             role == Role.PAYROLL_ADMIN)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only a super administrator can invite this role"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is already registered"
            );
        }

        Instant now = Instant.now();
        if (invitationRepository
            .existsByEmailIgnoreCaseAndAcceptedAtIsNullAndExpiresAtAfter(email, now)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "A valid invitation already exists for this email"
            );
        }

        String rawToken = generateSecret();
        Instant expiresAt = now.plus(INVITATION_VALIDITY_DAYS, ChronoUnit.DAYS);
        invitationRepository.save(new UserInvitation(
            email, role, hashToken(rawToken), normalizeEmail(inviterEmail), expiresAt
        ));
        auditService.record("INVITATION_CREATED", inviterEmail, email);

        return new InvitationResponse(email, role, rawToken, expiresAt);
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        UserInvitation invitation = invitationRepository
            .findByTokenHashForUpdate(hashToken(request.invitationToken()))
            .filter(value -> value.isUsableAt(Instant.now()))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invitation token is invalid or expired"
            ));

        if (!invitation.getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invitation email does not match registration email"
            );
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is already registered"
            );
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(invitation.getRole());
        user.setActive(true);

        User saved = userRepository.save(user);
        invitation.accept(Instant.now());
        auditService.record("REGISTRATION_COMPLETED", email, email);
        return createSession(saved);
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException ex) {
            auditService.recordLoginFailure(email);
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password"
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
            .filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password"
            ));

        auditService.record("LOGIN_SUCCEEDED", email, email);
        return createSession(user);
    }

    @Transactional
    public AuthSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalidRefreshToken();
        }

        Instant now = Instant.now();
        RefreshToken storedToken = refreshTokenRepository
            .findByTokenHashForUpdate(hashToken(rawRefreshToken))
            .filter(token -> token.isUsableAt(now))
            .orElseThrow(AuthService::invalidRefreshToken);

        storedToken.revoke(now);
        refreshTokenRepository.save(storedToken);
        auditService.record("TOKEN_REFRESHED",
            storedToken.getUser().getEmail(), storedToken.getUser().getEmail());
        return createSession(storedToken.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHashForUpdate(hashToken(rawRefreshToken))
            .ifPresent(token -> {
                token.revoke(Instant.now());
                refreshTokenRepository.save(token);
                auditService.record("LOGOUT", token.getUser().getEmail(),
                    token.getUser().getEmail());
            });
    }

    @Transactional
    public void requestPasswordReset(String rawEmail) {
        if (!passwordResetEmailEnabled) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Password reset email delivery is not configured"
            );
        }

        String email = normalizeEmail(rawEmail);
        Instant now = Instant.now();
        passwordResetTokenRepository.deleteExpiredOrConsumedBefore(
            now.minus(1, ChronoUnit.DAYS)
        );
        if (!passwordResetRateLimiter.allowRequest(email, now)) {
            auditService.record("PASSWORD_RESET_THROTTLED", null, email);
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email)
            .filter(User::isActive)
            .orElse(null);
        if (user == null) {
            auditService.record("PASSWORD_RESET_REQUESTED", null, email);
            return;
        }
        passwordResetTokenRepository.consumeUnusedForUser(user.getId(), now);
        String rawToken = generateSecret();
        passwordResetTokenRepository.save(new PasswordResetToken(
            user,
            hashToken(rawToken),
            now.plus(passwordResetTokenValidityMinutes, ChronoUnit.MINUTES)
        ));

        String resetUrl = UriComponentsBuilder
            .fromUriString(frontendBaseUrl)
            .replacePath("/reset-password")
            .replaceQuery(null)
            .fragment("token=" + rawToken)
            .build()
            .toUriString();
        eventPublisher.publishEvent(new PasswordResetEmailRequested(
            user.getEmail(),
            resetUrl,
            email,
            passwordResetTokenValidityMinutes
        ));
        auditService.record("PASSWORD_RESET_REQUESTED", null, email);
    }

    @Transactional
    public void completePasswordReset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidPasswordResetToken();
        }

        Instant now = Instant.now();
        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHashForUpdate(hashToken(rawToken))
            .filter(token -> token.isUsableAt(now))
            .orElseThrow(AuthService::invalidPasswordResetToken);
        User user = resetToken.getUser();
        resetToken.consume(now);
        passwordResetTokenRepository.save(resetToken);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
        passwordResetTokenRepository.consumeUnusedForUser(user.getId(), now);
        auditService.record(
            "PASSWORD_RESET_COMPLETED", user.getEmail(), user.getEmail()
        );
    }

    private AuthSession createSession(User user) {
        UserDetails details = org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .roles(user.getRole().name())
            .build();

        String accessToken = jwtService.generateToken(details);
        String refreshToken = generateSecret();
        Instant expiresAt = Instant.now().plus(
            REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS
        );
        refreshTokenRepository.save(new RefreshToken(
            user, hashToken(refreshToken), expiresAt
        ));

        return new AuthSession(
            user,
            accessToken,
            refreshToken,
            jwtService.getExpirationSeconds(),
            REFRESH_TOKEN_VALIDITY_DAYS * 24 * 60 * 60
        );
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"
        );
    }

    private static ResponseStatusException invalidPasswordResetToken() {
        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Password reset token is invalid or expired"
        );
    }
}
