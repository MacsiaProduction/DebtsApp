package ru.m_polukhin.debtsapp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.m_polukhin.debtsapp.dto.RegisterDTO;
import ru.m_polukhin.debtsapp.services.SecurityService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthWebControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private AuthWebController authWebController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authWebController).build();
    }

    @Test
    void testGenerateSessionToken() throws Exception {
        when(securityService.generateSessionToken()).thenReturn("session-token");

        mockMvc.perform(get("/session"))
                .andExpect(status().isOk())
                .andExpect(content().string("session-token"));
    }

    @Test
    void testAuthenticateUserFail() throws Exception {
        ResponseEntity<?> response = ResponseEntity.badRequest().body("Invalid token");
        doReturn(response).when(securityService).authenticateUser("badToken");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("badToken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid token"));
    }

    @Test
    void testAuthenticateUserSuccess() throws Exception {
        ResponseEntity<?> response = ResponseEntity.ok("jwt-token");
        doReturn(response).when(securityService).authenticateUser("goodToken");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("goodToken"))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token"));
    }

    @Test
    void testWebRegisterAndLogin() throws Exception {
        RegisterDTO dto = new RegisterDTO("testuser_auth", "TestPass1!");
        ResponseEntity<?> registerResponse = new ResponseEntity<>("Registered successfully", HttpStatus.CREATED);
        ResponseEntity<?> loginResponse = ResponseEntity.ok("jwt-token");
        doReturn(registerResponse).when(securityService).registerWeb(dto.username(), dto.password());
        doReturn(loginResponse).when(securityService).loginWeb(dto.username(), dto.password());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt-token"));
    }

    @Test
    void testWebRegisterDuplicate() throws Exception {
        RegisterDTO dto = new RegisterDTO("dupuser_auth", "TestPass1!");
        ResponseEntity<?> response = new ResponseEntity<>("Username already taken", HttpStatus.CONFLICT);
        doReturn(response).when(securityService).registerWeb(dto.username(), dto.password());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void testWebLoginWrongPassword() throws Exception {
        RegisterDTO dto = new RegisterDTO("badpassuser_auth", "Wrong!");
        ResponseEntity<?> response = new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        doReturn(response).when(securityService).loginWeb(dto.username(), dto.password());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLinkTokenRequiresPrincipal() throws Exception {
        mockMvc.perform(get("/auth/link-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLinkTokenUsesAuthenticatedPrincipal() throws Exception {
        when(securityService.generateLinkToken(42L)).thenReturn("link-token");

        mockMvc.perform(get("/auth/link-token").principal(() -> "42"))
                .andExpect(status().isOk())
                .andExpect(content().string("link-token"));

        verify(securityService).generateLinkToken(42L);
    }
}
