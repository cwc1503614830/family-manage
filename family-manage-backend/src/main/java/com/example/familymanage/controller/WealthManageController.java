package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.WealthAssetRequest;
import com.example.familymanage.dto.WealthTypeRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.WealthManageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wealth")
public class WealthManageController {

    private final WealthManageService wealthManageService;

    public WealthManageController(WealthManageService wealthManageService) {
        this.wealthManageService = wealthManageService;
    }

    @GetMapping("/assets")
    @RequirePermission("wealth:overview")
    public ApiResponse<List<Map<String, Object>>> assets() {
        return ApiResponse.ok(wealthManageService.listAssets());
    }

    @PostMapping("/assets")
    @RequirePermission("wealth:overview")
    public ApiResponse<Map<String, Object>> createAsset(@RequestBody WealthAssetRequest request) {
        return ApiResponse.ok(wealthManageService.createAsset(request));
    }

    @PutMapping("/assets/{id}")
    @RequirePermission("wealth:overview")
    public ApiResponse<Map<String, Object>> updateAsset(@PathVariable Long id, @RequestBody WealthAssetRequest request) {
        return ApiResponse.ok(wealthManageService.updateAsset(id, request));
    }

    @DeleteMapping("/assets/{id}")
    @RequirePermission("wealth:overview")
    public ApiResponse<Boolean> deleteAsset(@PathVariable Long id) {
        return ApiResponse.ok(wealthManageService.deleteAsset(id));
    }

    @GetMapping("/types")
    @RequirePermission("wealth:type")
    public ApiResponse<List<Map<String, Object>>> types() {
        return ApiResponse.ok(wealthManageService.listTypes());
    }

    @PostMapping("/types")
    @RequirePermission("wealth:type")
    public ApiResponse<Map<String, Object>> createType(@RequestBody WealthTypeRequest request) {
        return ApiResponse.ok(wealthManageService.createType(request));
    }

    @PutMapping("/types/{id}")
    @RequirePermission("wealth:type")
    public ApiResponse<Map<String, Object>> updateType(@PathVariable Long id, @RequestBody WealthTypeRequest request) {
        return ApiResponse.ok(wealthManageService.updateType(id, request));
    }

    @DeleteMapping("/types/{id}")
    @RequirePermission("wealth:type")
    public ApiResponse<Boolean> deleteType(@PathVariable Long id) {
        return ApiResponse.ok(wealthManageService.deleteType(id));
    }

    @GetMapping("/options")
    @RequirePermission("wealth:overview")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.ok(wealthManageService.getOptions());
    }
}
