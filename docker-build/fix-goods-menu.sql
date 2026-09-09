-- ============================================================
-- 生产环境数据修复 SQL
-- 修复：cc_test 库缺少 goods/kitchen/travel/album/todo 模块的菜单和权限
-- 执行环境：MySQL 服务器上的 cc_test 数据库
-- 执行方式：mysql -u root -p cc_test < fix-goods-menu.sql
-- 注意：所有语句均为幂等的（INSERT IGNORE / UPDATE ... WHERE）
-- ============================================================

-- 1. 确保所有模块菜单存在
INSERT IGNORE INTO sys_menu (id, parent_id, name, code, type, icon, sort_no, path, component, is_visible, is_system, remark) VALUES
(13, 0, '物品管理', 'goods', 'DIRECTORY', '📦', 8, '/goods', NULL, 1, 1, '物品管理模块'),
(14, 13, '物品明细', 'goods:item', 'MENU', '🎒', 1, '/goods/items', 'GoodsItemView', 1, 1, '物品明细列表'),

(15, 0, '菜品管理', 'kitchen', 'DIRECTORY', '🍳', 9, '/kitchen', NULL, 1, 1, '菜品管理模块'),
(16, 15, '菜品清单', 'kitchen:recipe', 'MENU', '🍽️', 1, '/kitchen/recipes', 'RecipeView', 1, 1, '菜品清单'),
(17, 15, '食材清单', 'kitchen:ingredient', 'MENU', '🛒', 2, '/kitchen/ingredients', 'IngredientView', 1, 1, '食材清单'),

(18, 0, '出行管理', 'travel', 'DIRECTORY', '✈️', 10, '/travel', NULL, 1, 1, '出行管理模块'),
(19, 18, '出行清单', 'travel:plan', 'MENU', '🧳', 1, '/travel/plans', 'TravelPlanView', 1, 1, '出行清单'),

(20, 0, '相册管理', 'album', 'DIRECTORY', '📸', 11, '/album', NULL, 1, 1, '相册管理模块'),
(21, 20, '相册列表', 'album:list', 'MENU', '🖼️', 1, '/album/list', 'AlbumView', 1, 1, '相册列表'),

(22, 0, '待办管理', 'todo', 'DIRECTORY', '✅', 12, '/todo', NULL, 1, 1, '待办管理模块'),
(23, 22, '待办清单', 'todo:task', 'MENU', '📝', 1, '/todo/tasks', 'TodoTaskView', 1, 1, '待办清单');

-- 2. 更新各子菜单的 parent_id（确保正确的层级关系）
UPDATE sys_menu SET parent_id = 13 WHERE code = 'goods:item';
UPDATE sys_menu SET parent_id = 15 WHERE code IN ('kitchen:recipe', 'kitchen:ingredient');
UPDATE sys_menu SET parent_id = 18 WHERE code = 'travel:plan';
UPDATE sys_menu SET parent_id = 20 WHERE code = 'album:list';
UPDATE sys_menu SET parent_id = 22 WHERE code = 'todo:task';

-- 3. 确保管理员拥有所有模块的菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.code = 'admin' AND m.code IN (
    'goods', 'goods:item',
    'kitchen', 'kitchen:recipe', 'kitchen:ingredient',
    'travel', 'travel:plan',
    'album', 'album:list',
    'todo', 'todo:task'
);

-- 4. 确保普通成员拥有所有模块的菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.code = 'member' AND m.code IN (
    'goods', 'goods:item',
    'kitchen', 'kitchen:recipe', 'kitchen:ingredient',
    'travel', 'travel:plan',
    'album', 'album:list',
    'todo', 'todo:task'
);
