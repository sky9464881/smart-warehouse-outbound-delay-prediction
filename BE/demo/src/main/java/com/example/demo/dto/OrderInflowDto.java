package com.example.demo.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderInflowDto {
    private String layoutId;
    private String scenarioId;
    private LocalDateTime snapshotTime;
    private Double orderInflow15m;
    private Integer uniqueSku15m;
    private Double urgentOrderRatio;
    private Double avgItemsPerOrder;
}
