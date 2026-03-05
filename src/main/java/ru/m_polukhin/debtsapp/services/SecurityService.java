package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.UserData;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.utils.TokenUtils;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private final DaoAuthenticationProvider authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtils tokenUtils;
    private final DebtsDAO dao;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${linkToken.lifetime}")
    private Duration linkTokenLifetime;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Вход через Telegram: POST /login с сессионным токеном → JWT
    public ResponseEntity<?> authenticateUser(String sessionToken) {
        try {
            var session = dao.getActiveSession(sessionToken);
            if (session.expirationDate().before(new Date())) {
                return new ResponseEntity<>("Session expired", HttpStatus.BAD_REQUEST);
            }
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(session.userId(), sessionToken));
            return ResponseEntity.ok(tokenUtils.generateJwtToken(String.valueOf(session.userId())));
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Вход через веб (логин + пароль) → JWT
    public ResponseEntity<?> loginWeb(String username, String password) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || userOpt.get().getPasswordHash() == null) {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        UserData user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(tokenUtils.generateJwtToken(String.valueOf(user.getId())));
    }

    // Регистрация через веб: создаёт пользователя с логином и паролем
    public ResponseEntity<?> registerWeb(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return new ResponseEntity<>("Username already taken", HttpStatus.CONFLICT);
        }
        var user = new UserData(null, null, null, username, passwordEncoder.encode(password));
        userRepository.save(user);
        return new ResponseEntity<>("Registered successfully", HttpStatus.CREATED);
    }

    // Активирует сессионный токен, отправленный через Telegram
    public void activateSessionToken(Long userId, String sessionToken) {
        var token = tokenUtils.generateSessionToken(userId, passwordEncoder.encode(sessionToken));
        dao.addActiveSession(token);
    }

    // Генерирует одноразовый сессионный токен (используется перед входом через Telegram)
    public String generateSessionToken() {
        String token = generateRandomToken(20);
        try {
            dao.getActiveSession(token);
            return generateSessionToken(); // повтор при коллизии (крайне редко)
        } catch (UserNotFoundException e) {
            return token;
        }
    }

    // Генерирует короткоживущий токен для привязки Telegram к веб-аккаунту
    public String generateLinkToken(Long userId) {
        String token = generateRandomToken(16);
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(linkTokenLifetime));
        jdbcTemplate.update(
                "INSERT INTO link_tokens (token, user_id, expires_at) VALUES (?, ?, ?) " +
                "ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, expires_at = EXCLUDED.expires_at",
                token, userId, expiresAt);
        return token;
    }

    // Привязывает Telegram-аккаунт к веб-пользователю по токену привязки
    public boolean linkTelegramAccount(Long telegramId, String telegramName, String linkToken) {
        var rows = jdbcTemplate.queryForList(
                "SELECT user_id FROM link_tokens WHERE token = ? AND expires_at > NOW()", linkToken);
        if (rows.isEmpty()) return false;

        Long userId = ((Number) rows.get(0).get("user_id")).longValue();
        jdbcTemplate.update(
                "UPDATE users SET telegram_id = ?, telegram_name = ? WHERE id = ?",
                telegramId, telegramName, userId);
        jdbcTemplate.update("DELETE FROM link_tokens WHERE token = ?", linkToken);
        return true;
    }

    private String generateRandomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
