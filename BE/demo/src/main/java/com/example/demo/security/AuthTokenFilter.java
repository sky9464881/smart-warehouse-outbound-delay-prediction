package com.example.demo.security;

import com.example.demo.dto.AuthTokenEntity;
import com.example.demo.dto.UserEntity;
import com.example.demo.mapper.UserMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring("Bearer ".length()).trim();
                if (!token.isBlank()) {
                    authenticate(token, request);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        AuthTokenEntity tokenEntity = userMapper.findToken(token);
        if (tokenEntity == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (tokenEntity.getExpiresAt() == null || tokenEntity.getExpiresAt().isBefore(now)) {
            userMapper.deleteToken(token);
            return;
        }

        UserEntity user = userMapper.findUserById(tokenEntity.getUserId());
        if (user == null) {
            userMapper.deleteToken(token);
            return;
        }

        UserRole role;
        try {
            role = UserRole.valueOf(user.getRole());
        } catch (IllegalArgumentException ignored) {
            return;
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        AppUserPrincipal principal = new AppUserPrincipal(user.getId(), user.getEmail(), user.getDisplayName(), role, token);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

