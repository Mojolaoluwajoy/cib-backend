package org.app.corporateinternetbanking.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.corporateinternetbanking.security.JwtService;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.app.corporateinternetbanking.user.dto.authDto.LoginResponse;
import org.app.corporateinternetbanking.user.enums.UserStatus;
import org.app.corporateinternetbanking.user.exceptions.SessionExpired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Issues, rotates and revokes login sessions. The access token (JWT) lives for minutes; the refresh token lives for
// days and is exchanged for a new pair on every use. Replaying an already-used refresh token ends the whole session,
// because only someone who copied it would still have the old one.
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    // Two tabs refreshing at the same moment both present the same token; don't treat the loser as theft
    static final Duration REUSE_GRACE = Duration.ofSeconds(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthSessionRepository repository;
    private final JwtService jwtService;

    @Value("${application.security.refresh-token.days}")
    private long refreshTokenDays;

    @Transactional
    public LoginResponse start(User user) {
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenDays));
        String refreshToken = newRefreshToken();
        session.setRefreshTokenHash(hash(refreshToken));
        repository.save(session);
        return response(user, session, refreshToken);
    }

    // Checked exceptions don't roll back, so revoking a session on token reuse is kept even though the call fails
    @Transactional
    public LoginResponse refresh(String refreshToken) throws SessionExpired {
        String hash = hash(refreshToken);
        LocalDateTime now = LocalDateTime.now();
        Optional<AuthSession> found = repository.findByRefreshTokenHashForUpdate(hash);
        if (found.isEmpty()) {
            repository.findByPreviousTokenHash(hash).ifPresent(session -> revokeIfReplayed(session, now));
            throw invalidRefreshToken();
        }

        AuthSession session = found.get();
        User user = session.getUser();
        if (!session.isActive(now) || user.getStatus() != UserStatus.ACTIVE) {
            throw invalidRefreshToken();
        }
        String rotated = newRefreshToken();
        session.setPreviousTokenHash(hash);
        session.setRefreshTokenHash(hash(rotated));
        session.setLastRefreshedAt(now);
        return response(user, session, rotated);
    }

    // Idempotent: logging out with an unknown or already revoked token is not an error
    @Transactional
    public void logout(String refreshToken) {
        repository.findByRefreshTokenHash(hash(refreshToken)).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(LocalDateTime.now());
            }
        });
    }

    // After a password change every device has to log in again
    @Transactional
    public void endAllSessions(User user) {
        int ended = repository.revokeAllForUser(user, LocalDateTime.now());
        log.info("Ended {} session(s) for user {}", ended, user.getId());
    }

    public boolean isActive(String sessionId) {
        return sessionId != null && repository.isActive(sessionId, LocalDateTime.now());
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeEndedSessions() {
        int purged = repository.deleteEndedBefore(LocalDateTime.now().minusDays(1));
        log.info("Purged {} ended session(s)", purged);
    }

    private void revokeIfReplayed(AuthSession session, LocalDateTime now) {
        boolean concurrentRefresh = session.getLastRefreshedAt() != null
                && session.getLastRefreshedAt().isAfter(now.minus(REUSE_GRACE));
        if (!concurrentRefresh && session.getRevokedAt() == null) {
            log.warn("Refresh token reused for session {}; revoking it", session.getId());
            session.setRevokedAt(now);
        }
    }

    private LoginResponse response(User user, AuthSession session, String refreshToken) {
        return new LoginResponse(
                user, jwtService.generateToken(user, session.getId()), refreshToken, jwtService.accessTokenSeconds());
    }

    private static SessionExpired invalidRefreshToken() {
        return new SessionExpired("Refresh token is invalid or expired. Please log in again");
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
