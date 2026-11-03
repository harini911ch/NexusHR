package com.nexushr.auth.repository;

import com.nexushr.auth.entity.UserInvitation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from UserInvitation invitation " +
           "where invitation.tokenHash = :tokenHash")
    Optional<UserInvitation> findByTokenHashForUpdate(
        @Param("tokenHash") String tokenHash
    );

    boolean existsByEmailIgnoreCaseAndAcceptedAtIsNullAndExpiresAtAfter(
        String email, Instant now
    );
}
