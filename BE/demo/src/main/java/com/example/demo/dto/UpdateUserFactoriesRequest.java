package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class UpdateUserFactoriesRequest {
    @NotNull(message = "factoryIds is required.")
    private List<String> factoryIds;
}

