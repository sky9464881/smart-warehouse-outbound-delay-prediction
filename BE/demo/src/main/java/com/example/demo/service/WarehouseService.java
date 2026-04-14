package com.example.demo.service;

import com.example.demo.dto.CongestionResponse;
import com.example.demo.dto.FactoryDetailResponse;
import com.example.demo.dto.FactoryOverviewResponse;
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
    private final AccessControlService accessControlService;

    public List<FactorySummaryResponse> getFactories() {
        if (accessControlService.isGlobalAdmin()) {
            return warehouseMapper.getFactories();
        }

        List<String> allowedFactoryIds = accessControlService.listAllowedFactoryIds();
        if (allowedFactoryIds.isEmpty()) {
            return List.of();
        }
        return warehouseMapper.getFactoriesByIds(allowedFactoryIds);
    }

    public List<FactoryOverviewResponse> getFactoriesOverview() {
        if (accessControlService.isGlobalAdmin()) {
            return warehouseMapper.getFactoriesOverview(null);
        }

        List<String> allowedFactoryIds = accessControlService.listAllowedFactoryIds();
        if (allowedFactoryIds.isEmpty()) {
            return List.of();
        }
        return warehouseMapper.getFactoriesOverview(allowedFactoryIds);
    }

    public FactoryDetailResponse getFactoryInfo(String layoutId) {
        accessControlService.assertFactoryAccess(layoutId);
        return warehouseMapper.getFactoryInfo(layoutId);
    }

    public List<ScenarioResponse> getScenarios(String layoutId) {
        accessControlService.assertFactoryAccess(layoutId);
        return warehouseMapper.getScenarios(layoutId);
    }

    public List<ShippingDelayResponse> getShippingDelay(String layoutId, String scenarioId) {
        accessControlService.assertFactoryAccess(layoutId);
        return warehouseMapper.getShippingDelay(layoutId, scenarioId);
    }

    public List<OrderInflowResponse> getOrderInflow(String layoutId, String scenarioId) {
        accessControlService.assertFactoryAccess(layoutId);
        return warehouseMapper.getOrderInflow(layoutId, scenarioId);
    }

    public List<RobotSummaryResponse> getRobotSummary(String layoutId, String scenarioId) {
        accessControlService.assertFactoryAccess(layoutId);
        return warehouseMapper.getRobotSummary(layoutId, scenarioId);
    }

    public List<CongestionResponse> getCongestion(String layoutId, String scenarioId) {
        accessControlService.assertFactoryAccess(layoutId);
        return warehouseMapper.getCongestion(layoutId, scenarioId);
    }
}
