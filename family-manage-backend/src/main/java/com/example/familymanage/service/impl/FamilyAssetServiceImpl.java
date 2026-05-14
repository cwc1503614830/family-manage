package com.example.familymanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.familymanage.dto.AssetQuery;
import com.example.familymanage.dto.AssetSummary;
import com.example.familymanage.entity.FamilyAsset;
import com.example.familymanage.mapper.FamilyAssetMapper;
import com.example.familymanage.service.FamilyAssetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FamilyAssetServiceImpl extends ServiceImpl<FamilyAssetMapper, FamilyAsset> implements FamilyAssetService {

    @Override
    public IPage<FamilyAsset> queryAssets(AssetQuery query) {
        Long pageNo = query.getPage() == null || query.getPage() < 1 ? 1L : query.getPage();
        Long pageSize = query.getSize() == null || query.getSize() < 1 ? 20L : query.getSize();
        LambdaQueryWrapper<FamilyAsset> wrapper = buildQueryWrapper(query);
        wrapper.orderByAsc(FamilyAsset::getExpireDate).orderByDesc(FamilyAsset::getUpdatedAt);
        return page(new Page<FamilyAsset>(pageNo, pageSize), wrapper);
    }

    @Override
    public AssetSummary getSummary() {
        LocalDate today = LocalDate.now();
        AssetSummary summary = new AssetSummary();
        summary.setTotalAssets(count());
        summary.setTotalCategories(baseMapper.selectObjs(new QueryWrapper<FamilyAsset>()
                .select("distinct category")
                .isNotNull("category")
                .ne("category", "")).size());
        summary.setExpiredAssets(count(new LambdaQueryWrapper<FamilyAsset>()
                .lt(FamilyAsset::getExpireDate, today)));
        summary.setExpiringSoonAssets(count(new LambdaQueryWrapper<FamilyAsset>()
                .ge(FamilyAsset::getExpireDate, today)
                .le(FamilyAsset::getExpireDate, today.plusDays(30))));
        summary.setLowStockAssets(list().stream()
                .filter(asset -> safe(asset.getQuantity()).compareTo(safe(asset.getWarningQuantity())) <= 0)
                .count());
        BigDecimal totalQuantity = list().stream()
                .map(FamilyAsset::getQuantity)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalQuantity(totalQuantity);
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyAsset createAsset(FamilyAsset asset) {
        LocalDateTime now = LocalDateTime.now();
        asset.setId(null);
        asset.setQuantity(safe(asset.getQuantity()));
        asset.setWarningQuantity(safe(asset.getWarningQuantity()));
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        save(asset);
        return asset;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyAsset updateAsset(Long id, FamilyAsset asset) {
        FamilyAsset old = getById(id);
        if (old == null) {
            throw new IllegalArgumentException("资产不存在");
        }
        asset.setId(id);
        asset.setQuantity(safe(asset.getQuantity()));
        asset.setWarningQuantity(safe(asset.getWarningQuantity()));
        asset.setCreatedAt(old.getCreatedAt());
        asset.setUpdatedAt(LocalDateTime.now());
        updateById(asset);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyAsset changeStock(Long id, BigDecimal delta) {
        FamilyAsset asset = getById(id);
        if (asset == null) {
            throw new IllegalArgumentException("资产不存在");
        }
        BigDecimal nextQuantity = safe(asset.getQuantity()).add(safe(delta));
        if (nextQuantity.compareTo(BigDecimal.ZERO) < 0) {
            nextQuantity = BigDecimal.ZERO;
        }
        asset.setQuantity(nextQuantity);
        asset.setUpdatedAt(LocalDateTime.now());
        updateById(asset);
        return asset;
    }

    @Override
    public Map<String, Object> getOptions() {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("categories", baseMapper.selectObjs(new QueryWrapper<FamilyAsset>()
                .select("distinct category")
                .isNotNull("category")
                .ne("category", "")));
        options.put("locations", baseMapper.selectObjs(new QueryWrapper<FamilyAsset>()
                .select("distinct location")
                .isNotNull("location")
                .ne("location", "")));
        return options;
    }

    private LambdaQueryWrapper<FamilyAsset> buildQueryWrapper(AssetQuery query) {
        LambdaQueryWrapper<FamilyAsset> wrapper = new LambdaQueryWrapper<FamilyAsset>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(FamilyAsset::getName, keyword)
                    .or()
                    .like(FamilyAsset::getTags, keyword)
                    .or()
                    .like(FamilyAsset::getRemark, keyword)
                    .or()
                    .like(FamilyAsset::getLocation, keyword));
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(FamilyAsset::getCategory, query.getCategory().trim());
        }
        if (StringUtils.hasText(query.getLocation())) {
            wrapper.eq(FamilyAsset::getLocation, query.getLocation().trim());
        }
        return wrapper;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
