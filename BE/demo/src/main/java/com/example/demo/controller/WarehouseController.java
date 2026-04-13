package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CongestionDto;
import com.example.demo.dto.FactoryDto;
import com.example.demo.dto.OrderInflowDto;
import com.example.demo.dto.RobotSummaryDto;
import com.example.demo.dto.ScenarioDto;
import com.example.demo.dto.ShippingDelayDto;
import com.example.demo.service.WarehouseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/factories")
    public List<FactoryDto> getFactories() {
        return warehouseService.getFactories();
    }

    @GetMapping("/factories/{factoryId}")
    public FactoryDto getFactoryInfo(@PathVariable String factoryId) {
        return warehouseService.getFactoryInfo(factoryId);
    }

    @GetMapping("/factories/{factoryId}/scenarios")
    public List<ScenarioDto> getScenarios(@PathVariable String factoryId) {
        return warehouseService.getScenarios(factoryId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/shipping-delay")
    public List<ShippingDelayDto> getShippingDelay(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getShippingDelay(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/order-inflow")
    public List<OrderInflowDto> getOrderInflow(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getOrderInflow(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/robot-summary")
    public List<RobotSummaryDto> getRobotSummary(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getRobotSummary(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/congestion")
    public List<CongestionDto> getCongestion(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getCongestion(factoryId, scenarioId);
    }
}
