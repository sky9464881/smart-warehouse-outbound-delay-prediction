package com.example.demo.controller;

import com.example.demo.dto.CongestionResponse;
import com.example.demo.dto.FactoryDetailResponse;
import com.example.demo.dto.FactoryOverviewResponse;
import com.example.demo.dto.FactorySummaryResponse;
import com.example.demo.dto.OrderInflowResponse;
import com.example.demo.dto.RobotSummaryResponse;
import com.example.demo.dto.ScenarioResponse;
import com.example.demo.dto.ShippingDelayResponse;
import com.example.demo.service.WarehouseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/factories")
    public List<FactorySummaryResponse> getFactories() {
        return warehouseService.getFactories();
    }

    @GetMapping("/factories/overview")
    public List<FactoryOverviewResponse> getFactoriesOverview() {
        return warehouseService.getFactoriesOverview();
    }

    @GetMapping("/factories/{factoryId}")
    public FactoryDetailResponse getFactoryInfo(@PathVariable String factoryId) {
        return warehouseService.getFactoryInfo(factoryId);
    }

    @GetMapping("/factories/{factoryId}/scenarios")
    public List<ScenarioResponse> getScenarios(@PathVariable String factoryId) {
        return warehouseService.getScenarios(factoryId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/shipping-delay")
    public List<ShippingDelayResponse> getShippingDelay(
            @PathVariable String factoryId,
            @PathVariable String scenarioId
    ) {
        return warehouseService.getShippingDelay(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/order-inflow")
    public List<OrderInflowResponse> getOrderInflow(
            @PathVariable String factoryId,
            @PathVariable String scenarioId
    ) {
        return warehouseService.getOrderInflow(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/robot-summary")
    public List<RobotSummaryResponse> getRobotSummary(
            @PathVariable String factoryId,
            @PathVariable String scenarioId
    ) {
        return warehouseService.getRobotSummary(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/congestion")
    public List<CongestionResponse> getCongestion(
            @PathVariable String factoryId,
            @PathVariable String scenarioId
    ) {
        return warehouseService.getCongestion(factoryId, scenarioId);
    }
}
