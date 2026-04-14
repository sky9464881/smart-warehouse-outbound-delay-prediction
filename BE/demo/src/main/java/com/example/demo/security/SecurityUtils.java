package com.example.demo.security;

import com.example.demo.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static AppUserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required.");
        }
        return principal;
    }

    public static boolean isGlobalAdmin(AppUserPrincipal principal) {
        return principal.getRole() == UserRole.GLOBAL_ADMIN;
    }
}

