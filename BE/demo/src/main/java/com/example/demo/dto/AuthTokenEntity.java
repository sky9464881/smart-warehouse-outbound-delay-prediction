package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuthTokenEntity {
    private String token;
    private Long userId;
    private LocalDateTime expiresAt;
}

