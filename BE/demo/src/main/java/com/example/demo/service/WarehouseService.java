package com.example.demo.service;

import com.example.demo.dto.WarehouseDto;
import com.example.demo.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public List<WarehouseDto> getFactories() {
        return warehouseMapper.getFactories();
    }

    public WarehouseDto getFactoryInfo(String layoutId) {
        return warehouseMapper.getFactoryInfo(layoutId);
    }

    public List<WarehouseDto> getScenarios(String layoutId) {
        return warehouseMapper.getScenarios(layoutId);
    }

    public List<WarehouseDto> getShippingDelay(String layoutId, String scenarioId) {
        return warehouseMapper.getShippingDelay(layoutId, scenarioId);
    }

    public List<WarehouseDto> getOrderInflow(String layoutId, String scenarioId) {
        return warehouseMapper.getOrderInflow(layoutId, scenarioId);
    }

    public List<WarehouseDto> getRobotSummary(String layoutId, String scenarioId) {
        return warehouseMapper.getRobotSummary(layoutId, scenarioId);
    }

    public List<WarehouseDto> getCongestion(String layoutId, String scenarioId) {
        return warehouseMapper.getCongestion(layoutId, scenarioId);
    }
}