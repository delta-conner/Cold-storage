# -*- coding: utf-8 -*-
"""模块①数据库增量迁移（可重复执行）"""
import pymysql

conn = pymysql.connect(host='localhost', user='root', password='123456',
                       database='cold_storage_oms', charset='utf8mb4')
cur = conn.cursor()

def cols(table):
    cur.execute("""
        SELECT COLUMN_NAME FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA='cold_storage_oms' AND TABLE_NAME=%s
    """, (table,))
    return {r[0] for r in cur.fetchall()}

def add_col(table, col, ddl):
    if col not in cols(table):
        cur.execute(f'ALTER TABLE `{table}` ADD COLUMN {ddl}')
        print(f'+ {table}.{col}')
    else:
        print(f'= {table}.{col} exists')

add_col('sys_user', 'status',
        "status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0禁用' AFTER role")

add_col('cold_storage', 'contact_name',
        "contact_name VARCHAR(50) DEFAULT NULL COMMENT '联系人' AFTER address")
add_col('cold_storage', 'contact_phone',
        "contact_phone VARCHAR(20) DEFAULT NULL AFTER contact_name")
add_col('cold_storage', 'status',
        "status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED' AFTER contact_phone")
add_col('cold_storage', 'commission_date',
        "commission_date DATE DEFAULT NULL COMMENT '投用时间' AFTER status")

add_col('cold_room', 'status',
        "status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' AFTER code")
add_col('cold_room', 'temp_min',
        "temp_min DECIMAL(6,2) DEFAULT NULL AFTER status")
add_col('cold_room', 'temp_max',
        "temp_max DECIMAL(6,2) DEFAULT NULL AFTER temp_min")
add_col('cold_room', 'volume',
        "volume DECIMAL(10,2) DEFAULT NULL AFTER temp_max")

add_col('device', 'brand', "brand VARCHAR(50) DEFAULT NULL AFTER device_type")
add_col('device', 'model', "model VARCHAR(50) DEFAULT NULL AFTER brand")
add_col('device', 'status',
        "status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER is_public")
add_col('device', 'rated_params',
        "rated_params VARCHAR(255) DEFAULT NULL AFTER owner_ops_id")
add_col('device', 'fault_count',
        "fault_count INT NOT NULL DEFAULT 0 AFTER rated_params")

cur.execute("""
CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  username VARCHAR(50) DEFAULT NULL,
  role VARCHAR(20) DEFAULT NULL,
  module VARCHAR(50) DEFAULT NULL,
  action VARCHAR(50) DEFAULT NULL,
  detail VARCHAR(500) DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_oplog_time (create_time),
  INDEX idx_oplog_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ operation_log')

cur.execute("""UPDATE cold_storage SET contact_name='王主管', contact_phone='0777-8888888',
            status='ENABLED', commission_date='2022-06-01' WHERE id=1""")
cur.execute("""UPDATE cold_room SET status='ENABLED', temp_min=-25, temp_max=-18, volume=500
            WHERE id IN (1,2,3,4)""")
cur.execute("""UPDATE cold_room SET status='ENABLED' WHERE id=5""")
cur.execute("UPDATE device SET status='NORMAL' WHERE status IS NULL OR status=''")
cur.execute("UPDATE device SET brand='约克', model='YCAI' WHERE device_type='约克主机' AND (brand IS NULL OR brand='')")
cur.execute("UPDATE device SET brand='比泽尔', model='CSH' WHERE device_type='比泽尔主机' AND (brand IS NULL OR brand='')")
cur.execute("UPDATE device SET brand='复叠', model='CAS-01' WHERE device_type='复叠活塞机' AND (brand IS NULL OR brand='')")
cur.execute("UPDATE device SET brand='通用', model='STD' WHERE brand IS NULL OR brand=''")
cur.execute("UPDATE sys_user SET status=1")

conn.commit()
conn.close()
print('migration_v1_foundation done')
