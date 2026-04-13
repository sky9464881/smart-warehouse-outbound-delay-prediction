package com.example.demo.mapper;

import com.example.demo.dto.CongestionResponse;
import com.example.demo.dto.FactoryDetailResponse;
import com.example.demo.dto.FactorySummaryResponse;
import com.example.demo.dto.OrderInflowResponse;
import com.example.demo.dto.RobotSummaryResponse;
import com.example.demo.dto.ScenarioResponse;
import com.example.demo.dto.ShippingDelayResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WarehouseMapper {
    List<FactorySummaryResponse> getFactories();

    FactoryDetailResponse getFactoryInfo(@Param("layoutId") String layoutId);

    List<ScenarioResponse> getScenarios(@Param("layoutId") String layoutId);

    List<ShippingDelayResponse> getShippingDelay(
            @Param("layoutId") String layoutId,
            @Param("scenarioId") String scenarioId
    );

    List<OrderInflowResponse> getOrderInflow(
            @Param("layoutId") String layoutId,
            @Param("scenarioId") String scenarioId
    );

    List<RobotSummaryResponse> getRobotSummary(
            @Param("layoutId") String layoutId,
            @Param("scenarioId") String scenarioId
    );

    List<CongestionResponse> getCongestion(
            @Param("layoutId") String layoutId,
            @Param("scenarioId") String scenarioId
    );
}
