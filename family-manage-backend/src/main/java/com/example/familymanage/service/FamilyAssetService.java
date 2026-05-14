package com.example.familymanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.familymanage.dto.AssetQuery;
import com.example.familymanage.dto.AssetSummary;
import com.example.familymanage.entity.FamilyAsset;

import java.math.BigDecimal;
import java.util.Map;

public interface FamilyAssetService extends IService<FamilyAsset> {

    IPage<FamilyAsset> queryAssets(AssetQuery query);

    AssetSummary getSummary();

    FamilyAsset createAsset(FamilyAsset asset);

    FamilyAsset updateAsset(Long id, FamilyAsset asset);

    FamilyAsset changeStock(Long id, BigDecimal delta);

    Map<String, Object> getOptions();
}
