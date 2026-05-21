package com.example.familymanage.service;

import com.example.familymanage.common.UnauthorizedException;
import com.example.familymanage.dto.LoginRequest;
import com.example.familymanage.security.AuthContext;
import com.example.familymanage.security.AuthUser;
import com.example.familymanage.security.PasswordUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final JdbcTemplate jdbcTemplate;

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new UnauthorizedException("请输入账号和密码");
        }
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, username, password_hash, display_name, status FROM sys_user WHERE username = ?",
                request.getUsername().trim());
        if (users.isEmpty()) {
            throw new UnauthorizedException("账号或密码错误");
        }
        Map<String, Object> user = users.get(0);
        if (asInt(user.get("status")) != 1) {
            throw new UnauthorizedException("账号已停用");
        }
        if (!PasswordUtil.matches(request.getPassword(), String.valueOf(user.get("password_hash")))) {
            throw new UnauthorizedException("账号或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plusDays(7);
        jdbcTemplate.update("INSERT INTO sys_auth_token (token, user_id, expire_at, created_at) VALUES (?, ?, ?, NOW())",
                token, user.get("id"), Timestamp.valueOf(expireAt));
        jdbcTemplate.update("UPDATE sys_user SET last_login_at = NOW(), updated_at = NOW() WHERE id = ?", user.get("id"));

        AuthUser authUser = loadUserById(((Number) user.get("id")).longValue());
        Map<String, Object> result = buildSession(authUser);
        result.put("token", token);
        return result;
    }

    public Map<String, Object> current() {
        return buildSession(requireCurrentUser());
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            jdbcTemplate.update("DELETE FROM sys_auth_token WHERE token = ?", token);
        }
    }

    public AuthUser requireByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("请先登录");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT t.user_id FROM sys_auth_token t JOIN sys_user u ON u.id = t.user_id "
                        + "WHERE t.token = ? AND t.expire_at > NOW() AND u.status = 1",
                token);
        if (rows.isEmpty()) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }
        return loadUserById(((Number) rows.get(0).get("user_id")).longValue());
    }

    public AuthUser requireCurrentUser() {
        AuthUser user = AuthContext.get();
        if (user == null) {
            throw new UnauthorizedException("请先登录");
        }
        return user;
    }

    private AuthUser loadUserById(Long userId) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, username, display_name FROM sys_user WHERE id = ?",
                userId);
        AuthUser user = new AuthUser();
        user.setId(((Number) row.get("id")).longValue());
        user.setUsername(String.valueOf(row.get("username")));
        user.setDisplayName(String.valueOf(row.get("display_name")));
        user.setPermissions(loadPermissions(userId));
        user.setMenus(loadVisibleMenus(user.getPermissions()));
        return user;
    }

    private Set<String> loadPermissions(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DISTINCT m.code FROM sys_menu m "
                        + "JOIN sys_role_menu rm ON rm.menu_id = m.id "
                        + "JOIN sys_user_role ur ON ur.role_id = rm.role_id "
                        + "WHERE ur.user_id = ?",
                userId);
        Set<String> permissions = new HashSet<String>();
        for (Map<String, Object> row : rows) {
            permissions.add(String.valueOf(row.get("code")));
        }
        return permissions;
    }

    private List<Map<String, Object>> loadVisibleMenus(Set<String> permissions) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, parent_id, name, code, icon, path, component, web_url FROM sys_menu "
                        + "WHERE visible = 1 ORDER BY sort_no, id");
        Map<Long, Map<String, Object>> menuMap = new LinkedHashMap<Long, Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            if (permissions.contains(String.valueOf(row.get("code")))) {
                Long id = ((Number) row.get("id")).longValue();
                menuMap.put(id, toMenu(row));
            }
        }
        for (Map<String, Object> row : rows) {
            Object parentValue = row.get("parent_id");
            if (parentValue != null) {
                Long parentId = ((Number) parentValue).longValue();
                Long id = ((Number) row.get("id")).longValue();
                if (menuMap.containsKey(id) && !menuMap.containsKey(parentId)) {
                    Map<String, Object> parent = findMenu(rows, parentId);
                    if (parent != null) {
                        menuMap.put(parentId, toMenu(parent));
                    }
                }
            }
        }
        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            if (!menuMap.containsKey(id)) {
                continue;
            }
            Map<String, Object> menu = menuMap.get(id);
            Object parentValue = menu.get("parentId");
            if (parentValue == null || !menuMap.containsKey(((Number) parentValue).longValue())) {
                roots.add(menu);
            } else {
                Map<String, Object> parent = menuMap.get(((Number) parentValue).longValue());
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(menu);
            }
        }
        return roots;
    }

    private Map<String, Object> toMenu(Map<String, Object> row) {
        Map<String, Object> menu = new HashMap<String, Object>();
        menu.put("id", row.get("id"));
        menu.put("parentId", row.get("parent_id"));
        menu.put("name", row.get("name"));
        menu.put("code", row.get("code"));
        menu.put("icon", row.get("icon"));
        menu.put("path", row.get("path"));
        menu.put("component", row.get("component"));
        menu.put("webUrl", row.get("web_url"));
        menu.put("children", new ArrayList<Map<String, Object>>());
        return menu;
    }

    private Map<String, Object> findMenu(List<Map<String, Object>> rows, Long id) {
        for (Map<String, Object> row : rows) {
            if (id.equals(((Number) row.get("id")).longValue())) {
                return row;
            }
        }
        return null;
    }

    private Map<String, Object> buildSession(AuthUser authUser) {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> user = new HashMap<String, Object>();
        user.put("id", authUser.getId());
        user.put("username", authUser.getUsername());
        user.put("displayName", authUser.getDisplayName());
        result.put("user", user);
        result.put("menus", authUser.getMenus());
        result.put("permissions", authUser.getPermissions());
        return result;
    }

    private int asInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }
}
