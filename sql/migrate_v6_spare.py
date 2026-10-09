# -*- coding: utf-8 -*-
"""模块⑥：供应商 + 备件库存（可重复执行）"""
import pymysql
from decimal import Decimal

conn = pymysql.connect(host='localhost', user='root', password='123456',
                       database='cold_storage_oms', charset='utf8mb4')
cur = conn.cursor()

cur.execute("""
CREATE TABLE IF NOT EXISTS supplier (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  contact_name VARCHAR(50) DEFAULT NULL,
  phone VARCHAR(20) DEFAULT NULL,
  address VARCHAR(200) DEFAULT NULL,
  supply_scope VARCHAR(200) DEFAULT NULL COMMENT '供应范围',
  status VARCHAR(20) NOT NULL DEFAULT 'COOPERATING' COMMENT 'COOPERATING/STOPPED',
  remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ supplier')

cur.execute("""
CREATE TABLE IF NOT EXISTS spare_part (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  part_no VARCHAR(40) NOT NULL UNIQUE,
  part_name VARCHAR(100) NOT NULL,
  part_type VARCHAR(50) DEFAULT NULL,
  spec VARCHAR(100) DEFAULT NULL,
  brand VARCHAR(50) DEFAULT NULL,
  unit VARCHAR(20) DEFAULT '个',
  stock_qty INT NOT NULL DEFAULT 0,
  safety_stock INT NOT NULL DEFAULT 0,
  supplier_id BIGINT DEFAULT NULL,
  unit_price DECIMAL(10,2) DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sp_supplier (supplier_id),
  INDEX idx_sp_type (part_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ spare_part')

cur.execute("""
CREATE TABLE IF NOT EXISTS stock_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_no VARCHAR(40) NOT NULL UNIQUE,
  part_id BIGINT NOT NULL,
  change_type VARCHAR(20) NOT NULL COMMENT 'IN/OUT/ADJUST/CHECK',
  change_qty INT NOT NULL COMMENT '变动数量，出库为负或用绝对值+类型',
  before_qty INT NOT NULL,
  after_qty INT NOT NULL,
  biz_type VARCHAR(40) DEFAULT NULL COMMENT 'MANUAL/WORK_ORDER/MAINTAIN',
  biz_id BIGINT DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  operator_id BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sr_part (part_id),
  INDEX idx_sr_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ stock_record')

cur.execute("""
CREATE TABLE IF NOT EXISTS work_order_part (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  part_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_wop_order (order_id),
  INDEX idx_wop_part (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
""")
print('+ work_order_part')

cur.execute("SELECT COUNT(1) FROM supplier")
if cur.fetchone()[0] == 0:
    cur.execute(
        "INSERT INTO supplier(name, contact_name, phone, address, supply_scope, status, remark) VALUES "
        "('桂冷机电配件', '王经理', '13900001111', '南宁市西乡塘区', '压缩机配件/传感器', 'COOPERATING', '主供应商'),"
        "('港城制冷耗材', '赵工', '13900002222', '钦州市钦南区', '风机/电热/滤芯', 'COOPERATING', NULL)"
    )
    print('seed suppliers: 2')

cur.execute("SELECT COUNT(1) FROM spare_part")
if cur.fetchone()[0] == 0:
    cur.execute("SELECT id FROM supplier ORDER BY id LIMIT 2")
    sids = [r[0] for r in cur.fetchall()]
    s1 = sids[0] if sids else None
    s2 = sids[1] if len(sids) > 1 else s1
    parts = [
        ('SP001', '压缩机润滑油', '油品', '5L', 'York', '桶', 20, 5, s1, Decimal('280.00')),
        ('SP002', '温度传感器探头', '传感器', 'PT100', 'Siemens', '个', 15, 5, s1, Decimal('65.00')),
        ('SP003', '冷凝风机轴承', '风机配件', '6204', 'SKF', '套', 8, 3, s2, Decimal('45.00')),
        ('SP004', '排水电热管', '电热', '800W', '国产', '根', 6, 2, s2, Decimal('120.00')),
        ('SP005', '干燥过滤器', '管路', '3/8', 'Danfoss', '个', 4, 5, s1, Decimal('88.00')),
    ]
    for p in parts:
        cur.execute(
            "INSERT INTO spare_part(part_no, part_name, part_type, spec, brand, unit, stock_qty, safety_stock, supplier_id, unit_price) "
            "VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)", p
        )
    print('seed spare_part: 5 (含低库存干燥过滤器)')
else:
    print('= spare_part already has data')

conn.commit()
cur.close()
conn.close()
print('migrate_v6_spare done')
