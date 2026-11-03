package com.nexushr.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_invitations")
public class UserInvitation {

    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "invited_by", nullable = false, length = 180)
    private String invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserInvitation() {}

    public UserInvitation(String email, Role role, String tokenHash,
                          String invitedBy, Instant expiresAt) {
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isUsableAt(Instant now) {
        return acceptedAt == null && expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        if (!isUsableAt(now)) {
            throw new IllegalStateException("Invitation is no longer valid");
        }
        acceptedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getTokenHash() { return tokenHash; }
    public String getInvitedBy() { return invitedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
