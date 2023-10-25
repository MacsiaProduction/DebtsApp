package ru.m_polukhin.debtsapp.services;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.m_polukhin.debtsapp.exceptions.PasswordsNotMatch;
import ru.m_polukhin.debtsapp.exceptions.UserNotFoundException;
import ru.m_polukhin.debtsapp.models.ChangePasswordDto;
import ru.m_polukhin.debtsapp.models.LogInDTO;

import static org.passay.IllegalCharacterRule.ERROR_CODE;

@Service
public class SecurityService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final DebtsDAO dao;

    @Autowired
    public SecurityService(AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, DebtsDAO dao) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.dao = dao;
    }

    public void authenticateUser(LogInDTO loginDto) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginDto.getName(), loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void updatePassword(ChangePasswordDto dto) throws UserNotFoundException, PasswordsNotMatch {
        var user = dao.findUserByName(dto.getUsername());

        if (user.getPasswordHash() == null) throw new PasswordsNotMatch();

        if (passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            dao.changeUserPassword(user.getId(), passwordEncoder.encode(dto.getNewPassword()));
        } else {
            throw new PasswordsNotMatch();
        }
    }

    /**
     * @return generated password
     */
    public String generatePassword(String username) throws UserNotFoundException {
        var password = generatePassayPassword();
        dao.changeUserPassword(dao.findUserByName(username).getId(), passwordEncoder.encode(password));
        return password;
    }

    private String generatePassayPassword() {
        PasswordGenerator gen = new PasswordGenerator();
        CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
        CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
        lowerCaseRule.setNumberOfCharacters(2);

        CharacterData upperCaseChars = EnglishCharacterData.UpperCase;
        CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
        upperCaseRule.setNumberOfCharacters(2);

        CharacterData digitChars = EnglishCharacterData.Digit;
        CharacterRule digitRule = new CharacterRule(digitChars);
        digitRule.setNumberOfCharacters(2);

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

        return gen.generatePassword(10, splCharRule, lowerCaseRule,
                upperCaseRule, digitRule);
    }
}
