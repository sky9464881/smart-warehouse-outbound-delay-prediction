package com.example.demo.service;

import com.example.demo.dto.CongestionResponse;
import com.example.demo.dto.FactoryDetailResponse;
import com.example.demo.dto.FactorySummaryResponse;
import com.example.demo.dto.OrderInflowResponse;
import com.example.demo.dto.RobotSummaryResponse;
import com.example.demo.dto.ScenarioResponse;
import com.example.demo.dto.ShippingDelayResponse;
import com.example.demo.mapper.WarehouseMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public List<FactorySummaryResponse> getFactories() {
        return warehouseMapper.getFactories();
    }

    public FactoryDetailResponse getFactoryInfo(String layoutId) {
        return warehouseMapper.getFactoryInfo(layoutId);
    }

    public List<ScenarioResponse> getScenarios(String layoutId) {
        return warehouseMapper.getScenarios(layoutId);
    }

    public List<ShippingDelayResponse> getShippingDelay(String layoutId, String scenarioId) {
        return warehouseMapper.getShippingDelay(layoutId, scenarioId);
    }

    public List<OrderInflowResponse> getOrderInflow(String layoutId, String scenarioId) {
        return warehouseMapper.getOrderInflow(layoutId, scenarioId);
    }

    public List<RobotSummaryResponse> getRobotSummary(String layoutId, String scenarioId) {
        return warehouseMapper.getRobotSummary(layoutId, scenarioId);
    }

    public List<CongestionResponse> getCongestion(String layoutId, String scenarioId) {
        return warehouseMapper.getCongestion(layoutId, scenarioId);
    }
}
