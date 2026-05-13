package com.example.familymanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.AssetQuery;
import com.example.familymanage.dto.AssetSummary;
import com.example.familymanage.dto.StockChangeRequest;
import com.example.familymanage.entity.FamilyAsset;
import com.example.familymanage.service.FamilyAssetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class FamilyAssetController {

    private final FamilyAssetService familyAssetService;

    public FamilyAssetController(FamilyAssetService familyAssetService) {
        this.familyAssetService = familyAssetService;
    }

    @GetMapping
    public ApiResponse<IPage<FamilyAsset>> list(AssetQuery query) {
        return ApiResponse.ok(familyAssetService.queryAssets(query));
    }

    @GetMapping("/summary")
    public ApiResponse<AssetSummary> summary() {
        return ApiResponse.ok(familyAssetService.getSummary());
    }

    @GetMapping("/options")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.ok(familyAssetService.getOptions());
    }

    @GetMapping("/{id}")
    public ApiResponse<FamilyAsset> get(@PathVariable Long id) {
        return ApiResponse.ok(familyAssetService.getById(id));
    }

    @PostMapping
    public ApiResponse<FamilyAsset> create(@RequestBody FamilyAsset asset) {
        return ApiResponse.ok(familyAssetService.createAsset(asset));
    }

    @PutMapping("/{id}")
    public ApiResponse<FamilyAsset> update(@PathVariable Long id, @RequestBody FamilyAsset asset) {
        return ApiResponse.ok(familyAssetService.updateAsset(id, asset));
    }

    @PatchMapping("/{id}/stock")
    public ApiResponse<FamilyAsset> changeStock(@PathVariable Long id, @RequestBody StockChangeRequest request) {
        return ApiResponse.ok(familyAssetService.changeStock(id, request.getDelta()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(familyAssetService.removeById(id));
    }
}
