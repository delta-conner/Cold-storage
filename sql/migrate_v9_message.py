# -*- coding: utf-8 -*-
"""模块⑨：消息中心表"""
import pymysql

conn = pymysql.connect(host='localhost', user='root', password='123456',
                       database='cold_storage_oms', charset='utf8mb4')
cur = conn.cursor()
cur.execute("""
CREATE TABLE IF NOT EXISTS sys_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  content VARCHAR(1000) DEFAULT NULL,
  msg_type VARCHAR(40) NOT NULL COMMENT 'ORDER_NEW/ORDER_ASSIGN/ORDER_DONE/MAINTAIN_DUE/STOCK_LOW',
  biz_type VARCHAR(40) DEFAULT NULL,
  biz_id BIGINT DEFAULT NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_msg_user (user_id, is_read),
  INDEX idx_msg_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ sys_message')
conn.commit()
cur.close()
conn.close()
print('migrate_v9_message done')
