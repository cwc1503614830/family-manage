package com.example.familymanage.service;

import com.example.familymanage.dto.TravelDomesticNoteRequest;
import com.example.familymanage.dto.TravelDomesticRequest;
import com.example.familymanage.dto.TravelWuhanSpotRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TravelManageService {

    private static final Set<String> SPOT_TYPES = new HashSet<String>(Arrays.asList("室内", "室外"));

    private final JdbcTemplate jdbcTemplate;

    public TravelManageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listWuhanSpots() {
        return jdbcTemplate.queryForList("SELECT id, spot_name, spot_type, checked_in, remark, created_at, updated_at "
                + "FROM travel_wuhan_spot ORDER BY checked_in ASC, updated_at DESC, id DESC");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createWuhanSpot(TravelWuhanSpotRequest request) {
        validateWuhanSpot(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO travel_wuhan_spot (spot_name, spot_type, checked_in, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.getSpotName().trim());
            statement.setString(2, normalizeSpotType(request.getSpotType()));
            statement.setInt(3, normalizeCheckedIn(request.getCheckedIn()));
            statement.setString(4, request.getRemark());
            return statement;
        }, keyHolder);
        return getWuhanSpot(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateWuhanSpot(Long id, TravelWuhanSpotRequest request) {
        validateWuhanSpot(request);
        jdbcTemplate.update("UPDATE travel_wuhan_spot SET spot_name = ?, spot_type = ?, checked_in = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                request.getSpotName().trim(), normalizeSpotType(request.getSpotType()),
                normalizeCheckedIn(request.getCheckedIn()), request.getRemark(), id);
        return getWuhanSpot(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWuhanSpot(Long id) {
        jdbcTemplate.update("DELETE FROM travel_wuhan_spot WHERE id = ?", id);
        return true;
    }

    public List<Map<String, Object>> listDomestic() {
        return jdbcTemplate.queryForList("SELECT d.id, d.city_id, d.province, d.city, d.cost, "
                + "n.id note_id, n.title note_title, n.updated_at note_updated_at, d.created_at, d.updated_at "
                + "FROM travel_domestic d "
                + "LEFT JOIN travel_domestic_note n ON n.domestic_id = d.id "
                + "ORDER BY d.updated_at DESC, d.id DESC");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createDomestic(TravelDomesticRequest request) {
        Map<String, Object> city = resolveCity(request.getCityId());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO travel_domestic (city_id, province, city, cost) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, request.getCityId());
            statement.setString(2, String.valueOf(city.get("province")));
            statement.setString(3, String.valueOf(city.get("city")));
            statement.setBigDecimal(4, normalizedCost(request.getCost()));
            return statement;
        }, keyHolder);
        return getDomestic(keyHolder.getKey().longValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateDomestic(Long id, TravelDomesticRequest request) {
        Map<String, Object> city = resolveCity(request.getCityId());
        jdbcTemplate.update("UPDATE travel_domestic SET city_id = ?, province = ?, city = ?, cost = ?, updated_at = NOW() WHERE id = ?",
                request.getCityId(), city.get("province"), city.get("city"), normalizedCost(request.getCost()), id);
        return getDomestic(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDomestic(Long id) {
        jdbcTemplate.update("DELETE FROM travel_domestic_note WHERE domestic_id = ?", id);
        jdbcTemplate.update("DELETE FROM travel_domestic WHERE id = ?", id);
        return true;
    }

    public Map<String, Object> getDomesticNote(Long domesticId) {
        ensureDomesticExists(domesticId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, domestic_id, title, content, created_at, updated_at FROM travel_domestic_note WHERE domestic_id = ?",
                domesticId);
        if (rows.isEmpty()) {
            Map<String, Object> empty = new HashMap<String, Object>();
            empty.put("domestic_id", domesticId);
            empty.put("title", "");
            empty.put("content", "");
            return empty;
        }
        return rows.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveDomesticNote(Long domesticId, TravelDomesticNoteRequest request) {
        ensureDomesticExists(domesticId);
        String title = request == null || request.getTitle() == null ? "" : request.getTitle().trim();
        String content = request == null || request.getContent() == null ? "" : request.getContent();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM travel_domestic_note WHERE domestic_id = ?", Integer.class, domesticId);
        if (count != null && count > 0) {
            jdbcTemplate.update("UPDATE travel_domestic_note SET title = ?, content = ?, updated_at = NOW() WHERE domestic_id = ?",
                    title, content, domesticId);
        } else {
            jdbcTemplate.update("INSERT INTO travel_domestic_note (domestic_id, title, content) VALUES (?, ?, ?)",
                    domesticId, title, content);
        }
        return getDomesticNote(domesticId);
    }

    public Map<String, Object> getOptions(String province) {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("provinces", jdbcTemplate.queryForList("SELECT DISTINCT province FROM travel_city ORDER BY province"));
        if (StringUtils.hasText(province)) {
            options.put("cities", jdbcTemplate.queryForList(
                    "SELECT id, province, city FROM travel_city WHERE province = ? ORDER BY city", province.trim()));
        } else {
            options.put("cities", jdbcTemplate.queryForList("SELECT id, province, city FROM travel_city ORDER BY province, city"));
        }
        options.put("spotTypes", Arrays.asList("室内", "室外"));
        return options;
    }

    private Map<String, Object> getWuhanSpot(Long id) {
        return jdbcTemplate.queryForMap("SELECT id, spot_name, spot_type, checked_in, remark, created_at, updated_at "
                + "FROM travel_wuhan_spot WHERE id = ?", id);
    }

    private Map<String, Object> getDomestic(Long id) {
        return jdbcTemplate.queryForMap("SELECT d.id, d.city_id, d.province, d.city, d.cost, "
                + "n.id note_id, n.title note_title, n.updated_at note_updated_at, d.created_at, d.updated_at "
                + "FROM travel_domestic d LEFT JOIN travel_domestic_note n ON n.domestic_id = d.id WHERE d.id = ?", id);
    }

    private Map<String, Object> resolveCity(Long cityId) {
        if (cityId == null) {
            throw new IllegalArgumentException("请选择城市");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, province, city FROM travel_city WHERE id = ?", cityId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("所选城市不存在");
        }
        return rows.get(0);
    }

    private void ensureDomesticExists(Long domesticId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM travel_domestic WHERE id = ?", Integer.class, domesticId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("国内游记录不存在");
        }
    }

    private void validateWuhanSpot(TravelWuhanSpotRequest request) {
        if (request == null || !StringUtils.hasText(request.getSpotName())) {
            throw new IllegalArgumentException("请输入景点名称");
        }
        if (!StringUtils.hasText(request.getSpotType())) {
            throw new IllegalArgumentException("请选择景点类型");
        }
        if (!SPOT_TYPES.contains(normalizeSpotType(request.getSpotType()))) {
            throw new IllegalArgumentException("景点类型仅支持室内或室外");
        }
    }

    private String normalizeSpotType(String spotType) {
        return spotType == null ? "室内" : spotType.trim();
    }

    private int normalizeCheckedIn(Integer checkedIn) {
        return checkedIn != null && checkedIn == 1 ? 1 : 0;
    }

    private BigDecimal normalizedCost(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
