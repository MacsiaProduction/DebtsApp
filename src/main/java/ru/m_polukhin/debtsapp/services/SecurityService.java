package ru.m_polukhin.debtsapp.services;

import lombok.RequiredArgsConstructor;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.utils.TokenUtils;

import java.util.Date;

import static org.passay.IllegalCharacterRule.ERROR_CODE;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private final DaoAuthenticationProvider authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtils tokenUtils;
    private final DebtsDAO dao;

    public ResponseEntity<?> authenticateUser(String sessionToken) {
        try {
            var session = dao.getActiveSession(sessionToken);
            //check lifetime
            if (session.expirationDate().before(new Date())) {
                throw new UserNotFoundException("Session Expired");
            }

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    session.userId(), sessionToken));
            var jwtToken =  tokenUtils.generateJwtToken(String.valueOf(session.userId()));
            return ResponseEntity.ok(jwtToken);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    public void activateSessionToken(Long userId, String sessionToken) {
        var token = tokenUtils.generateSessionToken(userId, passwordEncoder.encode(sessionToken));
        dao.addActiveSession(token);
    }

    //smth may fall if two same tokens generated
    public String generateSessionToken() {
        String token = generatePassayPassword();
        try {
            dao.getActiveSession(token);
            return generateSessionToken();
        } catch (UserNotFoundException e) {
            return token;
        }
    }

    private String generatePassayPassword() {
        PasswordGenerator gen = new PasswordGenerator();
        CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
        CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
        lowerCaseRule.setNumberOfCharacters(5);

        CharacterData upperCaseChars = EnglishCharacterData.UpperCase;
        CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
        upperCaseRule.setNumberOfCharacters(5);

        CharacterData digitChars = EnglishCharacterData.Digit;
        CharacterRule digitRule = new CharacterRule(digitChars);
        digitRule.setNumberOfCharacters(5);

        CharacterData specialChars = new CharacterData() {
            public String getErrorCode() {
                return ERROR_CODE;
            }

            public String getCharacters() {
                return "!@#$%^&*()_+";
            }
        };
        CharacterRule splCharRule = new CharacterRule(specialChars);
        splCharRule.setNumberOfCharacters(2);

        return gen.generatePassword(25, splCharRule, lowerCaseRule,
                upperCaseRule, digitRule);
    }
}
