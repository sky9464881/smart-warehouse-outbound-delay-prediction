USE warehouse_db;

ALTER TABLE warehouse_snapshot
    ADD COLUMN IF NOT EXISTS snapshot_time DATETIME
        COMMENT 'Synthetic 15-minute snapshot timestamp per factory scenario'
        AFTER scenario_id;

UPDATE warehouse_snapshot ws
JOIN (
    SELECT id,
           DATE_ADD(
               '2024-01-01 00:00:00',
               INTERVAL (ROW_NUMBER() OVER (
                   PARTITION BY layout_id, scenario_id
                   ORDER BY id
               ) - 1) * 15 MINUTE
           ) AS generated_snapshot_time
    FROM warehouse_snapshot
) ranked
    ON ws.id = ranked.id
SET ws.snapshot_time = COALESCE(ws.snapshot_time, ranked.generated_snapshot_time);

ALTER TABLE warehouse_snapshot
    MODIFY COLUMN snapshot_time DATETIME NOT NULL
        COMMENT 'Synthetic 15-minute snapshot timestamp per factory scenario';
