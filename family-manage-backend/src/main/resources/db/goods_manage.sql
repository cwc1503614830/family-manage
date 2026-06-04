-- 物品管理模块：表结构 + 菜单 + 角色权限

USE cc_test;

-- ========== 表结构 ==========

CREATE TABLE IF NOT EXISTS goods_location (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',
  area_name VARCHAR(120) NOT NULL COMMENT '所在区域名称',
  remark VARCHAR(300) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_area_code (area_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品存放位置';

CREATE TABLE IF NOT EXISTS goods_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  item_name VARCHAR(120) NOT NULL COMMENT '物品名称',
  area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',
  purchase_time DATE DEFAULT NULL COMMENT '买入时间',
  expire_time DATE DEFAULT NULL COMMENT '过期时间',
  image_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图片地址',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_by VARCHAR(60) DEFAULT NULL COMMENT '创建人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_by VARCHAR(60) DEFAULT NULL COMMENT '更新人',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_name (item_name),
  KEY idx_area_code (area_code),
  KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品明细';

-- ========== 菜单 ==========

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('物品管理', 'goods:manage', '📦', '/goods', 'GoodsLayout', '', 38, 1);

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('物品明细', 'goods:item', '☰', '/goods/items', 'GoodsItem', '', 39, 1);

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('存放位置', 'goods:location', '⌖', '/goods/locations', 'GoodsLocation', '', 40, 1);

UPDATE sys_menu SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE code = 'goods:manage') t)
WHERE code IN ('goods:item', 'goods:location');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m
ON m.code IN ('goods:manage', 'goods:item', 'goods:location')
WHERE r.code IN ('admin', 'member');
