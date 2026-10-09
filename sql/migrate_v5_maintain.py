# -*- coding: utf-8 -*-
"""模块⑤：维保计划 + 维保任务（可重复执行）"""
import pymysql
from datetime import date, timedelta

conn = pymysql.connect(host='localhost', user='root', password='123456',
                       database='cold_storage_oms', charset='utf8mb4')
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS maintain_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_no VARCHAR(40) NOT NULL UNIQUE,
  title VARCHAR(100) NOT NULL,
  cycle_type VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/HALF_YEAR/YEARLY',
  device_type VARCHAR(50) DEFAULT NULL COMMENT '空=全部类型',
  due_date DATE NOT NULL COMMENT '计划到期日',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'DRAFT/ACTIVE/CLOSED',
  remark VARCHAR(500) DEFAULT NULL,
  creator_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_mp_due (due_date),
  INDEX idx_mp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ maintain_plan')

cur.execute("""
CREATE TABLE IF NOT EXISTS maintain_plan_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  sort_no INT DEFAULT 0,
  INDEX idx_mpi_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ maintain_plan_item')

cur.execute("""
CREATE TABLE IF NOT EXISTS maintain_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(40) NOT NULL UNIQUE,
  plan_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  due_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE',
  assignee_id BIGINT DEFAULT NULL,
  result_summary VARCHAR(1000) DEFAULT NULL,
  has_abnormal TINYINT DEFAULT 0,
  measures VARCHAR(1000) DEFAULT NULL,
  parts_used VARCHAR(500) DEFAULT NULL,
  finish_time DATETIME DEFAULT NULL,
  handler_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_mt_plan (plan_id),
  INDEX idx_mt_device (device_id),
  INDEX idx_mt_status (status),
  INDEX idx_mt_due (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ maintain_task')

cur.execute("""
CREATE TABLE IF NOT EXISTS maintain_task_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  result VARCHAR(20) DEFAULT NULL COMMENT 'OK/ABNORMAL/NA',
  remark VARCHAR(500) DEFAULT NULL,
  sort_no INT DEFAULT 0,
  INDEX idx_mti_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ maintain_task_item')

cur.execute("SELECT COUNT(1) FROM maintain_plan")
if cur.fetchone()[0] == 0:
    due = (date.today() + timedelta(days=15)).isoformat()
    cur.execute(
        "INSERT INTO maintain_plan(plan_no, title, cycle_type, device_type, due_date, status, remark, creator_id) "
        "VALUES(%s,%s,%s,%s,%s,'ACTIVE',%s,1)",
        ('MP20260923001', '主机月度维保计划', 'MONTHLY', '约克主机', due, '检查油位油温高低压电流运行声音')
    )
    plan_id = cur.lastrowid
    items = ['油位检查', '油温检查', '高低压读数', '运行电流', '运行声音']
    for i, name in enumerate(items):
        cur.execute("INSERT INTO maintain_plan_item(plan_id, item_name, sort_no) VALUES(%s,%s,%s)",
                    (plan_id, name, i + 1))
    # 生成任务：匹配约克主机
    cur.execute("SELECT id FROM device WHERE device_type=%s AND (status IS NULL OR status<>'SCRAPPED')", ('约克主机',))
    devices = cur.fetchall()
    for idx, (did,) in enumerate(devices):
        task_no = f'MT2026092300{idx+1:02d}'
        cur.execute(
            "INSERT INTO maintain_task(task_no, plan_id, device_id, due_date, status) VALUES(%s,%s,%s,%s,'PENDING')",
            (task_no, plan_id, did, due)
        )
        tid = cur.lastrowid
        for i, name in enumerate(items):
            cur.execute(
                "INSERT INTO maintain_task_item(task_id, item_name, sort_no) VALUES(%s,%s,%s)",
                (tid, name, i + 1)
            )
    print(f'seed plan + {len(devices)} tasks')
else:
    print('= maintain_plan already has data')

conn.commit()
cur.close()
conn.close()
print('migrate_v5_maintain done')
