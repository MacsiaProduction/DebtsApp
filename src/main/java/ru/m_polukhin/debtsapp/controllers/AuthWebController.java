package ru.m_polukhin.debtsapp.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.m_polukhin.debtsapp.dto.RegisterDTO;
import ru.m_polukhin.debtsapp.services.SecurityService;

import java.security.Principal;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthWebController {
    private final SecurityService securityService;

    // Регистрация через веб (логин + пароль)
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO dto) {
        return securityService.registerWeb(dto.username(), dto.password());
    }

    // Вход через веб → JWT
    @PostMapping("/auth/login")
    public ResponseEntity<?> loginWeb(@RequestBody RegisterDTO dto) {
        return securityService.loginWeb(dto.username(), dto.password());
    }

    // Вход через Telegram-сессионный токен → JWT
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody String sessionToken) {
        return securityService.authenticateUser(sessionToken);
    }

    // Получить одноразовый сессионный токен для входа через Telegram
    @GetMapping("/session")
    public ResponseEntity<String> getUserSession() {
        return ResponseEntity.ok(securityService.generateSessionToken());
    }

    // Получить токен привязки Telegram к веб-аккаунту (требует JWT)
    @GetMapping("/auth/link-token")
    public ResponseEntity<String> getLinkToken(Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String token = securityService.generateLinkToken(Long.parseLong(principal.getName()));
        return ResponseEntity.ok(token);
    }

    @GetMapping("/access-denied")
    public ResponseEntity<String> accessDenied() {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }
}
