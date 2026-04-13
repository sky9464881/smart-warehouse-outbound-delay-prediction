package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CongestionDto {
    private String layoutId;
    private String scenarioId;
    private LocalDateTime snapshotTime;
    private Double congestionScore;
    private Double maxZoneDensity;
    private Integer blockedPath15m;
    private Integer nearCollision15m;
    private Double aisleTrafficScore;
}
