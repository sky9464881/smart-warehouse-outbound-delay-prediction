USE warehouse_db;

-- 1. 공장 목록 (드롭다운 1)
CREATE VIEW v_factories AS
SELECT layout_id,
       layout_type,
       floor_area_sqm,
       robot_total
FROM warehouse_layout;

-- 2. 공장 기본 정보
CREATE VIEW v_factory_info AS
SELECT layout_id,
       layout_type,
       floor_area_sqm,
       robot_total
FROM warehouse_layout;

-- 3. 시나리오 목록 (드롭다운 2)
CREATE VIEW v_scenarios AS
SELECT DISTINCT layout_id,
                scenario_id
FROM warehouse_snapshot;

-- 4. 예측 출고 지연 시간 (타임슬롯 전체)
CREATE VIEW v_dashboard_shipping_delay AS
SELECT layout_id,
       scenario_id,
       avg_delay_minutes_next_30m
FROM warehouse_snapshot;

-- 5. 주문 유입량 (타임슬롯 전체)
CREATE VIEW v_dashboard_order_inflow AS
SELECT layout_id,
       scenario_id,
       order_inflow_15m
FROM warehouse_snapshot;

-- 6. 로봇 운영 현황 (타임슬롯 전체)
CREATE VIEW v_dashboard_robot_summary AS
SELECT layout_id,
       scenario_id,
       robot_active,
       robot_idle,
       robot_charging,
       avg_idle_duration_min
FROM warehouse_snapshot;

-- 7. 혼잡도 (타임슬롯 전체)
CREATE VIEW v_dashboard_congestion AS
SELECT layout_id,
       scenario_id,
       congestion_score
FROM warehouse_snapshot;