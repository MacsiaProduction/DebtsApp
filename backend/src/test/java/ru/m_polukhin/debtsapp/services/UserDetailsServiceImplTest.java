package ru.m_polukhin.debtsapp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;
import ru.m_polukhin.debtsapp.models.UserData;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private DebtsDAO debtsDAO;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsernameUsesTelegramSessionForNumericPrincipal() throws UserNotFoundException {
        var session = new ActiveSessionToken(5L, "session-hash",
                Timestamp.from(Instant.now().plusSeconds(60)));
        when(debtsDAO.getUsersSession(5L)).thenReturn(session);

        var userDetails = userDetailsService.loadUserByUsername("5");

        assertThat(userDetails.getUsername()).isEqualTo("5");
        assertThat(userDetails.getPassword()).isEqualTo("session-hash");
    }

    @Test
    void loadUserByUsernameUsesWebCredentialsForLogin() {
        var user = new UserData(9L, null, null, "alice", "password-hash");
        when(debtsDAO.findUserByUsername("alice")).thenReturn(Optional.of(user));

        var userDetails = userDetailsService.loadUserByUsername("alice");

        assertThat(userDetails.getUsername()).isEqualTo("9");
        assertThat(userDetails.getPassword()).isEqualTo("password-hash");
    }

    @Test
    void loadUserByUsernameRejectsMissingWebPassword() {
        var user = new UserData(9L, null, null, "alice", null);
        when(debtsDAO.findUserByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("alice"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("No password set");
    }

    @Test
    void loadUserByUsernameRejectsUnknownWebUser() {
        when(debtsDAO.findUserByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void loadUserByUsernameRejectsMissingTelegramSession() throws UserNotFoundException {
        when(debtsDAO.getUsersSession(5L)).thenThrow(new UserNotFoundException(5L));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("5"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Session not found");
    }
}
