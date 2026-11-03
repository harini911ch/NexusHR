package com.nexushr.auth.repository;

import com.nexushr.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, java.util.UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token join fetch token.user " +
           "where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(
        @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :now " +
           "where token.user.id = :userId and token.revokedAt is null")
    int revokeAllActiveForUser(
        @Param("userId") java.util.UUID userId,
        @Param("now") java.time.Instant now
    );
}
