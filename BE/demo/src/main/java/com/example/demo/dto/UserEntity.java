package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserEntity {
    private Long id;
    private String email;
    private String displayName;
    private String passwordHash;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

