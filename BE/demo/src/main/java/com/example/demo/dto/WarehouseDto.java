package com.example.demo.dto;

import lombok.Data;

@Data
public class WarehouseDto {
    // 공장 목록 / 기본 정보
    private String layoutId;
    private String layoutType;
    private Integer floorAreaSqm;
    private Integer robotTotal;

    // 시나리오
    private String scenarioId;

    // 출고 지연
    private Double avgDelayMinutesNext30m;

    // 주문 유입량
    private Double orderInflow15m;

    // 로봇 현황
    private Integer robotActive;
    private Integer robotIdle;
    private Integer robotCharging;
    private Double avgIdleDurationMin;

    // 혼잡도
    private Double congestionScore;
}