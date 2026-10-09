# -*- coding: utf-8 -*-
"""模块②：工单七状态机字段升级（可重复执行）"""
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

add_col('work_order', 'fault_images',
        "fault_images VARCHAR(1000) DEFAULT NULL COMMENT '报修图片，逗号分隔路径' AFTER fault_desc")
add_col('work_order', 'fault_type',
        "fault_type VARCHAR(50) DEFAULT NULL COMMENT '故障类型' AFTER process_record")
add_col('work_order', 'fault_reason',
        "fault_reason VARCHAR(500) DEFAULT NULL COMMENT '故障原因' AFTER fault_type")
add_col('work_order', 'solution',
        "solution VARCHAR(500) DEFAULT NULL COMMENT '处理方案' AFTER fault_reason")
add_col('work_order', 'duration_minutes',
        "duration_minutes INT DEFAULT NULL COMMENT '维修耗时分钟' AFTER solution")
add_col('work_order', 'repair_images',
        "repair_images VARCHAR(1000) DEFAULT NULL COMMENT '维修图片' AFTER duration_minutes")
add_col('work_order', 'accept_remark',
        "accept_remark VARCHAR(500) DEFAULT NULL COMMENT '验收备注' AFTER evaluate_content")
add_col('work_order', 'close_reason',
        "close_reason VARCHAR(500) DEFAULT NULL COMMENT '关闭原因' AFTER accept_remark")
add_col('work_order', 'accept_time',
        "accept_time DATETIME DEFAULT NULL COMMENT '验收时间' AFTER finish_time")
add_col('work_order', 'evaluate_time',
        "evaluate_time DATETIME DEFAULT NULL COMMENT '评价时间' AFTER accept_time")
add_col('work_order', 'archive_time',
        "archive_time DATETIME DEFAULT NULL COMMENT '归档时间' AFTER evaluate_time")

# 旧数据：已指派且处理中的保持；已完成保持；注释状态口径
cur.execute("""
    UPDATE work_order SET status='ASSIGNED'
    WHERE status='PROCESSING' AND assignee_id IS NOT NULL
      AND (process_record IS NULL OR process_record='')
""")
print('normalized ASSIGNED from empty PROCESSING:', cur.rowcount)

conn.commit()
cur.close()
conn.close()
print('migrate_v2_workorder done')
