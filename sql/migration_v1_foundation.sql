-- 模块①：基础域升级（在现有库上增量执行，不删业务数据）
USE cold_storage_oms;

-- 用户状态：1启用 0禁用
ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0禁用' AFTER role;

-- 冷库扩展字段
ALTER TABLE cold_storage
  ADD COLUMN IF NOT EXISTS contact_name VARCHAR(50) DEFAULT NULL COMMENT '联系人' AFTER address,
  ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20) DEFAULT NULL AFTER contact_name,
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED' AFTER contact_phone,
  ADD COLUMN IF NOT EXISTS commission_date DATE DEFAULT NULL COMMENT '投用时间' AFTER status;

-- 冷藏间扩展
ALTER TABLE cold_room
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED' AFTER code,
  ADD COLUMN IF NOT EXISTS temp_min DECIMAL(6,2) DEFAULT NULL COMMENT '温度下限' AFTER status,
  ADD COLUMN IF NOT EXISTS temp_max DECIMAL(6,2) DEFAULT NULL COMMENT '温度上限' AFTER temp_min,
  ADD COLUMN IF NOT EXISTS volume DECIMAL(10,2) DEFAULT NULL COMMENT '容积' AFTER temp_max;

-- 设备扩展
ALTER TABLE device
  ADD COLUMN IF NOT EXISTS brand VARCHAR(50) DEFAULT NULL COMMENT '品牌' AFTER device_type,
  ADD COLUMN IF NOT EXISTS model VARCHAR(50) DEFAULT NULL COMMENT '型号' AFTER brand,
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/FAULT/REPAIRING/ACCEPTING/SCRAPPED' AFTER is_public,
  ADD COLUMN IF NOT EXISTS rated_params VARCHAR(255) DEFAULT NULL COMMENT '额定参数' AFTER owner_ops_id,
  ADD COLUMN IF NOT EXISTS fault_count INT NOT NULL DEFAULT 0 COMMENT '累计故障次数' AFTER rated_params;

-- 操作日志
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 回填演示数据
UPDATE cold_storage SET contact_name='王主管', contact_phone='0777-8888888', status='ENABLED', commission_date='2022-06-01' WHERE id=1;

UPDATE cold_room SET status='ENABLED', temp_min=-25, temp_max=-18, volume=500 WHERE id IN (1,2,3,4);
UPDATE cold_room SET status='ENABLED', temp_min=NULL, temp_max=NULL, volume=NULL, name='机房公共区' WHERE id=5;

UPDATE device SET brand='约克', model='YCAI', status='NORMAL' WHERE device_type='约克主机';
UPDATE device SET brand='比泽尔', model='CSH', status='NORMAL' WHERE device_type='比泽尔主机';
UPDATE device SET brand='复叠', model='CAS-01', status='NORMAL' WHERE device_type='复叠活塞机';
UPDATE device SET brand='通用', model='EV-A', status='NORMAL', rated_params='制冷量按库温配置' WHERE device_type LIKE '%蒸发器%';
UPDATE device SET brand='通用', model='DF-A', status='NORMAL' WHERE device_type LIKE '%融霜%';
UPDATE device SET brand='通用', model='HT-A', status='NORMAL' WHERE device_type LIKE '%电热%';
UPDATE device SET brand='通用', model='SN-A', status='NORMAL' WHERE device_type LIKE '%传感器%';

UPDATE sys_user SET status=1 WHERE status IS NULL OR status=0 AND username IN ('admin','ops01','client01');
UPDATE sys_user SET status=1;
