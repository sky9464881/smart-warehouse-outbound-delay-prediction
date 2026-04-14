package com.example.demo.dto;

import java.util.List;
import lombok.Data;

@Data
public class MeResponse {
    private Long id;
    private String email;
    private String displayName;
    private String role;
    private List<String> factoryIds;
}

