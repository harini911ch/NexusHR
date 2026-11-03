package com.nexushr.auth.controller;

import com.nexushr.auth.dto.*;
import com.nexushr.auth.entity.User;
import com.nexushr.auth.repository.UserRepository;
import com.nexushr.auth.service.AuthService;
import com.nexushr.auth.service.AuthSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_COOKIE = "nexushr_access";
    private static final String REFRESH_COOKIE = "nexushr_refresh";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final boolean secureCookies;

    public AuthController(
        AuthService authService,
        UserRepository userRepository,
        @Value("${app.auth.cookie-secure:false}") boolean secureCookies
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse createInvitation(
        Authentication authentication,
        @Valid @RequestBody InvitationRequest request
    ) {
        boolean isSuperAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return authService.createInvitation(request, authentication.getName(), isSuperAdmin);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletResponse response
    ) {
        return authenticate(authService.register(request), response);
    }

    @PostMapping("/login")
    public AuthResponse login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        return authenticate(authService.login(request), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
        @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
        HttpServletResponse response
    ) {
        return authenticate(authService.refresh(refreshToken), response);
    }

    @PostMapping("/password-reset/request")
    public PasswordResetAcceptedResponse requestPasswordReset(
        @Valid @RequestBody PasswordResetRequest request
    ) {
        authService.requestPasswordReset(request.email());
        return new PasswordResetAcceptedResponse(
            "If the account exists, password reset instructions will be sent."
        );
    }

    @PostMapping("/password-reset/complete")
    public PasswordResetAcceptedResponse completePasswordReset(
        @Valid @RequestBody PasswordResetCompletionRequest request,
        HttpServletResponse response
    ) {
        authService.completePasswordReset(request.token(), request.newPassword());
        clearCookie(response, ACCESS_COOKIE);
        clearCookie(response, REFRESH_COOKIE);
        return new PasswordResetAcceptedResponse(
            "Password reset completed. Sign in with your new password."
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
        HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        clearCookie(response, ACCESS_COOKIE);
        clearCookie(response, REFRESH_COOKIE);
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"
            ));

        return new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name()
        );
    }

    private AuthResponse authenticate(AuthSession session, HttpServletResponse response) {
        addCookie(response, ACCESS_COOKIE, session.accessToken(),
            Duration.ofSeconds(session.accessTokenExpiresInSeconds()));
        addCookie(response, REFRESH_COOKIE, session.refreshToken(),
            Duration.ofSeconds(session.refreshTokenExpiresInSeconds()));
        User user = session.user();
        return new AuthResponse(user.getId(), user.getFullName(),
            user.getEmail(), user.getRole().name());
    }

    private void addCookie(
        HttpServletResponse response, String name, String value, Duration maxAge
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Strict")
            .path("/api")
            .maxAge(maxAge)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Strict")
            .path("/api")
            .maxAge(Duration.ZERO)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
