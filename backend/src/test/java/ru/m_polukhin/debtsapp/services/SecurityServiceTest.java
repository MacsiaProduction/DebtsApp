package ru.m_polukhin.debtsapp.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.UserData;
import ru.m_polukhin.debtsapp.repository.UserRepository;
import ru.m_polukhin.debtsapp.utils.TokenUtils;

import org.springframework.security.core.Authentication;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private DaoAuthenticationProvider authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenUtils tokenUtils;
    @Mock
    private DebtsDAO dao;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityService, "linkTokenLifetime", Duration.ofMinutes(5));
    }

    @Test
    void loginWebReturnsUnauthorizedForUnknownUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        var response = securityService.loginWeb("alice", "secret");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void loginWebRejectsUserWithoutPassword() {
        var user = new UserData(1L, null, null, "alice", null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        var response = securityService.loginWeb("alice", "secret");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void loginWebReturnsUnauthorizedForWrongPassword() {
        var user = new UserData(1L, null, null, "alice", "hash");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        var response = securityService.loginWeb("alice", "wrong");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void loginWebReturnsJwtWhenCredentialsMatch() {
        var user = new UserData(1L, null, null, "alice", "hash");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(tokenUtils.generateJwtToken("1")).thenReturn("jwt-1");

        var response = securityService.loginWeb("alice", "secret");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-1", response.getBody());
    }

    @Test
    void registerWebRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        var response = securityService.registerWeb("alice", "secret");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void registerWebCreatesUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        var response = securityService.registerWeb("alice", "secret");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userRepository).save(any(UserData.class));
    }

    @Test
    void generateSessionTokenReturnsUniqueValue() throws UserNotFoundException {
        when(dao.getActiveSession(anyString())).thenThrow(new UserNotFoundException("missing"));

        String token = securityService.generateSessionToken();

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void linkTelegramAccountReturnsFalseWhenTokenMissing() {
        when(jdbcTemplate.queryForList(anyString(), eq("bad"))).thenReturn(List.of());

        assertFalse(securityService.linkTelegramAccount(42L, "tg", "bad"));
    }

    @Test
    void authenticateUserReturnsBadRequestForExpiredSession() throws UserNotFoundException {
        var expired = new ActiveSessionToken(1L, "hash",
                Timestamp.from(Instant.now().minusSeconds(60)));
        when(dao.getActiveSession("token")).thenReturn(expired);

        var response = securityService.authenticateUser("token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Session expired", response.getBody());
    }

    @Test
    void authenticateUserReturnsJwtForValidSession() throws UserNotFoundException {
        var session = new ActiveSessionToken(1L, "hash",
                Timestamp.from(Instant.now().plusSeconds(60)));
        when(dao.getActiveSession("token")).thenReturn(session);
        when(tokenUtils.generateJwtToken("1")).thenReturn("jwt-1");
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        var response = securityService.authenticateUser("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-1", response.getBody());
    }

    @Test
    void authenticateUserReturnsBadRequestWhenSessionMissing() throws UserNotFoundException {
        when(dao.getActiveSession("bad")).thenThrow(new UserNotFoundException("bad"));

        var response = securityService.authenticateUser("bad");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void activateSessionTokenStoresHashedSession() {
        var session = new ActiveSessionToken(7L, "encoded",
                Timestamp.from(Instant.now().plusSeconds(60)));
        when(passwordEncoder.encode("raw-token")).thenReturn("encoded");
        when(tokenUtils.generateSessionToken(7L, "encoded")).thenReturn(session);

        securityService.activateSessionToken(7L, "raw-token");

        verify(tokenUtils).generateSessionToken(7L, "encoded");
        verify(dao).addActiveSession(session);
    }

    @Test
    void generateLinkTokenPersistsToken() {
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenReturn(1);

        String token = securityService.generateLinkToken(42L);

        assertNotNull(token);
        assertFalse(token.isBlank());
        verify(jdbcTemplate).update(anyString(), anyString(), eq(42L), any(Timestamp.class));
    }

    @Test
    void linkTelegramAccountUpdatesUser() {
        when(jdbcTemplate.queryForList(anyString(), eq("good")))
                .thenReturn(List.of(Map.of("user_id", 7L)));

        assertTrue(securityService.linkTelegramAccount(42L, "tg", "good"));

        verify(jdbcTemplate).update(
                eq("UPDATE users SET telegram_id = ?, telegram_name = ? WHERE id = ?"),
                eq(42L), eq("tg"), eq(7L));
    }
}
