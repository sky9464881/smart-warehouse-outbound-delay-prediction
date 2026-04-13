package com.example.demo.dto;

import lombok.Data;

@Data
public class CongestionPointResponse {
    private String layoutId;
    private Double congestionScore;
}
