package com.example.demo.security;

import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AppUserPrincipal implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String displayName;
    private final UserRole role;
    private final String token;
}

