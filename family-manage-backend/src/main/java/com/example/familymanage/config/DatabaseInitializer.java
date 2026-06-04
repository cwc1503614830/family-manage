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
        initClothesTables();
        initWealthTables();
        ensureWealthTypeSortNum();
        initTravelTables();
        initKitchenTables();
        initAlbumTables();
        initGoodsTables();
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
                + "parent_id BIGINT DEFAULT NULL COMMENT '父级菜单ID',"
                + "name VARCHAR(80) NOT NULL COMMENT '菜单名称',"
                + "code VARCHAR(80) NOT NULL COMMENT '权限编码',"
                + "icon VARCHAR(20) DEFAULT NULL COMMENT '图标',"
                + "path VARCHAR(160) DEFAULT NULL COMMENT '菜单路径',"
                + "component VARCHAR(160) DEFAULT NULL COMMENT '菜单组件',"
                + "web_url VARCHAR(300) DEFAULT NULL COMMENT '网页地址',"
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
        ensureMenuColumns();
    }

    private void ensureMenuColumns() {
        addColumnIfMissing("sys_menu", "parent_id", "ALTER TABLE sys_menu ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父级菜单ID' AFTER id");
        addColumnIfMissing("sys_menu", "path", "ALTER TABLE sys_menu ADD COLUMN path VARCHAR(160) DEFAULT NULL COMMENT '菜单路径' AFTER icon");
        addColumnIfMissing("sys_menu", "component", "ALTER TABLE sys_menu ADD COLUMN component VARCHAR(160) DEFAULT NULL COMMENT '菜单组件' AFTER path");
        addColumnIfMissing("sys_menu", "web_url", "ALTER TABLE sys_menu ADD COLUMN web_url VARCHAR(300) DEFAULT NULL COMMENT '网页地址' AFTER component");
    }

    private void addColumnIfMissing(String tableName, String columnName, String sql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(sql);
        }
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

    private void initClothesTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS clothes_type ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "type_code VARCHAR(60) NOT NULL COMMENT '衣服类型编码',"
                + "type_name VARCHAR(80) NOT NULL COMMENT '衣服类型名称',"
                + "remark VARCHAR(300) DEFAULT NULL COMMENT '备注',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_type_code (type_code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣服类型'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS clothes_location ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',"
                + "area_name VARCHAR(120) NOT NULL COMMENT '所在区域名称',"
                + "remark VARCHAR(300) DEFAULT NULL COMMENT '备注',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_area_code (area_code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣服存放位置'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS clothes_item ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "clothes_name VARCHAR(120) NOT NULL COMMENT '衣服名称',"
                + "type_code VARCHAR(60) NOT NULL COMMENT '衣服类型编码',"
                + "area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',"
                + "purchase_time DATE DEFAULT NULL COMMENT '买入时间',"
                + "expire_time DATE DEFAULT NULL COMMENT '过期时间',"
                + "image_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图片地址',"
                + "owner_user_id BIGINT NOT NULL COMMENT '拥有者',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_by VARCHAR(60) DEFAULT NULL COMMENT '创建人',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_by VARCHAR(60) DEFAULT NULL COMMENT '更新人',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_clothes_name (clothes_name),"
                + "KEY idx_type_code (type_code),"
                + "KEY idx_area_code (area_code),"
                + "KEY idx_owner_user_id (owner_user_id),"
                + "KEY idx_expire_time (expire_time)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='衣服明细'");

        Integer typeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clothes_type", Integer.class);
        if (typeCount != null && typeCount == 0) {
            jdbcTemplate.update("INSERT INTO clothes_type (type_code, type_name, remark) VALUES (?, ?, ?)",
                    "XiaKu", "夏裤", "默认示例类型");
            jdbcTemplate.update("INSERT INTO clothes_type (type_code, type_name, remark) VALUES (?, ?, ?)",
                    "DuanXiu", "短袖", "默认示例类型");
        }

        Integer locationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clothes_location", Integer.class);
        if (locationCount != null && locationCount == 0) {
            jdbcTemplate.update("INSERT INTO clothes_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    "BOX1-ZWYG", "1号箱子-主卧衣柜", "需求示例位置");
            jdbcTemplate.update("INSERT INTO clothes_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    "BOX3-ZWYG", "3号箱子-主卧衣柜", "需求示例位置");
            jdbcTemplate.update("INSERT INTO clothes_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    "ZWYG", "主卧衣柜", "需求示例位置");
            jdbcTemplate.update("INSERT INTO clothes_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    "CWYG", "次卧衣柜", "需求示例位置");
            jdbcTemplate.update("INSERT INTO clothes_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    "BOX3-CWYG", "3号箱子-次卧衣柜", "需求示例位置");
        }
    }

    private void initWealthTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS wealth_type ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "asset_type VARCHAR(80) NOT NULL COMMENT '资产类型',"
                + "remark VARCHAR(300) DEFAULT NULL COMMENT '备注',"
                + "sort_num INT DEFAULT NULL COMMENT '排序',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_asset_type (asset_type)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财富资产类型'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS wealth_asset ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "asset_name VARCHAR(120) NOT NULL COMMENT '资产名称',"
                + "type_id BIGINT NOT NULL COMMENT '资产类型ID',"
                + "amount DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '资产金额',"
                + "record_date DATE DEFAULT NULL COMMENT '入账日期',"
                + "owner_user_id BIGINT NOT NULL COMMENT '持有人',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_by VARCHAR(60) DEFAULT NULL COMMENT '创建人',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_by VARCHAR(60) DEFAULT NULL COMMENT '更新人',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_asset_name (asset_name),"
                + "KEY idx_type_id (type_id),"
                + "KEY idx_owner_user_id (owner_user_id),"
                + "KEY idx_record_date (record_date)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财富资产总览'");

        Integer typeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wealth_type", Integer.class);
        if (typeCount != null && typeCount == 0) {
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "支付宝", "默认资产类型", 1);
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "微信", "默认资产类型", 2);
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "现金", "默认资产类型", 3);
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "基金", "默认资产类型", 4);
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "银行卡", "默认资产类型", 5);
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "公积金", "默认资产类型", 6);
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "黄金", "默认资产类型", 7);
            jdbcTemplate.update("INSERT INTO wealth_type (asset_type, remark, sort_num) VALUES (?, ?, ?)", "债权", "默认资产类型", 8);
        }
    }

    private void ensureWealthTypeSortNum() {
        addColumnIfMissing("wealth_type", "sort_num",
                "ALTER TABLE wealth_type ADD COLUMN sort_num INT DEFAULT NULL COMMENT '排序' AFTER remark");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 1 WHERE asset_type = '支付宝' AND sort_num IS NULL");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 2 WHERE asset_type = '微信' AND sort_num IS NULL");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 3 WHERE asset_type = '现金' AND sort_num IS NULL");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 4 WHERE asset_type = '基金' AND sort_num IS NULL");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 5 WHERE asset_type = '银行卡' AND sort_num IS NULL");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 6 WHERE asset_type = '公积金' AND sort_num IS NULL");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 7 WHERE asset_type = '黄金' AND sort_num IS NULL");
        jdbcTemplate.update("UPDATE wealth_type SET sort_num = 8 WHERE asset_type = '债权' AND sort_num IS NULL");
    }

    private void initTravelTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS travel_wuhan_spot ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "spot_name VARCHAR(120) NOT NULL COMMENT '景点名称',"
                + "spot_type VARCHAR(20) NOT NULL DEFAULT '室内' COMMENT '景点类型',"
                + "checked_in TINYINT NOT NULL DEFAULT 0 COMMENT '是否打卡',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='武汉游景点'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS travel_city ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "province VARCHAR(60) NOT NULL COMMENT '省份',"
                + "city VARCHAR(80) NOT NULL COMMENT '城市',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_province_city (province, city)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国内游城市字典'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS travel_domestic ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "city_id BIGINT NOT NULL COMMENT '城市ID',"
                + "province VARCHAR(60) NOT NULL COMMENT '省份',"
                + "city VARCHAR(80) NOT NULL COMMENT '城市',"
                + "cost DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '花费',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国内游记录'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS travel_domestic_note ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "domestic_id BIGINT NOT NULL COMMENT '国内游记录ID',"
                + "title VARCHAR(200) DEFAULT NULL COMMENT '笔记标题',"
                + "content MEDIUMTEXT COMMENT 'Markdown笔记内容',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_domestic_id (domestic_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国内游旅游笔记'");
    }

    private void initKitchenTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS kitchen_dish ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "dish_name VARCHAR(120) NOT NULL COMMENT '菜名',"
                + "ingredients TEXT COMMENT '配料',"
                + "image_url VARCHAR(500) DEFAULT '' COMMENT '图片地址',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_dish_name (dish_name)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品清单'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS kitchen_cook_record ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "dish_name VARCHAR(120) NOT NULL COMMENT '菜名',"
                + "cook_date DATE NOT NULL COMMENT '制作日期',"
                + "is_done TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：1是，0否',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "ordered_by_user_id BIGINT DEFAULT NULL COMMENT '下单用户',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "KEY idx_cook_date (cook_date),"
                + "KEY idx_is_done (is_done),"
                + "KEY idx_ordered_by_user_id (ordered_by_user_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制作记录'");
        addColumnIfMissing("kitchen_cook_record", "ordered_by_user_id",
                "ALTER TABLE kitchen_cook_record ADD COLUMN ordered_by_user_id BIGINT DEFAULT NULL COMMENT '下单用户' AFTER remark");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS kitchen_ingredient ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "ingredient_name VARCHAR(120) NOT NULL COMMENT '食材名称',"
                + "production_date DATE DEFAULT NULL COMMENT '生产日期',"
                + "shelf_life_months INT DEFAULT NULL COMMENT '保质期(月)',"
                + "expire_date DATE DEFAULT NULL COMMENT '过期日期',"
                + "image_url VARCHAR(500) DEFAULT '' COMMENT '图片地址',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "KEY idx_expire_date (expire_date)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材清单'");
    }

    private void initGoodsTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS goods_location ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',"
                + "area_name VARCHAR(120) NOT NULL COMMENT '所在区域名称',"
                + "remark VARCHAR(300) DEFAULT NULL COMMENT '备注',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_area_code (area_code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品存放位置'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS goods_item ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "item_name VARCHAR(120) NOT NULL COMMENT '物品名称',"
                + "area_code VARCHAR(80) NOT NULL COMMENT '所在区域编码',"
                + "purchase_time DATE DEFAULT NULL COMMENT '买入时间',"
                + "expire_time DATE DEFAULT NULL COMMENT '过期时间',"
                + "image_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图片地址',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_by VARCHAR(60) DEFAULT NULL COMMENT '创建人',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_by VARCHAR(60) DEFAULT NULL COMMENT '更新人',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_item_name (item_name),"
                + "KEY idx_area_code (area_code),"
                + "KEY idx_expire_time (expire_time)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品明细'");

        Integer locationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM goods_location", Integer.class);
        if (locationCount != null && locationCount == 0) {
            jdbcTemplate.update("INSERT INTO goods_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    "YT-LEFT1-UP", "阳台柜左1上", "需求示例位置");
            jdbcTemplate.update("INSERT INTO goods_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    "CB-LEFT1-DOWN", "餐边柜左1下", "需求示例位置");
        }
    }

    private void initAlbumTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS album_theme ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "theme_name VARCHAR(120) NOT NULL COMMENT '主题',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_theme_name (theme_name)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册主题'");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS album_photo ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                + "image_name VARCHAR(120) NOT NULL COMMENT '图片名称',"
                + "image_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '图片地址',"
                + "theme_id BIGINT NOT NULL COMMENT '主题ID',"
                + "remark VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                + "PRIMARY KEY (id),"
                + "KEY idx_theme_id (theme_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册图片'");
    }

    private void seedAuthData() {
        jdbcTemplate.update("INSERT IGNORE INTO sys_menu (name, code, icon, path, component, web_url, sort_no, visible) VALUES "
                + "('全部资产', 'asset:view', '▦', '/assets', 'AssetConsole', '', 10, 1),"
                + "('衣服管理', 'clothes:manage', '◇', '/clothes', 'ClothesLayout', '', 20, 1),"
                + "('衣服明细', 'clothes:item', '☰', '/clothes/items', 'ClothesItem', '', 21, 1),"
                + "('衣服类型', 'clothes:type', '◈', '/clothes/types', 'ClothesType', '', 22, 1),"
                + "('存放位置', 'clothes:location', '⌖', '/clothes/locations', 'ClothesLocation', '', 23, 1),"
                + "('财富管理', 'wealth:manage', '￥', '/wealth', 'WealthLayout', '', 30, 1),"
                + "('资产总览', 'wealth:overview', '☰', '/wealth/assets', 'WealthOverview', '', 31, 1),"
                + "('资产类型', 'wealth:type', '◈', '/wealth/types', 'WealthType', '', 32, 1),"
                + "('旅游管理', 'travel:manage', '✈', '/travel', 'TravelLayout', '', 40, 1),"
                + "('武汉游', 'travel:wuhan', '⌂', '/travel/wuhan', 'TravelWuhan', '', 41, 1),"
                + "('国内游', 'travel:domestic', '☰', '/travel/domestic', 'TravelDomestic', '', 42, 1),"
                + "('厨房管理', 'kitchen:manage', '🍳', '/kitchen', 'KitchenLayout', '', 25, 1),"
                + "('菜品清单', 'kitchen:dish', '☰', '/kitchen/dishes', 'KitchenDish', '', 26, 1),"
                + "('食材清单', 'kitchen:ingredient', '◈', '/kitchen/ingredients', 'KitchenIngredient', '', 27, 1),"
                + "('制作记录', 'kitchen:cook', '⌖', '/kitchen/cook-records', 'KitchenCookRecord', '', 28, 1),"
                + "('相册管理', 'album:manage', '📷', '/album', 'AlbumLayout', '', 35, 1),"
                + "('相册', 'album:photo', '☰', '/album/photos', 'AlbumPhoto', '', 36, 1),"
                + "('相册主题', 'album:theme', '◈', '/album/themes', 'AlbumTheme', '', 37, 1),"
                + "('物品管理', 'goods:manage', '📦', '/goods', 'GoodsLayout', '', 38, 1),"
                + "('物品明细', 'goods:item', '☰', '/goods/items', 'GoodsItem', '', 39, 1),"
                + "('存放位置', 'goods:location', '⌖', '/goods/locations', 'GoodsLocation', '', 40, 1),"
                + "('系统管理', 'system:manage', '⚙', '/system', 'SystemLayout', '', 80, 1),"
                + "('用户管理', 'system:user', '◉', '/system/users', 'SystemUser', '', 81, 1),"
                + "('角色管理', 'system:role', '◇', '/system/roles', 'SystemRole', '', 82, 1),"
                + "('菜单权限', 'system:menu', '☷', '/system/menus', 'SystemMenu', '', 83, 1)");
        jdbcTemplate.update("DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.id = rm.menu_id "
                + "WHERE m.code IN ('asset:create', 'asset:update', 'asset:delete')");
        jdbcTemplate.update("DELETE FROM sys_menu WHERE code IN ('asset:create', 'asset:update', 'asset:delete')");
        Long systemMenuId = getMenuId("system:manage");
        if (systemMenuId != null) {
            jdbcTemplate.update("UPDATE sys_menu SET parent_id = ?, visible = 1 WHERE code IN ('system:user', 'system:role', 'system:menu')", systemMenuId);
        }
        Long clothesMenuId = getMenuId("clothes:manage");
        if (clothesMenuId != null) {
            jdbcTemplate.update("UPDATE sys_menu SET parent_id = ?, visible = 1 WHERE code IN ('clothes:item', 'clothes:type', 'clothes:location')", clothesMenuId);
        }
        Long wealthMenuId = getMenuId("wealth:manage");
        if (wealthMenuId != null) {
            jdbcTemplate.update("UPDATE sys_menu SET parent_id = ?, visible = 1 WHERE code IN ('wealth:overview', 'wealth:type')", wealthMenuId);
        }
        Long travelMenuId = getMenuId("travel:manage");
        if (travelMenuId != null) {
            jdbcTemplate.update("UPDATE sys_menu SET parent_id = ?, visible = 1 WHERE code IN ('travel:wuhan', 'travel:domestic')", travelMenuId);
        }
        Long kitchenMenuId = getMenuId("kitchen:manage");
        if (kitchenMenuId != null) {
            jdbcTemplate.update("UPDATE sys_menu SET parent_id = ?, visible = 1 WHERE code IN ('kitchen:dish', 'kitchen:ingredient', 'kitchen:cook')", kitchenMenuId);
        }
        Long albumMenuId = getMenuId("album:manage");
        if (albumMenuId != null) {
            jdbcTemplate.update("UPDATE sys_menu SET parent_id = ?, visible = 1 WHERE code IN ('album:photo', 'album:theme')", albumMenuId);
        }
        Long goodsMenuId = getMenuId("goods:manage");
        if (goodsMenuId != null) {
            jdbcTemplate.update("UPDATE sys_menu SET parent_id = ?, visible = 1 WHERE code IN ('goods:item', 'goods:location')", goodsMenuId);
        }
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/assets', component = 'AssetConsole', sort_no = 10, visible = 1 WHERE code = 'asset:view'");
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/clothes', component = 'ClothesLayout', sort_no = 20, visible = 1 WHERE code = 'clothes:manage'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/clothes/items', component = 'ClothesItem', sort_no = 21, visible = 1 WHERE code = 'clothes:item'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/clothes/types', component = 'ClothesType', sort_no = 22, visible = 1 WHERE code = 'clothes:type'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/clothes/locations', component = 'ClothesLocation', sort_no = 23, visible = 1 WHERE code = 'clothes:location'");
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/wealth', component = 'WealthLayout', sort_no = 30, visible = 1 WHERE code = 'wealth:manage'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/wealth/assets', component = 'WealthOverview', sort_no = 31, visible = 1 WHERE code = 'wealth:overview'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/wealth/types', component = 'WealthType', sort_no = 32, visible = 1 WHERE code = 'wealth:type'");
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/travel', component = 'TravelLayout', sort_no = 40, visible = 1 WHERE code = 'travel:manage'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/travel/wuhan', component = 'TravelWuhan', sort_no = 41, visible = 1 WHERE code = 'travel:wuhan'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/travel/domestic', component = 'TravelDomestic', sort_no = 42, visible = 1 WHERE code = 'travel:domestic'");
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/kitchen', component = 'KitchenLayout', sort_no = 25, visible = 1 WHERE code = 'kitchen:manage'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/kitchen/dishes', component = 'KitchenDish', sort_no = 26, visible = 1 WHERE code = 'kitchen:dish'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/kitchen/ingredients', component = 'KitchenIngredient', sort_no = 27, visible = 1 WHERE code = 'kitchen:ingredient'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/kitchen/cook-records', component = 'KitchenCookRecord', sort_no = 28, visible = 1 WHERE code = 'kitchen:cook'");
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/album', component = 'AlbumLayout', sort_no = 35, visible = 1 WHERE code = 'album:manage'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/album/photos', component = 'AlbumPhoto', sort_no = 36, visible = 1 WHERE code = 'album:photo'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/album/themes', component = 'AlbumTheme', sort_no = 37, visible = 1 WHERE code = 'album:theme'");
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/goods', component = 'GoodsLayout', sort_no = 38, visible = 1 WHERE code = 'goods:manage'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/goods/items', component = 'GoodsItem', sort_no = 39, visible = 1 WHERE code = 'goods:item'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/goods/locations', component = 'GoodsLocation', sort_no = 40, visible = 1 WHERE code = 'goods:location'");
        jdbcTemplate.update("UPDATE sys_menu SET parent_id = NULL, path = '/system', component = 'SystemLayout', sort_no = 80, visible = 1 WHERE code = 'system:manage'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/system/users', component = 'SystemUser', sort_no = 81, visible = 1 WHERE code = 'system:user'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/system/roles', component = 'SystemRole', sort_no = 82, visible = 1 WHERE code = 'system:role'");
        jdbcTemplate.update("UPDATE sys_menu SET path = '/system/menus', component = 'SystemMenu', sort_no = 83, visible = 1 WHERE code = 'system:menu'");

        jdbcTemplate.update("INSERT IGNORE INTO sys_role (code, name, remark) VALUES "
                + "('admin', '家庭管理员', '拥有全部系统权限'),"
                + "('member', '家庭成员', '可查看并维护家庭资产')");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m WHERE r.code = 'admin'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code = 'asset:view' "
                + "WHERE r.code = 'member'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('clothes:manage', 'clothes:item', 'clothes:type', 'clothes:location') "
                + "WHERE r.code = 'member'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('wealth:manage', 'wealth:overview', 'wealth:type') "
                + "WHERE r.code = 'member'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m "
                + "WHERE r.code = 'admin' AND m.code IN ('travel:manage', 'travel:wuhan', 'travel:domestic')");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('travel:manage', 'travel:wuhan', 'travel:domestic') "
                + "WHERE r.code = 'member'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m "
                + "WHERE r.code = 'admin' AND m.code IN ('kitchen:manage', 'kitchen:dish', 'kitchen:ingredient', 'kitchen:cook')");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('kitchen:manage', 'kitchen:dish', 'kitchen:ingredient', 'kitchen:cook') "
                + "WHERE r.code = 'member'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m "
                + "WHERE r.code = 'admin' AND m.code IN ('album:manage', 'album:photo', 'album:theme')");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('album:manage', 'album:photo', 'album:theme') "
                + "WHERE r.code = 'member'");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m "
                + "WHERE r.code = 'admin' AND m.code IN ('goods:manage', 'goods:item', 'goods:location')");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role_menu (role_id, menu_id) "
                + "SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.code IN ('goods:manage', 'goods:item', 'goods:location') "
                + "WHERE r.code = 'member'");
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
        if (userCount != null && userCount == 0) {
            jdbcTemplate.update("INSERT INTO sys_user (username, password_hash, display_name, status) VALUES (?, ?, ?, 1)",
                    "admin", PasswordUtil.hash("admin123"), "家庭管理员");
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) "
                    + "SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.code = 'admin' WHERE u.username = 'admin'");
        }
    }

    private Long getMenuId(String code) {
        java.util.List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM sys_menu WHERE code = ?", Long.class, code);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
