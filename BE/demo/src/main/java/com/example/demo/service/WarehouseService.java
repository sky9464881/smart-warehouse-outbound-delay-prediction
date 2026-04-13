package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.CongestionDto;
import com.example.demo.dto.FactoryDto;
import com.example.demo.dto.OrderInflowDto;
import com.example.demo.dto.RobotSummaryDto;
import com.example.demo.dto.ScenarioDto;
import com.example.demo.dto.ShippingDelayDto;
import com.example.demo.mapper.WarehouseMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public List<FactoryDto> getFactories() {
        return warehouseMapper.getFactories();
    }

    public FactoryDto getFactoryInfo(String layoutId) {
        return warehouseMapper.getFactoryInfo(layoutId);
    }

    public List<ScenarioDto> getScenarios(String layoutId) {
        return warehouseMapper.getScenarios(layoutId);
    }

    public List<ShippingDelayDto> getShippingDelay(String layoutId, String scenarioId) {
        return warehouseMapper.getShippingDelay(layoutId, scenarioId);
    }

    public List<OrderInflowDto> getOrderInflow(String layoutId, String scenarioId) {
        return warehouseMapper.getOrderInflow(layoutId, scenarioId);
    }

    public List<RobotSummaryDto> getRobotSummary(String layoutId, String scenarioId) {
        return warehouseMapper.getRobotSummary(layoutId, scenarioId);
    }

    public List<CongestionDto> getCongestion(String layoutId, String scenarioId) {
        return warehouseMapper.getCongestion(layoutId, scenarioId);
    }
}
