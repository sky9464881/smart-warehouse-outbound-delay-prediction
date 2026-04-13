from pathlib import Path

import pandas as pd
from sqlalchemy import create_engine

BASE_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BASE_DIR / "data"
TIMESTAMP_START = pd.Timestamp("2024-01-01 00:00:00")

engine = create_engine("mysql+pymysql://root:root@localhost:3306/warehouse_db")


def attach_snapshot_time(snapshot: pd.DataFrame) -> pd.DataFrame:
    snapshot = snapshot.copy()

    for column_name in ("snapshot_time", "time_stamp", "timestamp"):
        if column_name in snapshot.columns:
            snapshot["snapshot_time"] = pd.to_datetime(snapshot[column_name])
            columns_to_drop = [
                candidate
                for candidate in ("time_stamp", "timestamp")
                if candidate in snapshot.columns
            ]
            if columns_to_drop:
                snapshot = snapshot.drop(columns=columns_to_drop)
            return snapshot

    snapshot = snapshot.sort_values(["layout_id", "scenario_id", "id"]).reset_index(drop=True)
    sequence = snapshot.groupby(["layout_id", "scenario_id"], dropna=False).cumcount()
    snapshot["snapshot_time"] = TIMESTAMP_START + pd.to_timedelta(sequence * 15, unit="m")
    return snapshot


layout = pd.read_csv(DATA_DIR / "layout_info.csv")
layout.to_sql("warehouse_layout", engine, if_exists="append", index=False)

snapshot = pd.read_csv(DATA_DIR / "train.csv")
snapshot = snapshot.rename(columns={"ID": "id"})
snapshot = attach_snapshot_time(snapshot)
snapshot.to_sql("warehouse_snapshot", engine, if_exists="append", index=False)
