package org.app.corporateinternetbanking.security.session;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// The refresh token travels in an HttpOnly cookie, so scripts running in the browser can never read it.
// It is only sent to /auth, where it is used to refresh or end the session.
@Component
public class RefreshTokenCookie {

    public static final String NAME = "cib_refresh";
    private static final String PATH = "/auth";

    @Value("${application.security.refresh-token.days}")
    private long refreshTokenDays;

    @Value("${application.security.refresh-cookie.secure}")
    private boolean secure;

    @Value("${application.security.refresh-cookie.same-site}")
    private String sameSite;

    public String issue(String refreshToken) {
        return build(refreshToken, Duration.ofDays(refreshTokenDays));
    }

    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
