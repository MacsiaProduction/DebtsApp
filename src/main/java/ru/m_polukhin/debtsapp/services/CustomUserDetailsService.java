package ru.m_polukhin.debtsapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.UserData;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private DebtsDAO debtsDAO;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserData user;
        try {
            user = debtsDAO.findUserByName(username);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException("User Not Found");
        }
        return new User(user.getTelegramName(),
                user.getPasswordHash(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRole())));
    }
}