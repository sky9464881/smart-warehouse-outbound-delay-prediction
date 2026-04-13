package com.example.demo.mapper;

import com.example.demo.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface WarehouseMapper {
    List<FactoryDto> getFactories();
    FactoryDto getFactoryInfo(@Param("layoutId") String layoutId);
    List<ScenarioDto> getScenarios(@Param("layoutId") String layoutId);
    List<ShippingDelayDto> getShippingDelay(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
    List<OrderInflowDto> getOrderInflow(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
    List<RobotSummaryDto> getRobotSummary(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
    List<CongestionDto> getCongestion(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
}
