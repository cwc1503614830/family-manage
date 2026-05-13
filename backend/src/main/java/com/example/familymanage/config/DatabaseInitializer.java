package com.example.familymanage.config;

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
}
