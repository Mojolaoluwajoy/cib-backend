package org.app.corporateinternetbanking.user.exceptions;

import java.time.Duration;
import lombok.Getter;

@Getter
public class TooManyLoginAttempts extends Exception {
    private final Duration retryAfter;

    public TooManyLoginAttempts(Duration retryAfter) {
        super("Too many failed login attempts. Try again in " + Math.max(1, (retryAfter.toSeconds() + 59) / 60)
                + " minute(s)");
        this.retryAfter = retryAfter;
    }
}
