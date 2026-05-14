package com.example.familymanage.service;

import com.example.familymanage.dto.WealthAssetRequest;
import com.example.familymanage.dto.WealthTypeRequest;
import com.example.familymanage.security.AuthContext;
import com.example.familymanage.security.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WealthManageService {

    private final JdbcTemplate jdbcTemplate;

    public WealthManageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listAssets() {
        return jdbcTemplate.queryForList("SELECT a.id, a.asset_name, a.type_id, t.asset_type, a.amount, a.record_date, "
                + "a.owner_user_id, u.display_name owner_name, a.remark, a.created_by, a.created_at, a.updated_by, a.updated_at "
                + "FROM wealth_asset a "
                + "LEFT JOIN wealth_type t ON t.id = a.type_id "
                + "LEFT JOIN sys_user u ON u.id = a.owner_user_id "
                + "ORDER BY a.updated_at DESC, a.id DESC");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAsset(WealthAssetRequest request) {
        validateAsset(request, true);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String operator = currentUsername();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO wealth_asset "
                    + "(asset_name, type_id, amount, record_date, owner_user_id, remark, created_by, created_at, updated_by, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, NOW())", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getAssetName().trim());
            statement.setLong(2, request.getTypeId());
            statement.setBigDecimal(3, normalizedAmount(request.getAmount()));
            statement.setDate(4, toDate(request.getRecordDate()));
            statement.setLong(5, request.getOwnerUserId());
            statement.setString(6, request.getRemark());
            statement.setString(7, operator);
            statement.setString(8, operator);
            return statement;
        }, keyHolder);
        return getAsset(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateAsset(Long id, WealthAssetRequest request) {
        validateAsset(request, false);
        jdbcTemplate.update("UPDATE wealth_asset SET type_id = ?, amount = ?, record_date = ?, owner_user_id = ?, remark = ?, updated_by = ?, updated_at = NOW() WHERE id = ?",
                request.getTypeId(), normalizedAmount(request.getAmount()), toDate(request.getRecordDate()),
                request.getOwnerUserId(), request.getRemark(), currentUsername(), id);
        return getAsset(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAsset(Long id) {
        jdbcTemplate.update("DELETE FROM wealth_asset WHERE id = ?", id);
        return true;
    }

    public List<Map<String, Object>> listTypes() {
        return jdbcTemplate.queryForList("SELECT id, asset_type, remark FROM wealth_type ORDER BY id DESC");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createType(WealthTypeRequest request) {
        validateType(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO wealth_type (asset_type, remark) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getAssetType().trim());
            statement.setString(2, request.getRemark());
            return statement;
        }, keyHolder);
        return getType(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateType(Long id, WealthTypeRequest request) {
        validateType(request);
        jdbcTemplate.update("UPDATE wealth_type SET asset_type = ?, remark = ? WHERE id = ?",
                request.getAssetType().trim(), request.getRemark(), id);
        return getType(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteType(Long id) {
        jdbcTemplate.update("DELETE FROM wealth_type WHERE id = ?", id);
        return true;
    }

    public Map<String, Object> getOptions() {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("types", jdbcTemplate.queryForList("SELECT id, asset_type FROM wealth_type ORDER BY id"));
        options.put("users", jdbcTemplate.queryForList("SELECT id, username, display_name FROM sys_user WHERE status = 1 ORDER BY id"));
        return options;
    }

    private Map<String, Object> getAsset(Long id) {
        return jdbcTemplate.queryForMap("SELECT a.id, a.asset_name, a.type_id, t.asset_type, a.amount, a.record_date, "
                + "a.owner_user_id, u.display_name owner_name, a.remark, a.created_by, a.created_at, a.updated_by, a.updated_at "
                + "FROM wealth_asset a "
                + "LEFT JOIN wealth_type t ON t.id = a.type_id "
                + "LEFT JOIN sys_user u ON u.id = a.owner_user_id WHERE a.id = ?", id);
    }

    private Map<String, Object> getType(Long id) {
        return jdbcTemplate.queryForMap("SELECT id, asset_type, remark FROM wealth_type WHERE id = ?", id);
    }

    private void validateAsset(WealthAssetRequest request, boolean requireCreateFields) {
        if (request == null || (requireCreateFields && !StringUtils.hasText(request.getAssetName()))) {
            throw new IllegalArgumentException("请输入资产名称");
        }
        if (request.getTypeId() == null) {
            throw new IllegalArgumentException("请选择资产类型");
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("请输入资产金额");
        }
        if (request.getOwnerUserId() == null) {
            throw new IllegalArgumentException("请选择持有人");
        }
    }

    private void validateType(WealthTypeRequest request) {
        if (request == null || !StringUtils.hasText(request.getAssetType())) {
            throw new IllegalArgumentException("请输入资产类型");
        }
    }

    private BigDecimal normalizedAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
