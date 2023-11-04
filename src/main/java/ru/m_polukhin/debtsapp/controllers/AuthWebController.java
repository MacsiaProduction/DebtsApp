package ru.m_polukhin.debtsapp.controllers;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.m_polukhin.debtsapp.services.SecurityService;

@RestController
@RequiredArgsConstructor
public class AuthWebController {
    private final SecurityService securityService;

    @Operation(summary = "Login page", description = "Returns jwt token")
    @PostMapping("/login")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User login successfully"),
            @ApiResponse(code = 401, message = "Unauthorized")
    })
    public ResponseEntity<?> authenticateUser(@RequestBody String sessionToken) {
        try {
            return ResponseEntity.ok(securityService.authenticateUser(sessionToken));
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @Operation(summary = "Get session token for access via telegram")
    @GetMapping("/session")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Get token successfully"),
    })
    public ResponseEntity<String> authenticateUser() {
        return ResponseEntity.ok(securityService.generateSessionToken());
    }
}