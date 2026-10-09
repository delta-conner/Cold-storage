# -*- coding: utf-8 -*-
"""模块③：设备生命周期状态日志表（可重复执行）"""
import pymysql

conn = pymysql.connect(host='localhost', user='root', password='123456',
                       database='cold_storage_oms', charset='utf8mb4')
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS device_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL,
  from_status VARCHAR(20) DEFAULT NULL COMMENT '原状态',
  to_status VARCHAR(20) NOT NULL COMMENT '新状态',
  reason VARCHAR(500) DEFAULT NULL COMMENT '原因/备注',
  source VARCHAR(40) DEFAULT NULL COMMENT 'WORK_ORDER/MANUAL/SYSTEM',
  operator_id BIGINT DEFAULT NULL,
  operator_name VARCHAR(50) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dsl_device (device_id),
  INDEX idx_dsl_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态变更历史'
""")
print('+ device_status_log ready')

# 为已有设备补一条“当前状态”起点记录（仅当该设备尚无日志）
cur.execute("SELECT id, status FROM device")
devices = cur.fetchall()
for did, st in devices:
    cur.execute("SELECT COUNT(1) FROM device_status_log WHERE device_id=%s", (did,))
    if cur.fetchone()[0] == 0:
        cur.execute(
            "INSERT INTO device_status_log(device_id, from_status, to_status, reason, source, operator_name) "
            "VALUES(%s, NULL, %s, %s, %s, %s)",
            (did, st or 'NORMAL', '系统初始化', 'SYSTEM', '系统')
        )
print('seed logs for', len(devices), 'devices')

conn.commit()
cur.close()
conn.close()
print('migrate_v3_lifecycle done')
