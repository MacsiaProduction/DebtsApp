package ru.m_polukhin.debtsapp.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TokenUtilsTest {

    @Autowired
    TokenUtils tokenUtils;
    @Test
    public void testGenerateJwtToken() {
        String expected = "testSubject";
        String token = tokenUtils.generateJwtToken(expected);
        String actual = tokenUtils.getSubject(token);
        Assertions.assertEquals(expected, actual);
    }
}