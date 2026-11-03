package com.nexushr.auth.repository;

import com.nexushr.auth.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token join fetch token.user " +
           "where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(
        @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("update PasswordResetToken token set token.consumedAt = :now " +
           "where token.user.id = :userId and token.consumedAt is null")
    int consumeUnusedForUser(
        @Param("userId") UUID userId,
        @Param("now") Instant now
    );

    @Modifying
    @Query("delete from PasswordResetToken token " +
           "where token.expiresAt < :cutoff or token.consumedAt < :cutoff")
    int deleteExpiredOrConsumedBefore(@Param("cutoff") Instant cutoff);
}
