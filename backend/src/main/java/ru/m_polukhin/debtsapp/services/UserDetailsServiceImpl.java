package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;

import java.util.Collections;

/*
 * Поддерживает два режима аутентификации:
 * - числовой principal → вход через Telegram (пароль = хэш сессионного токена)
 * - нечисловой principal → вход через веб (логин/пароль)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final DebtsDAO debtsDAO;

    @Override
    public UserDetails loadUserByUsername(String principal) throws UsernameNotFoundException {
        try {
            Long userId = Long.parseLong(principal);
            var sessionToken = debtsDAO.getUsersSession(userId);
            return new User(principal, sessionToken.hash(),
                    Collections.singleton(new SimpleGrantedAuthority("USER")));
        } catch (NumberFormatException e) {
            var user = debtsDAO.findUserByUsername(principal)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal));
            if (user.getPasswordHash() == null) {
                throw new UsernameNotFoundException("No password set for: " + principal);
            }
            return new User(String.valueOf(user.getId()), user.getPasswordHash(),
                    Collections.singleton(new SimpleGrantedAuthority("USER")));
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException("Session not found: " + principal);
        }
    }
}
