package com.example.familymanage.service;

import com.example.familymanage.dto.KitchenCookRecordRequest;
import com.example.familymanage.dto.KitchenDishRequest;
import com.example.familymanage.dto.KitchenIngredientRequest;
import com.example.familymanage.security.AuthContext;
import com.example.familymanage.security.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KitchenManageService {

    private static final String COOK_RECORD_SELECT_SQL = "SELECT r.id, r.dish_name, r.cook_date, r.is_done, r.remark, "
            + "r.ordered_by_user_id, u.display_name ordered_by_name, r.created_at, r.updated_at "
            + "FROM kitchen_cook_record r LEFT JOIN sys_user u ON u.id = r.ordered_by_user_id ";

    private final JdbcTemplate jdbcTemplate;
    private final MinioService minioService;

    public KitchenManageService(JdbcTemplate jdbcTemplate, MinioService minioService) {
        this.jdbcTemplate = jdbcTemplate;
        this.minioService = minioService;
    }

    public Map<String, Object> listDishes(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kitchen_dish", Long.class);
        if (total == null) {
            total = 0L;
        }
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT id, dish_name, ingredients, image_url, remark, created_at, updated_at "
                        + "FROM kitchen_dish ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                safeSize, offset);
        records.forEach(this::normalizeDishImageUrl);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createDish(KitchenDishRequest request) {
        validateDish(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO kitchen_dish (dish_name, ingredients, image_url, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getDishName().trim());
            statement.setString(2, request.getIngredients());
            statement.setString(3, normalizeStoredImageUrl(request.getImageUrl()));
            statement.setString(4, request.getRemark());
            return statement;
        }, keyHolder);
        return getDish(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateDish(Long id, KitchenDishRequest request) {
        validateDish(request);
        jdbcTemplate.update("UPDATE kitchen_dish SET dish_name = ?, ingredients = ?, image_url = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getDishName().trim(), request.getIngredients(),
                normalizeStoredImageUrl(request.getImageUrl()), request.getRemark(), id);
        return getDish(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDish(Long id) {
        jdbcTemplate.update("DELETE FROM kitchen_dish WHERE id = ?", id);
        return true;
    }

    public Map<String, Object> listIngredients(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kitchen_ingredient", Long.class);
        if (total == null) {
            total = 0L;
        }
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT id, ingredient_name, production_date, shelf_life_months, expire_date, image_url, remark, created_at, updated_at "
                        + "FROM kitchen_ingredient ORDER BY expire_date IS NULL, expire_date ASC, id ASC LIMIT ? OFFSET ?",
                safeSize, offset);
        records.forEach(this::normalizeIngredientImageUrl);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createIngredient(KitchenIngredientRequest request) {
        validateIngredient(request);
        Date productionDate = toDate(request.getProductionDate());
        Date expireDate = calcExpireDate(productionDate, request.getShelfLifeMonths());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO kitchen_ingredient (ingredient_name, production_date, shelf_life_months, expire_date, image_url, remark) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getIngredientName().trim());
            statement.setDate(2, productionDate);
            if (request.getShelfLifeMonths() == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, request.getShelfLifeMonths());
            }
            statement.setDate(4, expireDate);
            statement.setString(5, normalizeStoredImageUrl(request.getImageUrl()));
            statement.setString(6, request.getRemark());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("保存食材失败，请重试");
        }
        return getIngredient(key.longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateIngredient(Long id, KitchenIngredientRequest request) {
        validateIngredient(request);
        Date productionDate = toDate(request.getProductionDate());
        Date expireDate = calcExpireDate(productionDate, request.getShelfLifeMonths());
        jdbcTemplate.update("UPDATE kitchen_ingredient SET ingredient_name = ?, production_date = ?, shelf_life_months = ?, "
                        + "expire_date = ?, image_url = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getIngredientName().trim(), productionDate, request.getShelfLifeMonths(),
                expireDate, normalizeStoredImageUrl(request.getImageUrl()), request.getRemark(), id);
        return getIngredient(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteIngredient(Long id) {
        jdbcTemplate.update("DELETE FROM kitchen_ingredient WHERE id = ?", id);
        return true;
    }

    public Map<String, Object> listCookRecords(int page, int size, Boolean pendingOnly, Boolean mine) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<String> conditions = new ArrayList<String>();
        List<Object> params = new ArrayList<Object>();
        if (Boolean.TRUE.equals(pendingOnly)) {
            conditions.add("r.is_done = 0");
        }
        if (Boolean.TRUE.equals(mine)) {
            Long userId = currentUserId();
            if (userId == null) {
                throw new IllegalArgumentException("请先登录");
            }
            conditions.add("r.ordered_by_user_id = ?");
            params.add(userId);
        }
        String whereSql = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kitchen_cook_record r" + whereSql,
                Long.class,
                params.toArray());
        if (total == null) {
            total = 0L;
        }
        List<Object> queryParams = new ArrayList<Object>(params);
        queryParams.add(safeSize);
        queryParams.add(offset);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                COOK_RECORD_SELECT_SQL + whereSql + " ORDER BY r.cook_date DESC, r.id DESC LIMIT ? OFFSET ?",
                queryParams.toArray());
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createCookRecord(KitchenCookRecordRequest request) {
        validateCookRecord(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Date cookDate = toDate(request.getCookDate());
        if (cookDate == null) {
            cookDate = Date.valueOf(LocalDate.now());
        }
        int isDone = request.getIsDone() != null && request.getIsDone() == 1 ? 1 : 0;
        Date finalCookDate = cookDate;
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO kitchen_cook_record (dish_name, cook_date, is_done, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getDishName().trim());
            statement.setDate(2, finalCookDate);
            statement.setInt(3, isDone);
            statement.setString(4, request.getRemark());
            return statement;
        }, keyHolder);
        return getCookRecord(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> orderFromCart(KitchenCookRecordRequest request) {
        if (request == null || request.getDishNames() == null || request.getDishNames().isEmpty()) {
            throw new IllegalArgumentException("请选择要下单的菜品");
        }
        Date cookDate = Date.valueOf(LocalDate.now());
        List<Map<String, Object>> created = new ArrayList<Map<String, Object>>();
        for (String dishName : request.getDishNames()) {
            if (!StringUtils.hasText(dishName)) {
                continue;
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            Long userId = currentUserId();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO kitchen_cook_record (dish_name, cook_date, is_done, remark, ordered_by_user_id) VALUES (?, ?, 0, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, dishName.trim());
                statement.setDate(2, cookDate);
                statement.setString(3, "购物车下单");
                if (userId == null) {
                    statement.setNull(4, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(4, userId);
                }
                return statement;
            }, keyHolder);
            created.add(getCookRecord(keyHolder.getKey().longValue()));
        }
        if (created.isEmpty()) {
            throw new IllegalArgumentException("请选择要下单的菜品");
        }
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateCookRecord(Long id, KitchenCookRecordRequest request) {
        validateCookRecord(request);
        Date cookDate = toDate(request.getCookDate());
        if (cookDate == null) {
            cookDate = Date.valueOf(LocalDate.now());
        }
        int isDone = request.getIsDone() != null && request.getIsDone() == 1 ? 1 : 0;
        jdbcTemplate.update("UPDATE kitchen_cook_record SET dish_name = ?, cook_date = ?, is_done = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getDishName().trim(), cookDate, isDone, request.getRemark(), id);
        return getCookRecord(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> completeCookRecord(Long id) {
        requireKitchenAdmin();
        jdbcTemplate.update("UPDATE kitchen_cook_record SET is_done = 1, updated_at = NOW() WHERE id = ?", id);
        return getCookRecord(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCookRecord(Long id) {
        jdbcTemplate.update("DELETE FROM kitchen_cook_record WHERE id = ?", id);
        return true;
    }

    private Map<String, Object> getDish(Long id) {
        Map<String, Object> dish = jdbcTemplate.queryForMap(
                "SELECT id, dish_name, ingredients, image_url, remark, created_at, updated_at FROM kitchen_dish WHERE id = ?", id);
        normalizeDishImageUrl(dish);
        return dish;
    }

    private Map<String, Object> getIngredient(Long id) {
        Map<String, Object> ingredient = jdbcTemplate.queryForMap(
                "SELECT id, ingredient_name, production_date, shelf_life_months, expire_date, image_url, remark, created_at, updated_at "
                        + "FROM kitchen_ingredient WHERE id = ?", id);
        normalizeIngredientImageUrl(ingredient);
        return ingredient;
    }

    private Map<String, Object> getCookRecord(Long id) {
        return jdbcTemplate.queryForMap(COOK_RECORD_SELECT_SQL + "WHERE r.id = ?", id);
    }

    private Long currentUserId() {
        AuthUser user = AuthContext.get();
        return user == null ? null : user.getId();
    }

    private void requireKitchenAdmin() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getPermissions() == null || !user.getPermissions().contains("system:manage")) {
            throw new IllegalArgumentException("仅管理员可标记制作完成");
        }
    }

    private void normalizeDishImageUrl(Map<String, Object> item) {
        if (item == null || !item.containsKey("image_url")) {
            return;
        }
        Object imageUrl = item.get("image_url");
        item.put("image_url", minioService.normalizePublicUrl(imageUrl == null ? "" : String.valueOf(imageUrl)));
    }

    private void normalizeIngredientImageUrl(Map<String, Object> item) {
        normalizeDishImageUrl(item);
    }

    private String normalizeStoredImageUrl(String imageUrl) {
        return minioService.normalizePublicUrl(imageUrl);
    }

    private Date calcExpireDate(Date productionDate, Integer shelfLifeMonths) {
        if (productionDate == null || shelfLifeMonths == null || shelfLifeMonths <= 0) {
            return null;
        }
        LocalDate local = productionDate.toLocalDate().plusMonths(shelfLifeMonths);
        return Date.valueOf(local);
    }

    private void validateDish(KitchenDishRequest request) {
        if (request == null || !StringUtils.hasText(request.getDishName())) {
            throw new IllegalArgumentException("请输入菜名");
        }
    }

    private void validateIngredient(KitchenIngredientRequest request) {
        if (request == null || !StringUtils.hasText(request.getIngredientName())) {
            throw new IllegalArgumentException("请输入食材名称");
        }
        if (request.getShelfLifeMonths() != null && request.getShelfLifeMonths() < 0) {
            throw new IllegalArgumentException("保质期不能为负数");
        }
    }

    private void validateCookRecord(KitchenCookRecordRequest request) {
        if (request == null || !StringUtils.hasText(request.getDishName())) {
            throw new IllegalArgumentException("请输入菜名");
        }
    }

    private Date toDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(value.trim()));
    }
}
