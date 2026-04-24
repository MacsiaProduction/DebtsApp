package ru.m_polukhin.debtsapp.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class TokenUtils {

    @Value("${jwt.lifetime}")
    private Duration jwtLifetime;

    @Value("${sessionToken.lifetime}")
    private Duration sessionLifetime;

    private final SecretKey secretKey;

    public TokenUtils(
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${app.jwt.allow-generated-secret:true}") boolean allowGeneratedSecret) {
        this.secretKey = generateKey(jwtSecret, allowGeneratedSecret);
    }

    private SecretKey generateKey(String jwtSecret, boolean allowGeneratedSecret) {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            try {
                byte[] secretBytes = MessageDigest.getInstance("SHA-256")
                        .digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
                return Keys.hmacShaKeyFor(secretBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("No such algorithm", e);
            }
        }

        if (!allowGeneratedSecret) {
            throw new IllegalStateException("JWT_SECRET must be configured when app.jwt.allow-generated-secret=false");
        }

        return Jwts.SIG.HS256.key().build();
    }

    public String generateJwtToken(String subject) {
        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtLifetime.toMillis());
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issuedDate)
                .expiration(expiredDate)
                .signWith(secretKey)
                .compact();
    }

    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build().parseSignedClaims(token).getPayload().getSubject();
    }

    public ActiveSessionToken generateSessionToken(Long userId, String hashedToken) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant newInstant = currentTimestamp.toInstant().plus(sessionLifetime);
        Timestamp expirationDate = Timestamp.from(newInstant);
        return new ActiveSessionToken(userId, hashedToken, expirationDate);
    }

}
