CREATE DATABASE IF NOT EXISTS cold_storage_oms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE cold_storage_oms;

DROP TABLE IF EXISTS fault_record;
DROP TABLE IF EXISTS work_order;
DROP TABLE IF EXISTS user_cold_room;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS cold_room;
DROP TABLE IF EXISTS cold_storage;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) DEFAULT NULL,
  phone VARCHAR(20) DEFAULT NULL,
  role VARCHAR(20) NOT NULL COMMENT 'ADMIN/OPS/CLIENT',
  weight DECIMAL(6,2) DEFAULT NULL COMMENT '预留字段',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cold_storage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  address VARCHAR(200) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cold_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  storage_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(50) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_room_storage (storage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_cold_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_room (user_id, room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_no VARCHAR(50) NOT NULL UNIQUE,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(50) NOT NULL,
  room_id BIGINT NOT NULL,
  is_public TINYINT NOT NULL DEFAULT 0 COMMENT '1=公共主机等',
  install_date DATE DEFAULT NULL,
  maintain_cycle_days INT DEFAULT 90,
  owner_ops_id BIGINT DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_device_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE work_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL UNIQUE,
  device_id BIGINT NOT NULL,
  fault_desc VARCHAR(500) NOT NULL,
  fault_images VARCHAR(1000) DEFAULT NULL COMMENT '报修图片',
  status VARCHAR(20) NOT NULL COMMENT 'PENDING/ASSIGNED/PROCESSING/ACCEPTING/DONE/EVALUATED/ARCHIVED',
  submit_user_id BIGINT NOT NULL,
  assignee_id BIGINT DEFAULT NULL,
  process_record VARCHAR(1000) DEFAULT NULL,
  fault_type VARCHAR(50) DEFAULT NULL,
  fault_reason VARCHAR(500) DEFAULT NULL,
  solution VARCHAR(500) DEFAULT NULL,
  duration_minutes INT DEFAULT NULL,
  repair_images VARCHAR(1000) DEFAULT NULL,
  satisfaction INT DEFAULT NULL,
  evaluate_content VARCHAR(500) DEFAULT NULL,
  accept_remark VARCHAR(500) DEFAULT NULL,
  close_reason VARCHAR(500) DEFAULT NULL,
  assign_time DATETIME DEFAULT NULL,
  finish_time DATETIME DEFAULT NULL,
  accept_time DATETIME DEFAULT NULL,
  evaluate_time DATETIME DEFAULT NULL,
  archive_time DATETIME DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_wo_status (status),
  INDEX idx_wo_submit (submit_user_id),
  INDEX idx_wo_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fault_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_no VARCHAR(40) NOT NULL UNIQUE,
  device_id BIGINT NOT NULL,
  order_id BIGINT DEFAULT NULL,
  fault_type VARCHAR(50) NOT NULL,
  fault_desc VARCHAR(500) DEFAULT NULL,
  solution VARCHAR(500) DEFAULT NULL,
  duration_minutes INT DEFAULT NULL,
  handler_id BIGINT DEFAULT NULL,
  handle_time DATETIME DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- password = 123456 (BCrypt)
INSERT INTO sys_user (id, username, password, real_name, phone, role) VALUES
(1, 'admin', '$2a$10$N1NFw9790JV.zkSCXOGgUevH62AQ53JLMHTHKCjbhlX/Lg12RIxE6', '系统管理员', '13800000001', 'ADMIN'),
(2, 'ops01', '$2a$10$N1NFw9790JV.zkSCXOGgUevH62AQ53JLMHTHKCjbhlX/Lg12RIxE6', '运维-张工', '13800000002', 'OPS'),
(3, 'client01', '$2a$10$N1NFw9790JV.zkSCXOGgUevH62AQ53JLMHTHKCjbhlX/Lg12RIxE6', '甲方-李经理', '13800000003', 'CLIENT');

INSERT INTO cold_storage (id, name, address, remark) VALUES
(1, '广西龙门港冷库', '广西钦州龙门港区域', '演示用整座冷库');

INSERT INTO cold_room (id, storage_id, name, code, remark) VALUES
(1, 1, '冷藏间1', 'R01', NULL),
(2, 1, '冷藏间2', 'R02', NULL),
(3, 1, '冷藏间3', 'R03', NULL),
(4, 1, '冷藏间4', 'R04', NULL),
(5, 1, '机房公共区', 'PUB', '公共主机与公共设备');

-- client01 租用冷藏间1、2
INSERT INTO user_cold_room (user_id, room_id) VALUES (3, 1), (3, 2);

INSERT INTO device (device_no, device_name, device_type, room_id, is_public, install_date, maintain_cycle_days, owner_ops_id, remark) VALUES
('YK-01', '约克主机1', '约克主机', 5, 1, '2023-01-15', 90, 2, '公共主机'),
('YK-02', '约克主机2', '约克主机', 5, 1, '2023-01-15', 90, 2, '公共主机'),
('BZ-01', '比泽尔主机', '比泽尔主机', 5, 1, '2023-03-01', 90, 2, '公共主机'),
('FD-01', '复叠活塞机', '复叠活塞机', 5, 1, '2023-06-01', 120, 2, '公共主机'),
('EV-101', '冷藏间1蒸发器', '冷藏间蒸发器/风机', 1, 0, '2023-02-01', 60, 2, NULL),
('DF-101', '冷藏间1融霜设备', '融霜相关设备', 1, 0, '2023-02-01', 60, 2, NULL),
('EV-201', '冷藏间2蒸发器', '冷藏间蒸发器/风机', 2, 0, '2023-02-10', 60, 2, NULL),
('HT-201', '冷藏间2排水电热', '排水电热', 2, 0, '2023-02-10', 90, 2, NULL),
('EV-301', '冷藏间3蒸发器', '冷藏间蒸发器/风机', 3, 0, '2023-04-01', 60, 2, NULL),
('SN-301', '冷藏间3温度传感器', '传感器/保护类', 3, 0, '2023-04-01', 180, 2, NULL),
('EV-401', '冷藏间4蒸发器', '冷藏间蒸发器/风机', 4, 0, '2023-05-01', 60, 2, NULL);

INSERT INTO work_order (order_no, device_id, fault_desc, status, submit_user_id, assignee_id, process_record, satisfaction, evaluate_content, assign_time, finish_time, create_time) VALUES
('WO20260923001', 5, '冷藏间1温度偏高，蒸发器结霜明显', 'PENDING', 3, NULL, NULL, NULL, NULL, NULL, NULL, '2026-09-22 09:30:00'),
('WO20260923002', 7, '冷藏间2风机异响', 'PROCESSING', 3, 2, '已到场检查轴承', NULL, NULL, '2026-09-22 14:00:00', NULL, '2026-09-21 10:00:00'),
('WO20260922001', 6, '融霜不彻底', 'DONE', 3, 2, '更换融霜计时参数并清理排水', 5, '处理及时', '2026-09-20 11:00:00', '2026-09-20 16:30:00', '2026-09-20 09:00:00');
