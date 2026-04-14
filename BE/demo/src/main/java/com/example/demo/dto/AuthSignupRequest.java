package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthSignupRequest {
    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    private String email;

    @NotBlank(message = "Display name is required.")
    @Size(min = 2, max = 80, message = "Display name must be between 2 and 80 characters.")
    private String displayName;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be at least 8 characters.")
    private String password;
}

