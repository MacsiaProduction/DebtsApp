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

import java.time.Duration;
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
    void linkTelegramAccountUpdatesUser() {
        when(jdbcTemplate.queryForList(anyString(), eq("good")))
                .thenReturn(List.of(Map.of("user_id", 7L)));

        assertTrue(securityService.linkTelegramAccount(42L, "tg", "good"));

        verify(jdbcTemplate).update(
                eq("UPDATE users SET telegram_id = ?, telegram_name = ? WHERE id = ?"),
                eq(42L), eq("tg"), eq(7L));
    }
}
