package com.example.familymanage.service;

import com.example.familymanage.common.ForbiddenException;
import com.example.familymanage.dto.MenuSaveRequest;
import com.example.familymanage.dto.RoleSaveRequest;
import com.example.familymanage.dto.UserSaveRequest;
import com.example.familymanage.security.AuthContext;
import com.example.familymanage.security.PasswordUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemManageService {

    private final JdbcTemplate jdbcTemplate;

    public SystemManageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, display_name, status, last_login_at, created_at FROM sys_user ORDER BY id");
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("id")).longValue();
            row.put("roleIds", listRelationIds("SELECT role_id FROM sys_user_role WHERE user_id = ? ORDER BY role_id", userId));
            row.put("roleNames", String.join("、", listStrings(
                    "SELECT r.name FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = ? ORDER BY r.id",
                    userId)));
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUser(UserSaveRequest request) {
        validateUser(request, true);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO sys_user (username, password_hash, display_name, status, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getUsername().trim());
            statement.setString(2, PasswordUtil.hash(request.getPassword()));
            statement.setString(3, safeText(request.getDisplayName(), request.getUsername().trim()));
            statement.setInt(4, request.getStatus() == null ? 1 : request.getStatus());
            return statement;
        }, keyHolder);
        Long userId = keyHolder.getKey().longValue();
        replaceRelations("sys_user_role", "user_id", "role_id", userId, request.getRoleIds());
        return getUser(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateUser(Long id, UserSaveRequest request) {
        validateUser(request, false);
        if (StringUtils.hasText(request.getPassword())) {
            jdbcTemplate.update("UPDATE sys_user SET username = ?, password_hash = ?, display_name = ?, status = ?, updated_at = NOW() WHERE id = ?",
                    request.getUsername().trim(), PasswordUtil.hash(request.getPassword()),
                    safeText(request.getDisplayName(), request.getUsername().trim()),
                    request.getStatus() == null ? 1 : request.getStatus(), id);
        } else {
            jdbcTemplate.update("UPDATE sys_user SET username = ?, display_name = ?, status = ?, updated_at = NOW() WHERE id = ?",
                    request.getUsername().trim(), safeText(request.getDisplayName(), request.getUsername().trim()),
                    request.getStatus() == null ? 1 : request.getStatus(), id);
        }
        replaceRelations("sys_user_role", "user_id", "role_id", id, request.getRoleIds());
        return getUser(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(Long id) {
        if (AuthContext.get() != null && id.equals(AuthContext.get().getId())) {
            throw new ForbiddenException("不能删除当前登录账号");
        }
        jdbcTemplate.update("DELETE FROM sys_auth_token WHERE user_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", id);
        return true;
    }

    public List<Map<String, Object>> listRoles() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, code, name, remark, created_at FROM sys_role ORDER BY id");
        for (Map<String, Object> row : rows) {
            Long roleId = ((Number) row.get("id")).longValue();
            row.put("menuIds", listRelationIds("SELECT menu_id FROM sys_role_menu WHERE role_id = ? ORDER BY menu_id", roleId));
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createRole(RoleSaveRequest request) {
        validateRole(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO sys_role (code, name, remark, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getCode().trim());
            statement.setString(2, request.getName().trim());
            statement.setString(3, request.getRemark());
            return statement;
        }, keyHolder);
        Long roleId = keyHolder.getKey().longValue();
        replaceRelations("sys_role_menu", "role_id", "menu_id", roleId, request.getMenuIds());
        return getRole(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateRole(Long id, RoleSaveRequest request) {
        validateRole(request);
        jdbcTemplate.update("UPDATE sys_role SET code = ?, name = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getCode().trim(), request.getName().trim(), request.getRemark(), id);
        replaceRelations("sys_role_menu", "role_id", "menu_id", id, request.getMenuIds());
        return getRole(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRole(Long id) {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE role_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_role WHERE id = ?", id);
        return true;
    }

    public List<Map<String, Object>> listMenus() {
        return jdbcTemplate.queryForList(
                "SELECT id, name, code, icon, sort_no, visible, created_at FROM sys_menu ORDER BY sort_no, id");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createMenu(MenuSaveRequest request) {
        validateMenu(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO sys_menu (name, code, icon, sort_no, visible, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getName().trim());
            statement.setString(2, request.getCode().trim());
            statement.setString(3, request.getIcon());
            statement.setInt(4, request.getSortNo() == null ? 100 : request.getSortNo());
            statement.setInt(5, Boolean.FALSE.equals(request.getVisible()) ? 0 : 1);
            return statement;
        }, keyHolder);
        return getMenu(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateMenu(Long id, MenuSaveRequest request) {
        validateMenu(request);
        jdbcTemplate.update("UPDATE sys_menu SET name = ?, code = ?, icon = ?, sort_no = ?, visible = ?, updated_at = NOW() WHERE id = ?",
                request.getName().trim(), request.getCode().trim(), request.getIcon(),
                request.getSortNo() == null ? 100 : request.getSortNo(),
                Boolean.FALSE.equals(request.getVisible()) ? 0 : 1, id);
        return getMenu(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteMenu(Long id) {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE menu_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_menu WHERE id = ?", id);
        return true;
    }

    private Map<String, Object> getUser(Long id) {
        List<Map<String, Object>> matches = listUsers();
        for (Map<String, Object> item : matches) {
            if (id.equals(((Number) item.get("id")).longValue())) {
                return item;
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> getRole(Long id) {
        List<Map<String, Object>> matches = listRoles();
        for (Map<String, Object> item : matches) {
            if (id.equals(((Number) item.get("id")).longValue())) {
                return item;
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> getMenu(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, code, icon, sort_no, visible, created_at FROM sys_menu WHERE id = ?",
                id);
        return rows.isEmpty() ? Collections.<String, Object>emptyMap() : rows.get(0);
    }

    private void validateUser(UserSaveRequest request, boolean requirePassword) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("请输入账号");
        }
        if (requirePassword && !StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("请输入初始密码");
        }
    }

    private void validateRole(RoleSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请输入角色编码和名称");
        }
    }

    private void validateMenu(MenuSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("请输入权限编码和名称");
        }
    }

    private void replaceRelations(String table, String ownerColumn, String targetColumn, Long ownerId, List<Long> targetIds) {
        jdbcTemplate.update("DELETE FROM " + table + " WHERE " + ownerColumn + " = ?", ownerId);
        if (targetIds == null) {
            return;
        }
        for (Long targetId : targetIds) {
            jdbcTemplate.update("INSERT INTO " + table + " (" + ownerColumn + ", " + targetColumn + ") VALUES (?, ?)", ownerId, targetId);
        }
    }

    private List<Long> listRelationIds(String sql, Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        List<Long> ids = new ArrayList<Long>();
        for (Map<String, Object> row : rows) {
            Object value = row.values().iterator().next();
            ids.add(((Number) value).longValue());
        }
        return ids;
    }

    private List<String> listStrings(String sql, Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        List<String> values = new ArrayList<String>();
        for (Map<String, Object> row : rows) {
            Object value = row.values().iterator().next();
            if (value != null) {
                values.add(String.valueOf(value));
            }
        }
        return values;
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
