package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FactoryOverviewResponse {
    private String layoutId;
    private String scenarioId;
    private LocalDateTime snapshotTime;

    private Double avgDelayMinutesNext30m;
    private Double orderInflow15m;
    private Integer robotActive;
    private Integer robotIdle;
    private Integer robotCharging;
    private Double avgIdleDurationMin;
    private Double congestionScore;
}

