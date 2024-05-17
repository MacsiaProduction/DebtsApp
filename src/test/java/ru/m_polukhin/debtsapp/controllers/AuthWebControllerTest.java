package ru.m_polukhin.debtsapp.controllers;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.m_polukhin.debtsapp.services.SecurityService;

import static org.junit.jupiter.api.Assertions.assertEquals;

//todo disable scheduling in tests
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AuthWebControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SecurityService securityService;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    @Order(1)
    public void testGenerateSessionToken() {
        String url = "http://localhost:" + port + "/session";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testAuthenticateUserFail() {
        String url = "http://localhost:" + port + "/login";
        ResponseEntity<?> response = restTemplate.postForEntity(url, "sampleSessionToken", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testAuthenticateUserSuccess() {
        var userId = 100L;
        String sessionUrl = "http://localhost:" + port + "/session";
        ResponseEntity<String> sessionResponse = restTemplate.getForEntity(sessionUrl, String.class);
        assertEquals(HttpStatus.OK, sessionResponse.getStatusCode());

        securityService.activateSessionToken(userId, sessionResponse.getBody());

        String LoggingUrl = "http://localhost:" + port + "/login";
        ResponseEntity<?> LoggingResponse = restTemplate.postForEntity(LoggingUrl, sessionResponse.getBody(), String.class);
        assertEquals(HttpStatus.OK, LoggingResponse.getStatusCode());
    }
}