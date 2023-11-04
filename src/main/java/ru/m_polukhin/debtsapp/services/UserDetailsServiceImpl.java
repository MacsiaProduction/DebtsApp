package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.ActiveSessionToken;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final DebtsDAO debtsDAO;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        ActiveSessionToken sessionToken;
        try {
            sessionToken = debtsDAO.getUsersSession(Long.valueOf(userId));
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException("User " + userId + " session not Found");
        }
        return new  User(userId,
                        sessionToken.getHash(),
                        Collections.singleton(new SimpleGrantedAuthority("USER")));
    }
}