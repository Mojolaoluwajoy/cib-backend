package org.app.corporateinternetbanking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private String extractClaim(String token, String keyName) {

        return extractAllClaims(token).get(keyName).toString();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return isTokenExpired(extractAllClaims(token));
    }

    private SecretKey getSecretKey() {
        byte[] encodedKey = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(encodedKey);
    }

    // Purpose tokens (invitation / password reset) carry a "type" claim and must never work as login tokens.
    public boolean isTokenValid(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("type") == null && !isTokenExpired(claims);
    }

    // Login tokens belong to a session (see SessionService); JwtAuthFilter rejects them once it has ended.
    public String generateToken(User user, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SESSION_ID, sessionId);
        claims.put("id", user.getId());
        claims.put("username", user.getEmail());
        claims.put("role", user.getRole());
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSecretKey())
                .compact();
    }

    public String extractSessionId(String token) {
        return extractAllClaims(token).get(SESSION_ID, String.class);
    }

    public long accessTokenSeconds() {
        return jwtExpiration / 1000;
    }

    public String generateInvitationToken(String email) {
        return generatePurposeToken(email, INVITATION, null);
    }

    // The token is bound to the user's current password hash, so it stops working once the
    // password has been changed (i.e. it is single-use) without needing a token table.
    public String generatePasswordResetToken(User user) {
        return generatePurposeToken(user.getEmail(), PASSWORD_RESET, passwordFingerprint(user.getPassword()));
    }

    public boolean isInvitationTokenValid(String token, String email) {
        Claims claims = parsePurposeToken(token, INVITATION, email);
        return claims != null;
    }

    public boolean isPasswordResetTokenValid(String token, User user) {
        Claims claims = parsePurposeToken(token, PASSWORD_RESET, user.getEmail());
        return claims != null && passwordFingerprint(user.getPassword()).equals(claims.get("pwd"));
    }

    private static final String SESSION_ID = "sid";
    private static final String INVITATION = "INVITATION";
    private static final String PASSWORD_RESET = "PASSWORD_RESET";

    private String generatePurposeToken(String email, String type, String passwordFingerprint) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        if (passwordFingerprint != null) {
            claims.put("pwd", passwordFingerprint);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (3600000)))
                .signWith(getSecretKey())
                .compact();
    }

    // Returns the claims only if the token is well-formed, unexpired, of the expected type and issued for this email.
    private Claims parsePurposeToken(String token, String type, String email) {
        if (token == null || email == null) {
            return null;
        }
        Claims claims;
        try {
            claims = extractAllClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
        if (!type.equals(claims.get("type"))
                || !email.equalsIgnoreCase(claims.getSubject())
                || isTokenExpired(claims)) {
            return null;
        }
        return claims;
    }

    private String passwordFingerprint(String passwordHash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(passwordHash).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
