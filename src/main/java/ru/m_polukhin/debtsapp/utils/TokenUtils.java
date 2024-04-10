package ru.m_polukhin.debtsapp.utils;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
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

    public TokenUtils() {
        this.secretKey = generateKey();
    }
    private SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256"); // Use HmacSHA256
            keyGenerator.init(256); // Initialize with a key size of 256 bits
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No such algorithm", e);
        }
    }

    public String generateJwtToken(String subject) {
        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtLifetime.toMillis());
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issuedDate)
                .expiration(expiredDate)
                .signWith(secretKey) // Update the signWith method
                .compact();
    }

    public String getSubject(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .decryptWith(secretKey)
                .build().parseSignedClaims(token).getPayload().getSubject();
    }

    public ActiveSessionToken generateSessionToken(Long userId, String hashedToken) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant newInstant = currentTimestamp.toInstant().plus(sessionLifetime);
        Timestamp expirationDate = Timestamp.from(newInstant);
        return new ActiveSessionToken(userId, hashedToken, expirationDate);
    }

}