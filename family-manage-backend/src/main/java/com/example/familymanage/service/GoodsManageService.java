package com.example.familymanage.service;

import com.example.familymanage.dto.GoodsItemRequest;
import com.example.familymanage.dto.GoodsLocationRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoodsManageService {

    private static final String ITEM_SELECT_SQL = "SELECT i.id, i.item_name, i.area_code, l.area_name, "
            + "i.purchase_time, i.expire_time, i.image_url, i.remark, "
            + "i.created_by, i.created_at, i.updated_by, i.updated_at "
            + "FROM goods_item i "
            + "LEFT JOIN goods_location l ON l.area_code = i.area_code ";

    private final JdbcTemplate jdbcTemplate;
    private final MinioService minioService;

    public GoodsManageService(JdbcTemplate jdbcTemplate, MinioService minioService) {
        this.jdbcTemplate = jdbcTemplate;
        this.minioService = minioService;
    }

    public Map<String, Object> listItems(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM goods_item", Long.class);
        if (total == null) {
            total = 0L;
        }
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                ITEM_SELECT_SQL + "ORDER BY i.updated_at DESC, i.id DESC LIMIT ? OFFSET ?",
                safeSize, offset);
        records.forEach(this::normalizeItemImageUrl);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createItem(GoodsItemRequest request) {
        validateCreateItem(request);
        ensureAreaCodeExists(request.getAreaCode().trim());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String operator = currentUsername();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO goods_item (item_name, area_code, purchase_time, expire_time, image_url, remark, created_by, created_at, updated_by, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getItemName().trim());
            statement.setString(2, request.getAreaCode().trim());
            statement.setDate(3, toDate(request.getPurchaseTime()));
            statement.setDate(4, toDate(request.getExpireTime()));
            statement.setString(5, request.getImageUrl() == null ? "" : request.getImageUrl().trim());
            statement.setString(6, request.getRemark());
            statement.setString(7, operator);
            statement.setString(8, operator);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("保存物品失败，请重试");
        }
        return getItem(key.longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateItem(Long id, GoodsItemRequest request) {
        validateUpdateItem(request);
        ensureAreaCodeExists(request.getAreaCode().trim());
        jdbcTemplate.update(
                "UPDATE goods_item SET area_code = ?, expire_time = ?, image_url = ?, remark = ?, updated_by = ?, updated_at = NOW() WHERE id = ?",
                request.getAreaCode().trim(), toDate(request.getExpireTime()),
                request.getImageUrl() == null ? "" : request.getImageUrl().trim(),
                request.getRemark(), currentUsername(), id);
        return getItem(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteItem(Long id) {
        jdbcTemplate.update("DELETE FROM goods_item WHERE id = ?", id);
        return true;
    }

    public List<Map<String, Object>> listLocations() {
        return jdbcTemplate.queryForList(
                "SELECT id, area_code, area_name, remark FROM goods_location ORDER BY id DESC");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createLocation(GoodsLocationRequest request) {
        validateLocation(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO goods_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getAreaCode().trim());
            statement.setString(2, request.getAreaName().trim());
            statement.setString(3, request.getRemark());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("保存存放位置失败，请重试");
        }
        return getLocation(key.longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateLocation(Long id, GoodsLocationRequest request) {
        validateLocation(request);
        jdbcTemplate.update("UPDATE goods_location SET area_code = ?, area_name = ?, remark = ? WHERE id = ?",
                request.getAreaCode().trim(), request.getAreaName().trim(), request.getRemark(), id);
        return getLocation(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteLocation(Long id) {
        jdbcTemplate.update("DELETE FROM goods_location WHERE id = ?", id);
        return true;
    }

    public Map<String, Object> getOptions() {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("locations", jdbcTemplate.queryForList(
                "SELECT area_code, area_name FROM goods_location ORDER BY id"));
        return options;
    }

    private Map<String, Object> getItem(Long id) {
        Map<String, Object> item = jdbcTemplate.queryForMap(ITEM_SELECT_SQL + "WHERE i.id = ?", id);
        normalizeItemImageUrl(item);
        return item;
    }

    private Map<String, Object> getLocation(Long id) {
        return jdbcTemplate.queryForMap(
                "SELECT id, area_code, area_name, remark FROM goods_location WHERE id = ?", id);
    }

    private void normalizeItemImageUrl(Map<String, Object> item) {
        if (item == null || !item.containsKey("image_url")) {
            return;
        }
        Object imageUrl = item.get("image_url");
        item.put("image_url", minioService.normalizePublicUrl(imageUrl == null ? "" : String.valueOf(imageUrl)));
    }

    private void validateCreateItem(GoodsItemRequest request) {
        if (request == null || !StringUtils.hasText(request.getItemName())) {
            throw new IllegalArgumentException("请输入物品名称");
        }
        if (!StringUtils.hasText(request.getAreaCode())) {
            throw new IllegalArgumentException("请选择存放位置");
        }
    }

    private void validateUpdateItem(GoodsItemRequest request) {
        if (request == null || !StringUtils.hasText(request.getAreaCode())) {
            throw new IllegalArgumentException("请选择存放位置");
        }
    }

    private void validateLocation(GoodsLocationRequest request) {
        if (request == null || !StringUtils.hasText(request.getAreaCode()) || !StringUtils.hasText(request.getAreaName())) {
            throw new IllegalArgumentException("请输入所在区域编码和名称");
        }
    }

    private void ensureAreaCodeExists(String areaCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM goods_location WHERE area_code = ?", Integer.class, areaCode);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("存放位置不存在，请先在存放位置中维护");
        }
    }

    private String currentUsername() {
        AuthUser user = AuthContext.get();
        return user == null ? "system" : user.getUsername();
    }

    private Date toDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(value.trim()));
    }
}
