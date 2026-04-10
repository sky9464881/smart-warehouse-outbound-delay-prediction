package com.example.demo.controller;

import com.example.demo.dto.WarehouseDto;
import com.example.demo.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/factories")
    public List<WarehouseDto> getFactories() {
        return warehouseService.getFactories();
    }

    @GetMapping("/factories/{factoryId}")
    public WarehouseDto getFactoryInfo(@PathVariable String factoryId) {
        return warehouseService.getFactoryInfo(factoryId);
    }

    @GetMapping("/factories/{factoryId}/scenarios")
    public List<WarehouseDto> getScenarios(@PathVariable String factoryId) {
        return warehouseService.getScenarios(factoryId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/shipping-delay")
    public List<WarehouseDto> getShippingDelay(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getShippingDelay(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/order-inflow")
    public List<WarehouseDto> getOrderInflow(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getOrderInflow(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/robot-summary")
    public List<WarehouseDto> getRobotSummary(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getRobotSummary(factoryId, scenarioId);
    }

    @GetMapping("/factories/{factoryId}/scenarios/{scenarioId}/dashboard/congestion")
    public List<WarehouseDto> getCongestion(
            @PathVariable String factoryId,
            @PathVariable String scenarioId) {
        return warehouseService.getCongestion(factoryId, scenarioId);
    }
}