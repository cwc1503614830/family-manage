package com.example.familymanage.service;

import com.example.familymanage.dto.ClothesItemRequest;
import com.example.familymanage.dto.ClothesLocationRequest;
import com.example.familymanage.dto.ClothesTypeRequest;
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
public class ClothesManageService {

    private final JdbcTemplate jdbcTemplate;
    private final MinioService minioService;

    public ClothesManageService(JdbcTemplate jdbcTemplate, MinioService minioService) {
        this.jdbcTemplate = jdbcTemplate;
        this.minioService = minioService;
    }

    private static final String ITEM_SELECT_SQL = "SELECT i.id, i.clothes_name, i.type_code, t.type_name, i.area_code, l.area_name, "
            + "i.purchase_time, i.expire_time, i.image_url, i.owner_user_id, u.display_name owner_name, i.remark, "
            + "i.created_by, i.created_at, i.updated_by, i.updated_at "
            + "FROM clothes_item i "
            + "LEFT JOIN clothes_type t ON t.type_code = i.type_code "
            + "LEFT JOIN clothes_location l ON l.area_code = i.area_code "
            + "LEFT JOIN sys_user u ON u.id = i.owner_user_id ";

    public Map<String, Object> listItems(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clothes_item", Long.class);
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
    public Map<String, Object> createItem(ClothesItemRequest request) {
        validateItem(request, true);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String operator = currentUsername();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO clothes_item "
                    + "(clothes_name, type_code, area_code, purchase_time, expire_time, image_url, owner_user_id, remark, created_by, created_at, updated_by, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW())", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getClothesName().trim());
            statement.setString(2, request.getTypeCode().trim());
            statement.setString(3, request.getAreaCode().trim());
            statement.setDate(4, toDate(request.getPurchaseTime()));
            statement.setDate(5, toDate(request.getExpireTime()));
            statement.setString(6, normalizeStoredImageUrl(request.getImageUrl()));
            statement.setLong(7, request.getOwnerUserId());
            statement.setString(8, request.getRemark());
            statement.setString(9, operator);
            statement.setString(10, operator);
            return statement;
        }, keyHolder);
        return getItem(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateItem(Long id, ClothesItemRequest request) {
        validateItem(request, false);
        jdbcTemplate.update("UPDATE clothes_item SET clothes_name = ?, type_code = ?, area_code = ?, expire_time = ?, image_url = ?, remark = ?, updated_by = ?, updated_at = NOW() WHERE id = ?",
                request.getClothesName().trim(), request.getTypeCode().trim(), request.getAreaCode().trim(), toDate(request.getExpireTime()),
                normalizeStoredImageUrl(request.getImageUrl()),
                request.getRemark(), currentUsername(), id);
        return getItem(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteItem(Long id) {
        jdbcTemplate.update("DELETE FROM clothes_item WHERE id = ?", id);
        return true;
    }

    public List<Map<String, Object>> listTypes() {
        return jdbcTemplate.queryForList("SELECT id, type_code, type_name, remark FROM clothes_type ORDER BY id DESC");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createType(ClothesTypeRequest request) {
        validateType(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO clothes_type (type_code, type_name, remark) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getTypeCode().trim());
            statement.setString(2, request.getTypeName().trim());
            statement.setString(3, request.getRemark());
            return statement;
        }, keyHolder);
        return getType(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateType(Long id, ClothesTypeRequest request) {
        validateType(request);
        jdbcTemplate.update("UPDATE clothes_type SET type_code = ?, type_name = ?, remark = ? WHERE id = ?",
                request.getTypeCode().trim(), request.getTypeName().trim(), request.getRemark(), id);
        return getType(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteType(Long id) {
        jdbcTemplate.update("DELETE FROM clothes_type WHERE id = ?", id);
        return true;
    }

    public List<Map<String, Object>> listLocations() {
        return jdbcTemplate.queryForList("SELECT id, area_code, area_name, remark FROM clothes_location ORDER BY id DESC");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createLocation(ClothesLocationRequest request) {
        validateLocation(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO clothes_location (area_code, area_name, remark) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getAreaCode().trim());
            statement.setString(2, request.getAreaName().trim());
            statement.setString(3, request.getRemark());
            return statement;
        }, keyHolder);
        return getLocation(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateLocation(Long id, ClothesLocationRequest request) {
        validateLocation(request);
        jdbcTemplate.update("UPDATE clothes_location SET area_code = ?, area_name = ?, remark = ? WHERE id = ?",
                request.getAreaCode().trim(), request.getAreaName().trim(), request.getRemark(), id);
        return getLocation(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteLocation(Long id) {
        jdbcTemplate.update("DELETE FROM clothes_location WHERE id = ?", id);
        return true;
    }

    public Map<String, Object> getOptions() {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("types", jdbcTemplate.queryForList("SELECT type_code, type_name FROM clothes_type ORDER BY id"));
        options.put("locations", jdbcTemplate.queryForList("SELECT area_code, area_name FROM clothes_location ORDER BY id"));
        options.put("users", jdbcTemplate.queryForList("SELECT id, username, display_name FROM sys_user WHERE status = 1 ORDER BY id"));
        return options;
    }

    private Map<String, Object> getItem(Long id) {
        Map<String, Object> item = jdbcTemplate.queryForMap(ITEM_SELECT_SQL + "WHERE i.id = ?", id);
        normalizeItemImageUrl(item);
        return item;
    }

    private void normalizeItemImageUrl(Map<String, Object> item) {
        if (item == null || !item.containsKey("image_url")) {
            return;
        }
        Object imageUrl = item.get("image_url");
        item.put("image_url", minioService.normalizePublicUrl(imageUrl == null ? "" : String.valueOf(imageUrl)));
    }

    private String normalizeStoredImageUrl(String imageUrl) {
        return minioService.normalizePublicUrl(imageUrl);
    }

    private Map<String, Object> getType(Long id) {
        return jdbcTemplate.queryForMap("SELECT id, type_code, type_name, remark FROM clothes_type WHERE id = ?", id);
    }

    private Map<String, Object> getLocation(Long id) {
        return jdbcTemplate.queryForMap("SELECT id, area_code, area_name, remark FROM clothes_location WHERE id = ?", id);
    }

    private void validateItem(ClothesItemRequest request, boolean requireCreateFields) {
        if (request == null || !StringUtils.hasText(request.getClothesName())) {
            throw new IllegalArgumentException("请输入衣服名称");
        }
        if (!StringUtils.hasText(request.getTypeCode())) {
            throw new IllegalArgumentException("请选择衣服类型");
        }
        if (!StringUtils.hasText(request.getAreaCode())) {
            throw new IllegalArgumentException("请选择存放位置");
        }
        if (requireCreateFields && request.getOwnerUserId() == null) {
            throw new IllegalArgumentException("请选择拥有者");
        }
    }

    private void validateType(ClothesTypeRequest request) {
        if (request == null || !StringUtils.hasText(request.getTypeCode()) || !StringUtils.hasText(request.getTypeName())) {
            throw new IllegalArgumentException("请输入衣服类型编码和名称");
        }
    }

    private void validateLocation(ClothesLocationRequest request) {
        if (request == null || !StringUtils.hasText(request.getAreaCode()) || !StringUtils.hasText(request.getAreaName())) {
            throw new IllegalArgumentException("请输入所在区域编码和名称");
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
