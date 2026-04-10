import pandas as pd
from sqlalchemy import create_engine

engine = create_engine(
    'mysql+pymysql://root:dj001106@localhost:3306/warehouse_db'
)

# layout 먼저! (FK 순서)
layout = pd.read_csv('./data/layout_info.csv')
layout.to_sql('warehouse_layout', engine, if_exists='append', index=False)

# snapshot 나중에
snapshot = pd.read_csv('./data/train.csv')
snapshot = snapshot.rename(columns={'ID': 'id'})
snapshot.to_sql('warehouse_snapshot', engine, if_exists='append', index=False)