package com.example.demo.mapper;

import com.example.demo.dto.WarehouseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface WarehouseMapper {
    List<WarehouseDto> getFactories();
    WarehouseDto getFactoryInfo(@Param("layoutId") String layoutId);
    List<WarehouseDto> getScenarios(@Param("layoutId") String layoutId);
    List<WarehouseDto> getShippingDelay(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
    List<WarehouseDto> getOrderInflow(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
    List<WarehouseDto> getRobotSummary(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
    List<WarehouseDto> getCongestion(@Param("layoutId") String layoutId, @Param("scenarioId") String scenarioId);
}