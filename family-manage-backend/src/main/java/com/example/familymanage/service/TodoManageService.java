package com.example.familymanage.service;

import com.example.familymanage.dto.TodoItemRequest;
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
public class TodoManageService {

    private final JdbcTemplate jdbcTemplate;

    public TodoManageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> listItems(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM todo_item", Long.class);
        if (total == null) {
            total = 0L;
        }
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT id, item_title, due_date, is_done, remark, created_at, updated_at "
                        + "FROM todo_item ORDER BY (is_done = 1) ASC, due_date IS NULL ASC, due_date ASC, id DESC LIMIT ? OFFSET ?",
                safeSize, offset);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createItem(TodoItemRequest request) {
        validateItem(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO todo_item (item_title, due_date, is_done, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getItemTitle().trim());
            statement.setDate(2, toDate(request.getDueDate()));
            statement.setInt(3, normalizeIsDone(request.getIsDone()));
            statement.setString(4, request.getRemark());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("保存代办事项失败，请重试");
        }
        return getItem(key.longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateItem(Long id, TodoItemRequest request) {
        validateItem(request);
        jdbcTemplate.update(
                "UPDATE todo_item SET item_title = ?, due_date = ?, is_done = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getItemTitle().trim(), toDate(request.getDueDate()), normalizeIsDone(request.getIsDone()),
                request.getRemark(), id);
        return getItem(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteItem(Long id) {
        jdbcTemplate.update("DELETE FROM todo_item WHERE id = ?", id);
        return true;
    }

    private Map<String, Object> getItem(Long id) {
        return jdbcTemplate.queryForMap(
                "SELECT id, item_title, due_date, is_done, remark, created_at, updated_at FROM todo_item WHERE id = ?", id);
    }

    private void validateItem(TodoItemRequest request) {
        if (request == null || !StringUtils.hasText(request.getItemTitle())) {
            throw new IllegalArgumentException("请输入事项");
        }
    }

    private int normalizeIsDone(Integer isDone) {
        return isDone != null && isDone == 1 ? 1 : 0;
    }

    private Date toDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(value.trim()));
    }
}
