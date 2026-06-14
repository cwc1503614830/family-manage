package com.example.familymanage.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getDashboard() {
        BigDecimal totalWealthAssets = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM wealth_asset", BigDecimal.class);
        if (totalWealthAssets == null) {
            totalWealthAssets = BigDecimal.ZERO;
        }
        BigDecimal disposableWealthAssets = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(a.amount), 0) FROM wealth_asset a "
                        + "LEFT JOIN wealth_type t ON t.id = a.type_id "
                        + "WHERE t.asset_type IN ('微信', '支付宝', '银行卡', '基金', '现金')",
                BigDecimal.class);
        if (disposableWealthAssets == null) {
            disposableWealthAssets = BigDecimal.ZERO;
        }
        Long expiringIngredientCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kitchen_ingredient "
                        + "WHERE expire_date IS NOT NULL AND expire_date <= DATE_ADD(CURDATE(), INTERVAL 3 MONTH)",
                Long.class);
        if (expiringIngredientCount == null) {
            expiringIngredientCount = 0L;
        }
        Long pendingTodoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM todo_item WHERE is_done = 0", Long.class);
        if (pendingTodoCount == null) {
            pendingTodoCount = 0L;
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("totalWealthAssets", totalWealthAssets);
        result.put("disposableWealthAssets", disposableWealthAssets);
        result.put("expiringIngredientCount", expiringIngredientCount);
        result.put("pendingTodoCount", pendingTodoCount);
        return result;
    }
}
