package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.GoodsItemRequest;
import com.example.familymanage.dto.GoodsLocationRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.GoodsManageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class GoodsManageController {

    private final GoodsManageService goodsManageService;

    public GoodsManageController(GoodsManageService goodsManageService) {
        this.goodsManageService = goodsManageService;
    }

    @GetMapping("/items")
    @RequirePermission("goods:item")
    public ApiResponse<Map<String, Object>> items(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(goodsManageService.listItems(page, size));
    }

    @PostMapping("/items")
    @RequirePermission("goods:item")
    public ApiResponse<Map<String, Object>> createItem(@RequestBody GoodsItemRequest request) {
        return ApiResponse.ok(goodsManageService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @RequirePermission("goods:item")
    public ApiResponse<Map<String, Object>> updateItem(@PathVariable Long id, @RequestBody GoodsItemRequest request) {
        return ApiResponse.ok(goodsManageService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("goods:item")
    public ApiResponse<Boolean> deleteItem(@PathVariable Long id) {
        return ApiResponse.ok(goodsManageService.deleteItem(id));
    }

    @GetMapping("/locations")
    @RequirePermission("goods:location")
    public ApiResponse<List<Map<String, Object>>> locations() {
        return ApiResponse.ok(goodsManageService.listLocations());
    }

    @PostMapping("/locations")
    @RequirePermission("goods:location")
    public ApiResponse<Map<String, Object>> createLocation(@RequestBody GoodsLocationRequest request) {
        return ApiResponse.ok(goodsManageService.createLocation(request));
    }

    @PutMapping("/locations/{id}")
    @RequirePermission("goods:location")
    public ApiResponse<Map<String, Object>> updateLocation(@PathVariable Long id, @RequestBody GoodsLocationRequest request) {
        return ApiResponse.ok(goodsManageService.updateLocation(id, request));
    }

    @DeleteMapping("/locations/{id}")
    @RequirePermission("goods:location")
    public ApiResponse<Boolean> deleteLocation(@PathVariable Long id) {
        return ApiResponse.ok(goodsManageService.deleteLocation(id));
    }

    @GetMapping("/options")
    @RequirePermission("goods:item")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.ok(goodsManageService.getOptions());
    }
}
