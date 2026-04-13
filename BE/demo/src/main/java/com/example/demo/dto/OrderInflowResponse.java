package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderInflowResponse {
    private String layoutId;
    private String scenarioId;
    private LocalDateTime snapshotTime;
    private Double orderInflow15m;
}
