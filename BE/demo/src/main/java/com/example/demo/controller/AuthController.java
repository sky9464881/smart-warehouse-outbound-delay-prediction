package com.example.demo.controller;

import com.example.demo.dto.AuthLoginRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.AuthSignupRequest;
import com.example.demo.dto.MeResponse;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.security.SecurityUtils;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody AuthSignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeResponse me() {
        AppUserPrincipal principal = SecurityUtils.requirePrincipal();
        return authService.me(principal);
    }

    @PostMapping("/logout")
    public void logout() {
        AppUserPrincipal principal = SecurityUtils.requirePrincipal();
        authService.logout(principal.getToken());
    }
}

