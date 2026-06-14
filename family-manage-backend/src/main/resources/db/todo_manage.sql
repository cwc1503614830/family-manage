-- 代办管理模块：表结构 + 菜单 + 角色权限

USE cc_test;

CREATE TABLE IF NOT EXISTS todo_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  item_title VARCHAR(200) NOT NULL COMMENT '事项',
  due_date DATE DEFAULT NULL COMMENT '截止日期',
  is_done TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：1是，0否',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_due_date (due_date),
  KEY idx_is_done (is_done)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代办清单';

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('代办管理', 'todo:manage', '✓', '/todo', 'TodoLayout', '', 43, 1);

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('代办清单', 'todo:item', '☰', '/todo/items', 'TodoItem', '', 44, 1);

UPDATE sys_menu SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE code = 'todo:manage') t)
WHERE code = 'todo:item';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m
ON m.code IN ('todo:manage', 'todo:item')
WHERE r.code IN ('admin', 'member');
