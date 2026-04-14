package com.example.demo.service;

import com.example.demo.dto.AuthLoginRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.AuthSignupRequest;
import com.example.demo.dto.MeResponse;
import com.example.demo.dto.UserEntity;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.security.UserRole;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.token.ttl-hours:168}")
    private long tokenTtlHours;

    public AuthResponse signup(AuthSignupRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userMapper.findUserByEmail(email) != null) {
            throw new ConflictException("Email is already registered.");
        }

        UserRole role = userMapper.existsGlobalAdmin() ? UserRole.FACTORY_ADMIN : UserRole.GLOBAL_ADMIN;

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role.name());
        userMapper.insertUser(user);

        String token = issueToken(user.getId());
        return buildAuthResponse(user, token);
    }

    public AuthResponse login(AuthLoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        UserEntity user = userMapper.findUserByEmail(email);
        if (user == null) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String token = issueToken(user.getId());
        return buildAuthResponse(user, token);
    }

    public MeResponse me(AppUserPrincipal principal) {
        UserEntity user = userMapper.findUserById(principal.getId());
        if (user == null) {
            throw new UnauthorizedException("User not found.");
        }
        return buildMeResponse(user);
    }

    public void logout(String token) {
        userMapper.deleteToken(token);
    }

    private String issueToken(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        userMapper.deleteExpiredTokens(now);

        String token = generateToken();
        LocalDateTime expiresAt = now.plusHours(tokenTtlHours);
        userMapper.insertToken(token, userId, expiresAt);
        return token;
    }

    private AuthResponse buildAuthResponse(UserEntity user, String token) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(buildMeResponse(user));
        return response;
    }

    private MeResponse buildMeResponse(UserEntity user) {
        MeResponse response = new MeResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setRole(user.getRole());

        if (UserRole.GLOBAL_ADMIN.name().equals(user.getRole())) {
            response.setFactoryIds(List.of());
        } else {
            response.setFactoryIds(userMapper.listUserFactoryIds(user.getId()));
        }
        return response;
    }

    private String generateToken() {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}

