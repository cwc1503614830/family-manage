-- 厨房管理模块：表结构 + 菜单 + 角色权限
-- 若 id 53/54/55/56 已占用，请改为未使用的 id

USE cc_test;

-- ========== 表结构 ==========

CREATE TABLE IF NOT EXISTS kitchen_dish (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  dish_name VARCHAR(120) NOT NULL COMMENT '菜名',
  ingredients TEXT COMMENT '配料',
  image_url VARCHAR(500) DEFAULT '' COMMENT '图片地址',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dish_name (dish_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品清单';

CREATE TABLE IF NOT EXISTS kitchen_cook_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  dish_name VARCHAR(120) NOT NULL COMMENT '菜名',
  cook_date DATE NOT NULL COMMENT '制作日期',
  is_done TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：1是，0否',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  ordered_by_user_id BIGINT DEFAULT NULL COMMENT '下单用户',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_cook_date (cook_date),
  KEY idx_is_done (is_done),
  KEY idx_ordered_by_user_id (ordered_by_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制作记录';

CREATE TABLE IF NOT EXISTS kitchen_ingredient (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  ingredient_name VARCHAR(120) NOT NULL COMMENT '食材名称',
  production_date DATE DEFAULT NULL COMMENT '生产日期',
  shelf_life_months INT DEFAULT NULL COMMENT '保质期(月)',
  expire_date DATE DEFAULT NULL COMMENT '过期日期',
  image_url VARCHAR(500) DEFAULT '' COMMENT '图片地址',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_expire_date (expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材清单';

-- ========== 菜单 ==========

INSERT INTO cc_test.sys_menu
(id, parent_id, name, code, icon, `path`, component, web_url, sort_no, visible, created_at, updated_at)
VALUES(53, NULL, '厨房管理', 'kitchen:manage', '🍳', '/kitchen', 'KitchenLayout', '', 25, 1, NOW(), NOW());

INSERT INTO cc_test.sys_menu
(id, parent_id, name, code, icon, `path`, component, web_url, sort_no, visible, created_at, updated_at)
VALUES(54, 53, '菜品清单', 'kitchen:dish', '☰', '/kitchen/dishes', 'KitchenDish', '', 26, 1, NOW(), NOW());

INSERT INTO cc_test.sys_menu
(id, parent_id, name, code, icon, `path`, component, web_url, sort_no, visible, created_at, updated_at)
VALUES(55, 53, '食材清单', 'kitchen:ingredient', '◈', '/kitchen/ingredients', 'KitchenIngredient', '', 27, 1, NOW(), NOW());

INSERT INTO cc_test.sys_menu
(id, parent_id, name, code, icon, `path`, component, web_url, sort_no, visible, created_at, updated_at)
VALUES(56, 53, '制作记录', 'kitchen:cook', '⌖', '/kitchen/cook-records', 'KitchenCookRecord', '', 28, 1, NOW(), NOW());

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m
ON m.code IN ('kitchen:manage', 'kitchen:dish', 'kitchen:ingredient', 'kitchen:cook')
WHERE r.code IN ('admin', 'member');
