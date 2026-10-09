# -*- coding: utf-8 -*-
"""模块④：故障记录强化 + 案例库（可重复执行）"""
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

add_col('fault_record', 'fault_level',
        "fault_level VARCHAR(20) DEFAULT '一般' COMMENT '一般/严重/紧急' AFTER fault_type")
add_col('fault_record', 'fault_phenomenon',
        "fault_phenomenon VARCHAR(500) DEFAULT NULL COMMENT '故障现象' AFTER fault_desc")
add_col('fault_record', 'fault_reason',
        "fault_reason VARCHAR(500) DEFAULT NULL COMMENT '故障原因' AFTER fault_phenomenon")
add_col('fault_record', 'start_time',
        "start_time DATETIME DEFAULT NULL COMMENT '开始时间' AFTER solution")
add_col('fault_record', 'end_time',
        "end_time DATETIME DEFAULT NULL COMMENT '结束时间' AFTER start_time")
add_col('fault_record', 'parts_used',
        "parts_used VARCHAR(500) DEFAULT NULL COMMENT '使用备件说明' AFTER duration_minutes")
add_col('fault_record', 'fault_images',
        "fault_images VARCHAR(1000) DEFAULT NULL COMMENT '故障图片' AFTER parts_used")
add_col('fault_record', 'remark',
        "remark VARCHAR(500) DEFAULT NULL COMMENT '备注' AFTER fault_images")

cur.execute("""
CREATE TABLE IF NOT EXISTS fault_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL COMMENT '案例标题',
  fault_phenomenon VARCHAR(500) DEFAULT NULL COMMENT '故障现象',
  fault_type VARCHAR(50) NOT NULL COMMENT '故障类型',
  fault_reason VARCHAR(500) DEFAULT NULL COMMENT '故障原因',
  check_method VARCHAR(1000) DEFAULT NULL COMMENT '排查方法',
  solution VARCHAR(1000) DEFAULT NULL COMMENT '处理方案',
  device_type VARCHAR(50) DEFAULT NULL COMMENT '适用设备类型',
  notice VARCHAR(500) DEFAULT NULL COMMENT '注意事项',
  attachment VARCHAR(1000) DEFAULT NULL COMMENT '附件路径',
  creator_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_fc_type (fault_type),
  INDEX idx_fc_device_type (device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故障案例库'
""")
print('+ fault_case ready')

cur.execute("SELECT COUNT(1) FROM fault_case")
if cur.fetchone()[0] == 0:
    cases = [
        ('高压异常排查案例', '排气高压持续报警，机组频繁保护停机', '高压异常',
         '冷凝器结垢/风机故障导致冷凝压力过高', '检查冷凝风机电流与转速；检查冷凝器翅片是否脏堵；核对高低压表读数',
         '清洗冷凝器翅片，更换故障风机轴承，恢复后观察高压稳定', '约克主机', '清洗时断电挂牌；恢复后观察30分钟', None),
        ('融霜排水异常案例', '融霜结束后盘管仍结霜，库温回升慢', '融霜异常',
         '排水电热失效，融霜水再次结冰堵塞', '检查排水电热阻值；观察融霜排水是否通畅；核对融霜时间参数',
         '更换排水电热管，疏通排水管路，适当延长融霜时间', '冷藏间蒸发器/风机', '注意库温回升幅度，避免货品化冻', None),
        ('温度传感器漂移案例', '显示温度与实测偏差大，压缩机启停紊乱', '温度传感器故障',
         '传感器探头老化或安装位置不当', '用标准温度计对比；检查接线电阻；检查探头安装位置',
         '更换同型号温度传感器，按规范重新固定探头', '传感器/保护类', '更换后重新标定控制参数', None),
    ]
    for c in cases:
        cur.execute(
            "INSERT INTO fault_case(title, fault_phenomenon, fault_type, fault_reason, check_method, solution, device_type, notice, attachment, creator_id) "
            "VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,1)", c)
    print('seed fault_case:', len(cases))
else:
    print('= fault_case already has data')

# 回填现象字段
cur.execute("""
UPDATE fault_record SET fault_phenomenon = fault_desc
WHERE (fault_phenomenon IS NULL OR fault_phenomenon='') AND fault_desc IS NOT NULL
""")
print('backfill phenomenon:', cur.rowcount)

conn.commit()
cur.close()
conn.close()
print('migrate_v4_fault_case done')
