package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ShippingDelayResponse {
    private String layoutId;
    private String scenarioId;
    private LocalDateTime snapshotTime;
    private Double avgDelayMinutesNext30m;
}
