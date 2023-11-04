package ru.m_polukhin.debtsapp.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class TokenUtils {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.lifetime}")
    private Duration jwtLifetime;

    @Value("${sessionToken.lifetime}")
    private Duration sessionLifetime;

    public String generateJwtToken(String subject) {
        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtLifetime.toMillis());
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(issuedDate)
                .setExpiration(expiredDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public String getSubject(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    public ActiveSessionToken generateSessionToken(Long userId, String hashedToken) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant newInstant = currentTimestamp.toInstant().plus(sessionLifetime);
        Timestamp expirationDate = Timestamp.from(newInstant);
        return new ActiveSessionToken(userId, hashedToken, expirationDate);
    }

}