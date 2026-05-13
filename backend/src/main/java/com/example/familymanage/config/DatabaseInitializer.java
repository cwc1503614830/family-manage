package com.example.familymanage.config;

import com.example.familymanage.security.PasswordUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        initAuthTables();
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS family_asset_item ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "name VARCHAR(80) NOT NULL COMMENT '资产名称',"
                + "category VARCHAR(40) NOT NULL COMMENT '分类',"
                + "location VARCHAR(80) DEFAULT NULL COMMENT '存放位置',"
                + "quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '数量',"
                + "unit VARCHAR(20) NOT NULL DEFAULT '件' COMMENT '单位',"
                + "warning_quantity DECIMAL(12,2) NOT NULL DEFAULT 1 COMMENT '低库存阈值',"
                + "purchase_date DATE DEFAULT NULL COMMENT '购入日期',"
                + "expire_date DATE DEFAULT NULL COMMENT '到期日期',"
                + "image_url VARCHAR(500) DEFAULT NULL COMMENT '图片地址',"
                + "tags VARCHAR(200) DEFAULT NULL COMMENT '标签',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "KEY idx_category (category),"
                + "KEY idx_location (location),"
                + "KEY idx_expire_date (expire_date)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭资产物品'");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM family_asset_item", Integer.class);
        if (count != null && count == 0) {
            seedDemoData();
        }
        seedAuthData();
    }

    private void initAuthTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_user ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "username VARCHAR(60) NOT NULL COMMENT '账号',"
                + "password_hash VARCHAR(128) NOT NULL COMMENT '密码摘要',"
                + "display_name VARCHAR(80) NOT NULL COMMENT '显示名称',"
                + "status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',"
                + "last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_username (username)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_role ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "code VARCHAR(60) NOT NULL COMMENT '角色编码',"
                + "name VARCHAR(80) NOT NULL COMMENT '角色名称',"
                + "remark VARCHAR(300) DEFAULT NULL COMMENT '备注',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_code (code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_menu ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "name VARCHAR(80) NOT NULL COMMENT '菜单名称',"
                + "code VARCHAR(80) NOT NULL COMMENT '权限编码',"
                + "icon VARCHAR(20) DEFAULT NULL COMMENT '图标',"
                + "sort_no INT NOT NULL DEFAULT 100 COMMENT '排序',"
                + "visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否菜单可见',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_code (code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_user_role ("
                + "user_id BIGINT NOT NULL COMMENT '用户ID',"
                + "role_id BIGINT NOT NULL COMMENT '角色ID',"
                + "PRIMARY KEY (user_id, role_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_role_menu ("
                + "role_id BIGINT NOT NULL COMMENT '角色ID',"
                + "menu_id BIGINT NOT NULL COMMENT '菜单ID',"
                + "PRIMARY KEY (role_id, menu_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单权限关系'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_auth_token ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "token VARCHAR(80) NOT NULL COMMENT '登录令牌',"
                + "user_id BIGINT NOT NULL COMMENT '用户ID',"
                + "expire_at DATETIME NOT NULL COMMENT '过期时间',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_token (token),"
                + "KEY idx_user_id (user_id),"
                + "KEY idx_expire_at (expire_at)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录令牌'");
    }

    private void seedDemoData() {
        jdbcTemplate.update("INSERT INTO family_asset_item "
                + "(name, category, location, quantity, unit, warning_quantity, purchase_date, expire_date, image_url, tags, remark) "
                + "VALUES (?, ?, ?, ?, ?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), ?, ?, ?)",
                "全脂鲜牛奶", "生鲜食品", "冰箱", 3, "盒", 2,
                "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=900&q=80",
                "早餐,冷藏", "儿童早餐常备");
        jdbcTemplate.update("INSERT INTO family_asset_item "
                + "(name, category, location, quantity, unit, warning_quantity, purchase_date, expire_date, image_url, tags, remark) "
                + "VALUES (?, ?, ?, ?, ?, ?, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 550 DAY), ?, ?, ?)",
                "特级生抽", "调味品", "厨房橱柜", 1, "瓶", 1,
                "https://images.unsplash.com/photo-1472476443507-c7a5948772fc?auto=format&fit=crop&w=900&q=80",
                "厨房,中餐", "开封后注意密封");
        jdbcTemplate.update("INSERT INTO family_asset_item "
                + "(name, category, location, quantity, unit, warning_quantity, purchase_date, expire_date, image_url, tags, remark) "
                + "VALUES (?, ?, ?, ?, ?, ?, DATE_SUB(CURDATE(), INTERVAL 3 DAY), NULL, ?, ?, ?)",
                "卷纸", "个护厨卫", "储物间", 12, "卷", 6,
                "https://images.unsplash.com/photo-1583947581924-a31b1d6bb176?auto=format&fit=crop&w=900&q=80",
                "卫生间,补货", "每周检查余量");
        jdbcTemplate.update("INSERT INTO family_asset_item "
                + "(name, category, location, quantity, unit, warning_quantity, purchase_date, expire_date, image_url, tags, remark) "
                + "VALUES (?, ?, ?, ?, ?, ?, DATE_SUB(CURDATE(), INTERVAL 90 DAY), DATE_ADD(CURDATE(), INTERVAL 232 DAY), ?, ?, ?)",
                "意大利面", "生鲜食品", "储物间", 0, "袋", 2,
                "https://images.unsplash.com/photo-1551462147-ff29053bfc14?auto=format&fit=crop&w=900&q=80",
                "主食,低库存", "已用完，待补货");
    }

    private void seedAuthData() {
        Integer menuCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_menu", Integer.class);
        if (menuCount != null && menuCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_menu (name, code, icon, sort_no, visible) VALUES "
                    + "('全部资产', 'asset:view', '▦', 10, 1),"
                    + "('新增资产', 'asset:create', '＋', 11, 0),"
                    + "('编辑资产', 'asset:update', '✎', 12, 0),"
                    + "('删除资产', 'asset:delete', '×', 13, 0),"
                    + "('用户管理', 'system:user', '◉', 80, 1),"
                    + "('角色管理', 'system:role', '◇', 81, 1),"
                    + "('菜单权限', 'system:menu', '⚙', 82, 1)");
        }
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class);
        if (roleCount != null && roleCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_role (code, name, remark) VALUES "
                    + "('admin', '家庭管理员', '拥有全部系统权限'),"
                    + "('member', '家庭成员', '可查看并维护家庭资产')");
            jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) "
                    + "SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m WHERE r.code = 'admin'");
            jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) "
                    + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('asset:view', 'asset:create', 'asset:update') "
                    + "WHERE r.code = 'member'");
        }
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
        if (userCount != null && userCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_user (username, password_hash, display_name, status) VALUES (?, ?, ?, 1)",
                    "admin", PasswordUtil.hash("admin123"), "家庭管理员");
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) "
                    + "SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.code = 'admin' WHERE u.username = 'admin'");
        }
    }
}
