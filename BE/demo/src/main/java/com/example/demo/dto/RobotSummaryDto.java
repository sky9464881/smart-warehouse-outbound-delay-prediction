package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RobotSummaryDto {
    private String layoutId;
    private String scenarioId;
    private LocalDateTime snapshotTime;
    private Integer robotActive;
    private Integer robotIdle;
    private Integer robotCharging;
    private Double avgIdleDurationMin;
}
