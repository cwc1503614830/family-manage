package com.example.familymanage.service;

import com.example.familymanage.dto.AlbumPhotoRequest;
import com.example.familymanage.dto.AlbumThemeRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlbumManageService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PHOTO_ORDER_BY = " ORDER BY COALESCE(p.taken_at, p.created_at) DESC, p.id DESC";
    private static final String PHOTO_SELECT = "SELECT p.id, p.image_name, p.image_url, p.theme_id, t.theme_name, p.remark, "
            + "p.taken_at, p.created_at, p.updated_at FROM album_photo p "
            + "LEFT JOIN album_theme t ON t.id = p.theme_id";

    private final JdbcTemplate jdbcTemplate;
    private final MinioService minioService;

    public AlbumManageService(JdbcTemplate jdbcTemplate, MinioService minioService) {
        this.jdbcTemplate = jdbcTemplate;
        this.minioService = minioService;
    }

    public List<Map<String, Object>> listThemes() {
        List<Map<String, Object>> themes = jdbcTemplate.queryForList(
                "SELECT id, theme_name, remark, created_at, updated_at FROM album_theme ORDER BY updated_at DESC, id DESC");
        themes.forEach(this::attachThemeStats);
        return themes;
    }

    public Map<String, Object> listCards() {
        List<Map<String, Object>> cards = jdbcTemplate.queryForList(
                "SELECT t.id, t.theme_name, t.remark, t.created_at, t.updated_at, "
                        + "(SELECT COUNT(*) FROM album_photo p WHERE p.theme_id = t.id) AS photo_count, "
                        + "(SELECT p.image_url FROM album_photo p WHERE p.theme_id = t.id "
                        + "ORDER BY COALESCE(p.taken_at, p.created_at) DESC, p.id DESC LIMIT 1) AS cover_url "
                        + "FROM album_theme t ORDER BY t.updated_at DESC, t.id DESC");
        cards.forEach(this::normalizeCardCover);
        Long albumCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM album_theme", Long.class);
        Long photoCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM album_photo", Long.class);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("cards", cards);
        result.put("albumCount", albumCount == null ? 0L : albumCount);
        result.put("photoCount", photoCount == null ? 0L : photoCount);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createTheme(AlbumThemeRequest request) {
        validateTheme(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO album_theme (theme_name, remark) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getThemeName().trim());
            statement.setString(2, request.getRemark());
            return statement;
        }, keyHolder);
        return getTheme(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateTheme(Long id, AlbumThemeRequest request) {
        validateTheme(request);
        jdbcTemplate.update("UPDATE album_theme SET theme_name = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getThemeName().trim(), request.getRemark(), id);
        return getTheme(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTheme(Long id) {
        jdbcTemplate.update("DELETE FROM album_photo WHERE theme_id = ?", id);
        jdbcTemplate.update("DELETE FROM album_theme WHERE id = ?", id);
        return true;
    }

    public Map<String, Object> listPhotos(int page, int size, Long themeId) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        String countSql = "SELECT COUNT(*) FROM album_photo";
        String listSql = PHOTO_SELECT;
        if (themeId != null) {
            countSql += " WHERE theme_id = ?";
            listSql += " WHERE p.theme_id = ?" + PHOTO_ORDER_BY + " LIMIT ? OFFSET ?";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class, themeId);
            if (total == null) {
                total = 0L;
            }
            List<Map<String, Object>> records = jdbcTemplate.queryForList(listSql, themeId, safeSize, offset);
            records.forEach(this::normalizePhotoImageUrl);
            return buildPageResult(records, total, safePage, safeSize);
        }
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);
        if (total == null) {
            total = 0L;
        }
        listSql += PHOTO_ORDER_BY + " LIMIT ? OFFSET ?";
        List<Map<String, Object>> records = jdbcTemplate.queryForList(listSql, safeSize, offset);
        records.forEach(this::normalizePhotoImageUrl);
        return buildPageResult(records, total, safePage, safeSize);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPhoto(AlbumPhotoRequest request) {
        validatePhoto(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO album_photo (image_name, image_url, theme_id, remark, taken_at) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getImageName().trim());
            statement.setString(2, normalizeStoredImageUrl(request.getImageUrl()));
            statement.setLong(3, request.getThemeId());
            statement.setString(4, request.getRemark());
            Timestamp takenAt = toTimestamp(request.getTakenAt());
            if (takenAt == null) {
                statement.setNull(5, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(5, takenAt);
            }
            return statement;
        }, keyHolder);
        touchThemeUpdatedAt(request.getThemeId());
        return getPhoto(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> createPhotosBatch(List<AlbumPhotoRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("请至少上传一张图片");
        }
        Long themeId = requests.get(0).getThemeId();
        for (AlbumPhotoRequest request : requests) {
            if (request.getThemeId() == null || !request.getThemeId().equals(themeId)) {
                throw new IllegalArgumentException("批量上传的图片必须属于同一主题");
            }
        }
        List<Map<String, Object>> created = new java.util.ArrayList<Map<String, Object>>();
        for (AlbumPhotoRequest request : requests) {
            created.add(createPhoto(request));
        }
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updatePhoto(Long id, AlbumPhotoRequest request) {
        validatePhoto(request);
        Map<String, Object> existing = getPhoto(id);
        jdbcTemplate.update("UPDATE album_photo SET image_name = ?, image_url = ?, theme_id = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getImageName().trim(), normalizeStoredImageUrl(request.getImageUrl()),
                request.getThemeId(), request.getRemark(), id);
        touchThemeUpdatedAt(request.getThemeId());
        Object oldThemeId = existing.get("theme_id");
        if (oldThemeId != null && !request.getThemeId().equals(((Number) oldThemeId).longValue())) {
            touchThemeUpdatedAt(((Number) oldThemeId).longValue());
        }
        return getPhoto(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePhoto(Long id) {
        Map<String, Object> photo = getPhoto(id);
        jdbcTemplate.update("DELETE FROM album_photo WHERE id = ?", id);
        Object themeId = photo.get("theme_id");
        if (themeId != null) {
            touchThemeUpdatedAt(((Number) themeId).longValue());
        }
        return true;
    }

    private Map<String, Object> buildPageResult(List<Map<String, Object>> records, Long total, int page, int size) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private Map<String, Object> getTheme(Long id) {
        Map<String, Object> theme = jdbcTemplate.queryForMap(
                "SELECT id, theme_name, remark, created_at, updated_at FROM album_theme WHERE id = ?", id);
        attachThemeStats(theme);
        return theme;
    }

    private Map<String, Object> getPhoto(Long id) {
        Map<String, Object> photo = jdbcTemplate.queryForMap(
                PHOTO_SELECT + " WHERE p.id = ?", id);
        normalizePhotoImageUrl(photo);
        return photo;
    }

    private void attachThemeStats(Map<String, Object> theme) {
        if (theme == null || theme.get("id") == null) {
            return;
        }
        Long themeId = ((Number) theme.get("id")).longValue();
        Long photoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM album_photo WHERE theme_id = ?", Long.class, themeId);
        theme.put("photo_count", photoCount == null ? 0L : photoCount);
        List<Map<String, Object>> covers = jdbcTemplate.queryForList(
                "SELECT image_url FROM album_photo WHERE theme_id = ? ORDER BY COALESCE(taken_at, created_at) DESC, id DESC LIMIT 1",
                themeId);
        if (covers.isEmpty()) {
            theme.put("cover_url", "");
        } else {
            Object coverUrl = covers.get(0).get("image_url");
            theme.put("cover_url", minioService.normalizePublicUrl(coverUrl == null ? "" : String.valueOf(coverUrl)));
        }
    }

    private void normalizeCardCover(Map<String, Object> card) {
        if (card == null || !card.containsKey("cover_url")) {
            return;
        }
        Object coverUrl = card.get("cover_url");
        card.put("cover_url", minioService.normalizePublicUrl(coverUrl == null ? "" : String.valueOf(coverUrl)));
    }

    private void normalizePhotoImageUrl(Map<String, Object> item) {
        if (item == null || !item.containsKey("image_url")) {
            return;
        }
        Object imageUrl = item.get("image_url");
        item.put("image_url", minioService.normalizePublicUrl(imageUrl == null ? "" : String.valueOf(imageUrl)));
    }

    private String normalizeStoredImageUrl(String imageUrl) {
        return minioService.normalizePublicUrl(imageUrl);
    }

    private void touchThemeUpdatedAt(Long themeId) {
        if (themeId == null) {
            return;
        }
        jdbcTemplate.update("UPDATE album_theme SET updated_at = NOW() WHERE id = ?", themeId);
    }

    private void validateTheme(AlbumThemeRequest request) {
        if (request == null || !StringUtils.hasText(request.getThemeName())) {
            throw new IllegalArgumentException("主题名称不能为空");
        }
    }

    private void validatePhoto(AlbumPhotoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (!StringUtils.hasText(request.getImageName())) {
            request.setImageName(defaultImageName(request.getImageUrl()));
        }
        if (!StringUtils.hasText(request.getImageUrl())) {
            throw new IllegalArgumentException("请上传图片");
        }
        if (request.getThemeId() == null) {
            throw new IllegalArgumentException("请选择相册主题");
        }
        Integer themeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM album_theme WHERE id = ?", Integer.class, request.getThemeId());
        if (themeCount == null || themeCount == 0) {
            throw new IllegalArgumentException("相册主题不存在");
        }
    }

    private String defaultImageName(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return "照片";
        }
        String path = imageUrl;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        int slashIndex = path.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < path.length() - 1) {
            path = path.substring(slashIndex + 1);
        }
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0) {
            path = path.substring(0, dotIndex);
        }
        return StringUtils.hasText(path) ? path : "照片";
    }

    private Timestamp toTimestamp(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim().replace('T', ' ');
        if (text.length() == 10) {
            text += " 00:00:00";
        }
        if (text.length() == 16) {
            text += ":00";
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(text, DATE_TIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
