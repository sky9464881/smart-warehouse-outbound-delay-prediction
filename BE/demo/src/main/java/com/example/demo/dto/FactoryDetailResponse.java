package com.example.demo.dto;

import lombok.Data;

@Data
public class FactoryDetailResponse {
    private String layoutId;
    private String layoutType;
    private Integer floorAreaSqm;
    private Integer robotTotal;
}
