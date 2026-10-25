package org.app.corporateinternetbanking.security.session;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    // Locked so two refreshes with the same token can't both rotate it
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s where s.refreshTokenHash = :hash")
    Optional<AuthSession> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    Optional<AuthSession> findByRefreshTokenHash(String hash);

    Optional<AuthSession> findByPreviousTokenHash(String hash);

    @Query("select count(s) > 0 from AuthSession s where s.id = :id and s.revokedAt is null and s.expiresAt > :now")
    boolean isActive(@Param("id") String id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update AuthSession s set s.revokedAt = :now where s.user = :user and s.revokedAt is null")
    int revokeAllForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from AuthSession s where s.expiresAt < :cutoff or s.revokedAt < :cutoff")
    int deleteEndedBefore(@Param("cutoff") LocalDateTime cutoff);
}
