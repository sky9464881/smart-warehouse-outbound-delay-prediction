USE warehouse_db;

CREATE OR REPLACE VIEW v_factories AS
SELECT layout_id,
       layout_type,
       floor_area_sqm,
       robot_total
FROM warehouse_layout;

CREATE OR REPLACE VIEW v_factory_info AS
SELECT layout_id,
       layout_type,
       floor_area_sqm,
       robot_total
FROM warehouse_layout;

CREATE OR REPLACE VIEW v_scenarios AS
SELECT DISTINCT layout_id,
                scenario_id
FROM warehouse_snapshot;

CREATE OR REPLACE VIEW v_dashboard_shipping_delay AS
SELECT layout_id,
       scenario_id,
       snapshot_time,
       avg_delay_minutes_next_30m
FROM warehouse_snapshot;

CREATE OR REPLACE VIEW v_dashboard_order_inflow AS
SELECT layout_id,
       scenario_id,
       snapshot_time,
       order_inflow_15m
FROM warehouse_snapshot;

CREATE OR REPLACE VIEW v_dashboard_robot_summary AS
SELECT layout_id,
       scenario_id,
       snapshot_time,
       robot_active,
       robot_idle,
       robot_charging,
       avg_idle_duration_min
FROM warehouse_snapshot;

CREATE OR REPLACE VIEW v_dashboard_congestion AS
SELECT layout_id,
       scenario_id,
       snapshot_time,
       congestion_score
FROM warehouse_snapshot;
