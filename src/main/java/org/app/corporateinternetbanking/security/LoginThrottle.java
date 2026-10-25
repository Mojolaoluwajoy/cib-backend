package org.app.corporateinternetbanking.security;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Slows down password guessing. An email is locked after max-failures-per-email failed logins, and an IP address
// after max-failures-per-ip failures across any emails, each for lockout-minutes. Unknown emails are counted the
// same way, so lockouts don't reveal which accounts exist. State is in memory: per app instance, reset on restart.
@Component
public class LoginThrottle {

    private static final int MAX_TRACKED = 10_000;

    private final int maxFailuresPerEmail;
    private final int maxFailuresPerIp;
    private final Duration lockout;
    private final Map<String, Attempts> byEmail = new HashMap<>();
    private final Map<String, Attempts> byIp = new HashMap<>();

    public LoginThrottle(
            @Value("${security.login.max-failures-per-email}") int maxFailuresPerEmail,
            @Value("${security.login.max-failures-per-ip}") int maxFailuresPerIp,
            @Value("${security.login.lockout-minutes}") long lockoutMinutes) {
        this.maxFailuresPerEmail = maxFailuresPerEmail;
        this.maxFailuresPerIp = maxFailuresPerIp;
        this.lockout = Duration.ofMinutes(lockoutMinutes);
    }

    // How long the caller still has to wait, if either the email or the IP address is locked
    public synchronized Optional<Duration> blockedFor(String email, String ip) {
        Instant now = Instant.now();
        Duration wait = Duration.ZERO;
        for (Attempts attempts : new Attempts[] {byEmail.get(key(email)), byIp.get(ip)}) {
            if (attempts != null && attempts.lockedUntil != null && now.isBefore(attempts.lockedUntil)) {
                Duration remaining = Duration.between(now, attempts.lockedUntil);
                wait = remaining.compareTo(wait) > 0 ? remaining : wait;
            }
        }
        return wait.isZero() ? Optional.empty() : Optional.of(wait);
    }

    public synchronized void recordFailure(String email, String ip) {
        Instant now = Instant.now();
        if (byEmail.size() + byIp.size() > MAX_TRACKED) {
            byEmail.values().removeIf(attempts -> attempts.isStale(now, lockout));
            byIp.values().removeIf(attempts -> attempts.isStale(now, lockout));
        }
        fail(byEmail, key(email), maxFailuresPerEmail, now);
        fail(byIp, ip, maxFailuresPerIp, now);
    }

    // A correct password clears the email's failures; the IP's count stays so one valid login can't reset a spray
    public synchronized void recordSuccess(String email) {
        byEmail.remove(key(email));
    }

    public synchronized void clear() {
        byEmail.clear();
        byIp.clear();
    }

    private void fail(Map<String, Attempts> attemptsByKey, String key, int maxFailures, Instant now) {
        Attempts attempts = attemptsByKey.get(key);
        if (attempts == null || attempts.isStale(now, lockout)) {
            attempts = new Attempts(now);
            attemptsByKey.put(key, attempts);
        }
        attempts.failures++;
        if (attempts.failures >= maxFailures) {
            attempts.lockedUntil = now.plus(lockout);
            attempts.failures = 0;
            attempts.windowStart = now;
        }
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    // Failures count within a window of one lockout period from the first failure
    private static final class Attempts {
        private Instant windowStart;
        private int failures;
        private Instant lockedUntil;

        private Attempts(Instant windowStart) {
            this.windowStart = windowStart;
        }

        private boolean isStale(Instant now, Duration lockout) {
            boolean locked = lockedUntil != null && now.isBefore(lockedUntil);
            return !locked && now.isAfter(windowStart.plus(lockout));
        }
    }
}
