-- 相册管理模块：表结构 + 菜单 + 角色权限

USE cc_test;

-- ========== 表结构 ==========

CREATE TABLE IF NOT EXISTS album_theme (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  theme_name VARCHAR(120) NOT NULL COMMENT '主题',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_theme_name (theme_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册主题';

CREATE TABLE IF NOT EXISTS album_photo (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  image_name VARCHAR(120) NOT NULL COMMENT '图片名称',
  image_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图片地址',
  theme_id BIGINT NOT NULL COMMENT '主题ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_theme_id (theme_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册图片';

-- ========== 菜单 ==========

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('相册管理', 'album:manage', '📷', '/album', 'AlbumLayout', '', 35, 1);

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('相册', 'album:photo', '☰', '/album/photos', 'AlbumPhoto', '', 36, 1);

INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible)
VALUES ('相册主题', 'album:theme', '◈', '/album/themes', 'AlbumTheme', '', 37, 1);

UPDATE sys_menu SET parent_id = (SELECT id FROM (SELECT id FROM sys_menu WHERE code = 'album:manage') t)
WHERE code IN ('album:photo', 'album:theme');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m
ON m.code IN ('album:manage', 'album:photo', 'album:theme')
WHERE r.code IN ('admin', 'member');
