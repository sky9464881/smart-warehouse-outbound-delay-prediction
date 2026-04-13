package com.example.demo.dto;

import lombok.Data;

@Data
public class FactorySummaryResponse {
    private String layoutId;
    private String layoutType;
    private Integer floorAreaSqm;
    private Integer robotTotal;
}
