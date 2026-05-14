CREATE DATABASE IF NOT EXISTS cc_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE cc_test;

CREATE TABLE IF NOT EXISTS family_asset_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(80) NOT NULL COMMENT '资产名称',
  category VARCHAR(40) NOT NULL COMMENT '分类',
  location VARCHAR(80) DEFAULT NULL COMMENT '存放位置',
  quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '数量',
  unit VARCHAR(20) NOT NULL DEFAULT '件' COMMENT '单位',
  warning_quantity DECIMAL(12,2) NOT NULL DEFAULT 1 COMMENT '低库存阈值',
  purchase_date DATE DEFAULT NULL COMMENT '购入日期',
  expire_date DATE DEFAULT NULL COMMENT '到期日期',
  image_url VARCHAR(500) DEFAULT NULL COMMENT '图片地址',
  tags VARCHAR(200) DEFAULT NULL COMMENT '标签',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_category (category),
  KEY idx_location (location),
  KEY idx_expire_date (expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭资产物品';

INSERT INTO family_asset_item
(name, category, location, quantity, unit, warning_quantity, purchase_date, expire_date, image_url, tags, remark)
VALUES
('全脂鲜牛奶', '生鲜食品', '冰箱', 3, '盒', 2, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=900&q=80', '早餐,冷藏', '儿童早餐常备'),
('特级生抽', '调味品', '厨房橱柜', 1, '瓶', 1, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 550 DAY), 'https://images.unsplash.com/photo-1472476443507-c7a5948772fc?auto=format&fit=crop&w=900&q=80', '厨房,中餐', '开封后注意密封'),
('卷纸', '个护厨卫', '储物间', 12, '卷', 6, DATE_SUB(CURDATE(), INTERVAL 3 DAY), NULL, 'https://images.unsplash.com/photo-1583947581924-a31b1d6bb176?auto=format&fit=crop&w=900&q=80', '卫生间,补货', '每周检查余量'),
('意大利面', '生鲜食品', '储物间', 0, '袋', 2, DATE_SUB(CURDATE(), INTERVAL 90 DAY), DATE_ADD(CURDATE(), INTERVAL 232 DAY), 'https://images.unsplash.com/photo-1551462147-ff29053bfc14?auto=format&fit=crop&w=900&q=80', '主食,低库存', '已用完，待补货');

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(60) NOT NULL COMMENT '账号',
  password_hash VARCHAR(128) NOT NULL COMMENT '密码摘要',
  display_name VARCHAR(80) NOT NULL COMMENT '显示名称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  code VARCHAR(60) NOT NULL COMMENT '角色编码',
  name VARCHAR(80) NOT NULL COMMENT '角色名称',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id BIGINT DEFAULT NULL COMMENT '父级菜单ID',
  name VARCHAR(80) NOT NULL COMMENT '菜单名称',
  code VARCHAR(80) NOT NULL COMMENT '权限编码',
  icon VARCHAR(20) DEFAULT NULL COMMENT '图标',
  path VARCHAR(160) DEFAULT NULL COMMENT '菜单路径',
  component VARCHAR(160) DEFAULT NULL COMMENT '菜单组件',
  web_url VARCHAR(300) DEFAULT NULL COMMENT '网页地址',
  sort_no INT NOT NULL DEFAULT 100 COMMENT '排序',
  visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否菜单可见',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单权限关系';

CREATE TABLE IF NOT EXISTS sys_auth_token (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  token VARCHAR(80) NOT NULL COMMENT '登录令牌',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  expire_at DATETIME NOT NULL COMMENT '过期时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_token (token),
  KEY idx_user_id (user_id),
  KEY idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录令牌';

CREATE TABLE IF NOT EXISTS clothes_type (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  type_code VARCHAR(60) NOT NULL COMMENT '衣服类型编码',
  type_name VARCHAR(80) NOT NULL COMMENT '衣服类型名称',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣服类型';

CREATE TABLE IF NOT EXISTS clothes_location (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',
  area_name VARCHAR(120) NOT NULL COMMENT '所在区域名称',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_area_code (area_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣服存放位置';

CREATE TABLE IF NOT EXISTS clothes_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  clothes_name VARCHAR(120) NOT NULL COMMENT '衣服名称',
  type_code VARCHAR(60) NOT NULL COMMENT '衣服类型编码',
  area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',
  purchase_time DATE DEFAULT NULL COMMENT '买入时间',
  expire_time DATE DEFAULT NULL COMMENT '过期时间',
  image_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图片地址',
  owner_user_id BIGINT NOT NULL COMMENT '拥有者',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_by VARCHAR(60) DEFAULT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by VARCHAR(60) DEFAULT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_clothes_name (clothes_name),
  KEY idx_type_code (type_code),
  KEY idx_area_code (area_code),
  KEY idx_owner_user_id (owner_user_id),
  KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣服明细';

INSERT IGNORE INTO clothes_type (type_code, type_name, remark) VALUES
('XiaKu', '夏裤', '默认示例类型'),
('DuanXiu', '短袖', '默认示例类型');

INSERT IGNORE INTO clothes_location (area_code, area_name, remark) VALUES
('BOX1-ZWYG', '1号箱子-主卧衣柜', '需求示例位置'),
('BOX3-ZWYG', '3号箱子-主卧衣柜', '需求示例位置'),
('ZWYG', '主卧衣柜', '需求示例位置'),
('CWYG', '次卧衣柜', '需求示例位置'),
('BOX3-CWYG', '3号箱子-次卧衣柜', '需求示例位置');

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible) VALUES
('全部资产', 'asset:view', '▦', '/assets', 'AssetConsole', '', 10, 1),
('衣服管理', 'clothes:manage', '◇', '/clothes', 'ClothesLayout', '', 20, 1),
('衣服明细', 'clothes:item', '☰', '/clothes/items', 'ClothesItem', '', 21, 1),
('衣服类型', 'clothes:type', '◈', '/clothes/types', 'ClothesType', '', 22, 1),
('存放位置', 'clothes:location', '⌖', '/clothes/locations', 'ClothesLocation', '', 23, 1),
('系统管理', 'system:manage', '⚙', '/system', 'SystemLayout', '', 80, 1),
('用户管理', 'system:user', '◉', '/system/users', 'SystemUser', '', 81, 1),
('角色管理', 'system:role', '◇', '/system/roles', 'SystemRole', '', 82, 1),
('菜单权限', 'system:menu', '☷', '/system/menus', 'SystemMenu', '', 83, 1);

UPDATE sys_menu child
JOIN sys_menu parent ON parent.code = 'system:manage'
SET child.parent_id = parent.id
WHERE child.code IN ('system:user', 'system:role', 'system:menu');

UPDATE sys_menu child
JOIN sys_menu parent ON parent.code = 'clothes:manage'
SET child.parent_id = parent.id
WHERE child.code IN ('clothes:item', 'clothes:type', 'clothes:location');

INSERT IGNORE INTO sys_role (code, name, remark) VALUES
('admin', '家庭管理员', '拥有全部系统权限'),
('member', '家庭成员', '可查看并维护家庭资产');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m WHERE r.code = 'admin';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code = 'asset:view'
WHERE r.code = 'member';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('clothes:manage', 'clothes:item', 'clothes:type', 'clothes:location')
WHERE r.code = 'member';

INSERT IGNORE INTO sys_user (username, password_hash, display_name, status) VALUES
('admin', 'b3f76132ce453df3b27d0fa943ac784174134ba894e2fb8a08a52e40046a1ee3', '家庭管理员', 1);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.code = 'admin' WHERE u.username = 'admin';
