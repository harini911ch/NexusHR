package com.nexushr.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.auth.dto.InvitationRequest;
import com.nexushr.auth.dto.RegisterRequest;
import com.nexushr.auth.entity.RefreshToken;
import com.nexushr.auth.entity.PasswordResetToken;
import com.nexushr.auth.entity.Role;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.entity.UserInvitation;
import com.nexushr.auth.repository.RefreshTokenRepository;
import com.nexushr.auth.repository.PasswordResetTokenRepository;
import com.nexushr.auth.repository.UserInvitationRepository;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.auth.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserInvitationRepository invitationRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordResetRateLimiter passwordResetRateLimiter;
    @Mock private AuthAuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
            userRepository,
            invitationRepository,
            refreshTokenRepository,
            passwordResetTokenRepository,
            passwordResetRateLimiter,
            auditService,
            passwordEncoder,
            authenticationManager,
            jwtService,
            eventPublisher,
            true,
            "http://localhost:3000",
            30
        );
    }

    @Test
    void invitationReturnsOneTimeSecretAndStoresOnlyItsHash() throws Exception {
        when(invitationRepository
            .existsByEmailIgnoreCaseAndAcceptedAtIsNullAndExpiresAtAfter(
                anyString(), any(Instant.class)))
            .thenReturn(false);

        var response = service.createInvitation(
            new InvitationRequest("New.Employee@example.com", Role.EMPLOYEE),
            "admin@example.com",
            false
        );

        ArgumentCaptor<UserInvitation> captor =
            ArgumentCaptor.forClass(UserInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertEquals("new.employee@example.com", response.email());
        assertEquals(Role.EMPLOYEE, response.role());
        assertNotNull(response.invitationToken());
        assertFalse(response.invitationToken().isBlank());
        assertEquals(sha256(response.invitationToken()), captor.getValue().getTokenHash());
        assertFalse(captor.getValue().getTokenHash().equals(response.invitationToken()));
    }

    @Test
    void hrAdminCannotInvitePrivilegedRoles() {
        var exception = assertThrows(ResponseStatusException.class, () ->
            service.createInvitation(
                new InvitationRequest("admin@example.com", Role.SUPER_ADMIN),
                "hr@example.com",
                false
            )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(invitationRepository, never()).save(any(UserInvitation.class));
    }

    @Test
    void registrationUsesRoleFromValidInvitation() throws Exception {
        String rawInvitation = "valid-invitation-token";
        UserInvitation invitation = new UserInvitation(
            "member@example.com",
            Role.DEPARTMENT_MANAGER,
            sha256(rawInvitation),
            "admin@example.com",
            Instant.now().plusSeconds(3600)
        );
        when(invitationRepository.findByTokenHashForUpdate(sha256(rawInvitation)))
            .thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmailIgnoreCase("member@example.com"))
            .thenReturn(false);
        when(passwordEncoder.encode("password-123"))
            .thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        AuthSession session = service.register(new RegisterRequest(
            "New Member",
            "Member@example.com",
            "password-123",
            rawInvitation
        ));

        assertEquals(Role.DEPARTMENT_MANAGER, session.user().getRole());
        assertEquals("member@example.com", session.user().getEmail());
        assertEquals("access-token", session.accessToken());
        assertEquals(900L, session.accessTokenExpiresInSeconds());
        assertNotNull(session.refreshToken());
        assertNotNull(invitation.getAcceptedAt());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void registrationRejectsExpiredOrUnknownInvitation() throws Exception {
        String rawInvitation = "invalid-invitation-token";
        UserInvitation invitation = new UserInvitation(
            "member@example.com",
            Role.EMPLOYEE,
            sha256(rawInvitation),
            "admin@example.com",
            Instant.now().minusSeconds(1)
        );
        when(invitationRepository.findByTokenHashForUpdate(sha256(rawInvitation)))
            .thenReturn(Optional.of(invitation));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.register(new RegisterRequest(
                "New Member",
                "member@example.com",
                "password-123",
                rawInvitation
            ))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refreshRevokesOldTokenAndIssuesReplacement() throws Exception {
        String oldTokenValue = "current-refresh-token";
        User user = new User();
        user.setFullName("Existing User");
        user.setEmail("member@example.com");
        user.setPasswordHash("bcrypt-hash");
        user.setRole(Role.EMPLOYEE);
        user.setActive(true);
        RefreshToken oldToken = new RefreshToken(
            user,
            sha256(oldTokenValue),
            Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(oldTokenValue)))
            .thenReturn(Optional.of(oldToken));
        when(jwtService.generateToken(any())).thenReturn("replacement-access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        AuthSession session = service.refresh(oldTokenValue);

        assertNotNull(oldToken.getRevokedAt());
        assertEquals("replacement-access-token", session.accessToken());
        assertNotNull(session.refreshToken());
        verify(refreshTokenRepository, org.mockito.Mockito.times(2))
            .save(any(RefreshToken.class));
        verify(auditService).record(
            "TOKEN_REFRESHED", "member@example.com", "member@example.com"
        );
    }

    @Test
    void refreshRejectsRevokedToken() throws Exception {
        String rawToken = "revoked-refresh-token";
        User user = new User();
        user.setActive(true);
        RefreshToken oldToken = new RefreshToken(
            user, sha256(rawToken), Instant.now().plusSeconds(3600)
        );
        oldToken.revoke(Instant.now());
        when(refreshTokenRepository.findByTokenHashForUpdate(sha256(rawToken)))
            .thenReturn(Optional.of(oldToken));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.refresh(rawToken)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void passwordResetRequestStoresOnlyHashAndEmailsOneTimeToken() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("member@example.com");
        user.setActive(true);
        when(passwordResetRateLimiter.allowRequest(
            eq("member@example.com"), any(Instant.class)
        )).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("member@example.com"))
            .thenReturn(Optional.of(user));

        service.requestPasswordReset("Member@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
            ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(PasswordResetEmailRequested.class, eventCaptor.getValue().getClass());
        PasswordResetEmailRequested event =
            (PasswordResetEmailRequested) eventCaptor.getValue();
        assertEquals("member@example.com", event.recipient());
        assertTrue(event.resetUrl().contains("#token="));
        assertEquals(30, event.expiresInMinutes());
        assertEquals(64, tokenCaptor.getValue().getTokenHash().length());
        assertNotNull(tokenCaptor.getValue().getExpiresAt());
        verify(passwordResetTokenRepository)
            .consumeUnusedForUser(org.mockito.ArgumentMatchers.eq(user.getId()), any(Instant.class));
    }

    @Test
    void passwordResetRequestDoesNotRevealUnknownAccountsOrSendEmail() {
        when(passwordResetRateLimiter.allowRequest(
            eq("unknown@example.com"), any(Instant.class)
        )).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("unknown@example.com"))
            .thenReturn(Optional.empty());

        service.requestPasswordReset("unknown@example.com");

        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(auditService).record(
            "PASSWORD_RESET_REQUESTED", null, "unknown@example.com"
        );
    }

    @Test
    void passwordResetRequestIsThrottledByEmail() {
        when(passwordResetRateLimiter.allowRequest(
            eq("member@example.com"), any(Instant.class)
        )).thenReturn(false);

        service.requestPasswordReset("member@example.com");

        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(auditService).record(
            "PASSWORD_RESET_THROTTLED", null, "member@example.com"
        );
    }

    @Test
    void completingPasswordResetChangesPasswordAndRevokesSessions() throws Exception {
        String rawToken = "one-time-reset-token";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("member@example.com");
        user.setActive(true);
        PasswordResetToken resetToken = new PasswordResetToken(
            user, sha256(rawToken), Instant.now().plusSeconds(1800)
        );
        when(passwordResetTokenRepository.findByTokenHashForUpdate(sha256(rawToken)))
            .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("new-password"))
            .thenReturn("new-bcrypt-hash");

        service.completePasswordReset(rawToken, "new-password");

        assertNotNull(resetToken.getConsumedAt());
        assertEquals("new-bcrypt-hash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(refreshTokenRepository)
            .revokeAllActiveForUser(org.mockito.ArgumentMatchers.eq(user.getId()), any(Instant.class));
        verify(passwordResetTokenRepository)
            .consumeUnusedForUser(org.mockito.ArgumentMatchers.eq(user.getId()), any(Instant.class));
        verify(auditService).record(
            "PASSWORD_RESET_COMPLETED", "member@example.com", "member@example.com"
        );

        assertThrows(ResponseStatusException.class, () ->
            service.completePasswordReset(rawToken, "another-password")
        );
        verify(passwordEncoder).encode("new-password");
        verify(refreshTokenRepository, org.mockito.Mockito.times(1))
            .revokeAllActiveForUser(
                org.mockito.ArgumentMatchers.eq(user.getId()), any(Instant.class)
            );
    }

    @Test
    void completingPasswordResetRejectsExpiredToken() throws Exception {
        String rawToken = "expired-reset-token";
        User user = new User();
        user.setActive(true);
        PasswordResetToken resetToken = new PasswordResetToken(
            user, sha256(rawToken), Instant.now().minusSeconds(1)
        );
        when(passwordResetTokenRepository.findByTokenHashForUpdate(sha256(rawToken)))
            .thenReturn(Optional.of(resetToken));

        var exception = assertThrows(ResponseStatusException.class, () ->
            service.completePasswordReset(rawToken, "new-password")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository, never())
            .revokeAllActiveForUser(any(UUID.class), any(Instant.class));
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
