package com.example.familymanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.AssetQuery;
import com.example.familymanage.dto.StockChangeRequest;
import com.example.familymanage.entity.FamilyAsset;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.DashboardService;
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
    private final DashboardService dashboardService;

    public FamilyAssetController(FamilyAssetService familyAssetService, DashboardService dashboardService) {
        this.familyAssetService = familyAssetService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @RequirePermission("asset:view")
    public ApiResponse<IPage<FamilyAsset>> list(AssetQuery query) {
        return ApiResponse.ok(familyAssetService.queryAssets(query));
    }

    @GetMapping("/summary")
    @RequirePermission("asset:view")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }

    @GetMapping("/options")
    @RequirePermission("asset:view")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.ok(familyAssetService.getOptions());
    }

    @GetMapping("/{id}")
    @RequirePermission("asset:view")
    public ApiResponse<FamilyAsset> get(@PathVariable Long id) {
        return ApiResponse.ok(familyAssetService.getById(id));
    }

    @PostMapping
    @RequirePermission("asset:view")
    public ApiResponse<FamilyAsset> create(@RequestBody FamilyAsset asset) {
        return ApiResponse.ok(familyAssetService.createAsset(asset));
    }

    @PutMapping("/{id}")
    @RequirePermission("asset:view")
    public ApiResponse<FamilyAsset> update(@PathVariable Long id, @RequestBody FamilyAsset asset) {
        return ApiResponse.ok(familyAssetService.updateAsset(id, asset));
    }

    @PatchMapping("/{id}/stock")
    @RequirePermission("asset:view")
    public ApiResponse<FamilyAsset> changeStock(@PathVariable Long id, @RequestBody StockChangeRequest request) {
        return ApiResponse.ok(familyAssetService.changeStock(id, request.getDelta()));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("asset:view")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(familyAssetService.removeById(id));
    }
}
