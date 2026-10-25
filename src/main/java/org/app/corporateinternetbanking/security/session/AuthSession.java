package org.app.corporateinternetbanking.security.session;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.app.corporateinternetbanking.user.domain.entity.User;

// One login on one device. Access tokens carry this session's id ("sid"), so revoking the session (logout,
// password change, a stolen refresh token being replayed) cuts off its access tokens immediately.
// Refresh tokens are stored only as SHA-256 hashes and rotate on every use.
@Entity
@Setter
@Getter
@Table(
        name = "auth_session",
        indexes = @Index(name = "idx_auth_session_previous_token_hash", columnList = "previous_token_hash"))
public class AuthSession {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    // The token this one replaced; presenting it again means a refresh token was copied
    @Column(name = "previous_token_hash", length = 64)
    private String previousTokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime lastRefreshedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
