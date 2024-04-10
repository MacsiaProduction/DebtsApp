package ru.m_polukhin.debtsapp.controllers;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @ApiResponse(code = 400, message = "Bad credentials")
    })
    @PreAuthorize("permitAll")
    public ResponseEntity<?> authenticateUser(@RequestBody String sessionToken) {
        return securityService.authenticateUser(sessionToken);
    }

    @Operation(summary = "Get session token for access via telegram")
    @GetMapping("/session")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Get token successfully"),
    })
    @PreAuthorize("permitAll")
    public ResponseEntity<String> getUserSession() {
        return ResponseEntity.ok(securityService.generateSessionToken());
    }

    @Operation(summary = "Page for the case that access was denied")
    @GetMapping("/access-denied")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Access was denied"),
    })
    @PreAuthorize("permitAll")
    public ResponseEntity<String> accessDenied() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}