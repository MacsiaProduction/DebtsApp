package ru.m_polukhin.debtsapp.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenUtilsTest {

    @Test
    void testGenerateJwtTokenWithConfiguredSecret() {
        TokenUtils tokenUtils = new TokenUtils("test-secret", false);
        configureLifetimes(tokenUtils);

        String token = tokenUtils.generateJwtToken("testSubject");

        assertThat(tokenUtils.getSubject(token)).isEqualTo("testSubject");
    }

    @Test
    void testGenerateJwtTokenWithGeneratedSecretInLocalMode() {
        TokenUtils tokenUtils = new TokenUtils("", true);
        configureLifetimes(tokenUtils);

        String token = tokenUtils.generateJwtToken("generated");

        assertThat(tokenUtils.getSubject(token)).isEqualTo("generated");
    }

    @Test
    void testMissingJwtSecretFailsWhenGeneratedSecretsDisabled() {
        assertThatThrownBy(() -> new TokenUtils("", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    private void configureLifetimes(TokenUtils tokenUtils) {
        ReflectionTestUtils.setField(tokenUtils, "jwtLifetime", Duration.ofMinutes(10));
        ReflectionTestUtils.setField(tokenUtils, "sessionLifetime", Duration.ofMinutes(1));
    }
}
