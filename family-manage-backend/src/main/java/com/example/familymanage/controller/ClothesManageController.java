package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.ClothesItemRequest;
import com.example.familymanage.dto.ClothesLocationRequest;
import com.example.familymanage.dto.ClothesTypeRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.ClothesManageService;
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
@RequestMapping("/api/clothes")
public class ClothesManageController {

    private final ClothesManageService clothesManageService;

    public ClothesManageController(ClothesManageService clothesManageService) {
        this.clothesManageService = clothesManageService;
    }

    @GetMapping("/items")
    @RequirePermission("clothes:item")
    public ApiResponse<List<Map<String, Object>>> items() {
        return ApiResponse.ok(clothesManageService.listItems());
    }

    @PostMapping("/items")
    @RequirePermission("clothes:item")
    public ApiResponse<Map<String, Object>> createItem(@RequestBody ClothesItemRequest request) {
        return ApiResponse.ok(clothesManageService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @RequirePermission("clothes:item")
    public ApiResponse<Map<String, Object>> updateItem(@PathVariable Long id, @RequestBody ClothesItemRequest request) {
        return ApiResponse.ok(clothesManageService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("clothes:item")
    public ApiResponse<Boolean> deleteItem(@PathVariable Long id) {
        return ApiResponse.ok(clothesManageService.deleteItem(id));
    }

    @GetMapping("/types")
    @RequirePermission("clothes:type")
    public ApiResponse<List<Map<String, Object>>> types() {
        return ApiResponse.ok(clothesManageService.listTypes());
    }

    @PostMapping("/types")
    @RequirePermission("clothes:type")
    public ApiResponse<Map<String, Object>> createType(@RequestBody ClothesTypeRequest request) {
        return ApiResponse.ok(clothesManageService.createType(request));
    }

    @PutMapping("/types/{id}")
    @RequirePermission("clothes:type")
    public ApiResponse<Map<String, Object>> updateType(@PathVariable Long id, @RequestBody ClothesTypeRequest request) {
        return ApiResponse.ok(clothesManageService.updateType(id, request));
    }

    @DeleteMapping("/types/{id}")
    @RequirePermission("clothes:type")
    public ApiResponse<Boolean> deleteType(@PathVariable Long id) {
        return ApiResponse.ok(clothesManageService.deleteType(id));
    }

    @GetMapping("/locations")
    @RequirePermission("clothes:location")
    public ApiResponse<List<Map<String, Object>>> locations() {
        return ApiResponse.ok(clothesManageService.listLocations());
    }

    @PostMapping("/locations")
    @RequirePermission("clothes:location")
    public ApiResponse<Map<String, Object>> createLocation(@RequestBody ClothesLocationRequest request) {
        return ApiResponse.ok(clothesManageService.createLocation(request));
    }

    @PutMapping("/locations/{id}")
    @RequirePermission("clothes:location")
    public ApiResponse<Map<String, Object>> updateLocation(@PathVariable Long id, @RequestBody ClothesLocationRequest request) {
        return ApiResponse.ok(clothesManageService.updateLocation(id, request));
    }

    @DeleteMapping("/locations/{id}")
    @RequirePermission("clothes:location")
    public ApiResponse<Boolean> deleteLocation(@PathVariable Long id) {
        return ApiResponse.ok(clothesManageService.deleteLocation(id));
    }

    @GetMapping("/options")
    @RequirePermission("clothes:item")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.ok(clothesManageService.getOptions());
    }
}
