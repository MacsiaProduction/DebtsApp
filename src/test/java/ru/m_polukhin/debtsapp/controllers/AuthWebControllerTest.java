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
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.m_polukhin.debtsapp.dto.RegisterDTO;
import ru.m_polukhin.debtsapp.services.SecurityService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AuthWebControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community")
            .withoutAuthentication();

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private SecurityService securityService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @Order(1)
    public void testGenerateSessionToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/session"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testAuthenticateUserFail() {
        ResponseEntity<?> response = restTemplate.postForEntity(url("/login"), "badToken", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testAuthenticateUserSuccess() {
        ResponseEntity<String> sessionResponse = restTemplate.getForEntity(url("/session"), String.class);
        assertEquals(HttpStatus.OK, sessionResponse.getStatusCode());

        securityService.activateSessionToken(100L, sessionResponse.getBody());

        ResponseEntity<?> loginResponse = restTemplate.postForEntity(url("/login"), sessionResponse.getBody(), String.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    }

    @Test
    public void testWebRegisterAndLogin() {
        var dto = new RegisterDTO("testuser_auth", "TestPass1!");

        ResponseEntity<?> registerResponse = restTemplate.postForEntity(url("/auth/register"), dto, String.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        ResponseEntity<?> loginResponse = restTemplate.postForEntity(url("/auth/login"), dto, String.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
    }

    @Test
    public void testWebRegisterDuplicate() {
        var dto = new RegisterDTO("dupuser_auth", "TestPass1!");
        restTemplate.postForEntity(url("/auth/register"), dto, String.class);

        ResponseEntity<?> response = restTemplate.postForEntity(url("/auth/register"), dto, String.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void testWebLoginWrongPassword() {
        var dto = new RegisterDTO("badpassuser_auth", "TestPass1!");
        restTemplate.postForEntity(url("/auth/register"), dto, String.class);

        ResponseEntity<?> response = restTemplate.postForEntity(url("/auth/login"), new RegisterDTO("badpassuser_auth", "Wrong!"), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
