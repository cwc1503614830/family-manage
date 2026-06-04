package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.KitchenCookRecordRequest;
import com.example.familymanage.dto.KitchenDishRequest;
import com.example.familymanage.dto.KitchenIngredientRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.KitchenManageService;
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
@RequestMapping("/api/kitchen")
public class KitchenManageController {

    private final KitchenManageService kitchenManageService;

    public KitchenManageController(KitchenManageService kitchenManageService) {
        this.kitchenManageService = kitchenManageService;
    }

    @GetMapping("/dishes")
    @RequirePermission("kitchen:dish")
    public ApiResponse<Map<String, Object>> dishes(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(kitchenManageService.listDishes(page, size));
    }

    @PostMapping("/dishes")
    @RequirePermission("kitchen:dish")
    public ApiResponse<Map<String, Object>> createDish(@RequestBody KitchenDishRequest request) {
        return ApiResponse.ok(kitchenManageService.createDish(request));
    }

    @PutMapping("/dishes/{id}")
    @RequirePermission("kitchen:dish")
    public ApiResponse<Map<String, Object>> updateDish(@PathVariable Long id, @RequestBody KitchenDishRequest request) {
        return ApiResponse.ok(kitchenManageService.updateDish(id, request));
    }

    @DeleteMapping("/dishes/{id}")
    @RequirePermission("kitchen:dish")
    public ApiResponse<Boolean> deleteDish(@PathVariable Long id) {
        return ApiResponse.ok(kitchenManageService.deleteDish(id));
    }

    @GetMapping("/ingredients")
    @RequirePermission("kitchen:ingredient")
    public ApiResponse<Map<String, Object>> ingredients(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(kitchenManageService.listIngredients(page, size));
    }

    @PostMapping("/ingredients")
    @RequirePermission("kitchen:ingredient")
    public ApiResponse<Map<String, Object>> createIngredient(@RequestBody KitchenIngredientRequest request) {
        return ApiResponse.ok(kitchenManageService.createIngredient(request));
    }

    @PutMapping("/ingredients/{id}")
    @RequirePermission("kitchen:ingredient")
    public ApiResponse<Map<String, Object>> updateIngredient(@PathVariable Long id,
                                                             @RequestBody KitchenIngredientRequest request) {
        return ApiResponse.ok(kitchenManageService.updateIngredient(id, request));
    }

    @DeleteMapping("/ingredients/{id}")
    @RequirePermission("kitchen:ingredient")
    public ApiResponse<Boolean> deleteIngredient(@PathVariable Long id) {
        return ApiResponse.ok(kitchenManageService.deleteIngredient(id));
    }

    @GetMapping("/cook-records")
    @RequirePermission("kitchen:cook")
    public ApiResponse<Map<String, Object>> cookRecords(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) Boolean pendingOnly,
                                                        @RequestParam(required = false) Boolean mine) {
        return ApiResponse.ok(kitchenManageService.listCookRecords(page, size, pendingOnly, mine));
    }

    @PostMapping("/cook-records")
    @RequirePermission("kitchen:dish")
    public ApiResponse<Map<String, Object>> createCookRecord(@RequestBody KitchenCookRecordRequest request) {
        return ApiResponse.ok(kitchenManageService.createCookRecord(request));
    }

    @PostMapping("/cook-records/order")
    @RequirePermission("kitchen:dish")
    public ApiResponse<List<Map<String, Object>>> orderFromCart(@RequestBody KitchenCookRecordRequest request) {
        return ApiResponse.ok(kitchenManageService.orderFromCart(request));
    }

    @PutMapping("/cook-records/{id}")
    @RequirePermission("kitchen:cook")
    public ApiResponse<Map<String, Object>> updateCookRecord(@PathVariable Long id,
                                                             @RequestBody KitchenCookRecordRequest request) {
        return ApiResponse.ok(kitchenManageService.updateCookRecord(id, request));
    }

    @PutMapping("/cook-records/{id}/complete")
    @RequirePermission("kitchen:cook")
    public ApiResponse<Map<String, Object>> completeCookRecord(@PathVariable Long id) {
        return ApiResponse.ok(kitchenManageService.completeCookRecord(id));
    }

    @DeleteMapping("/cook-records/{id}")
    @RequirePermission("kitchen:cook")
    public ApiResponse<Boolean> deleteCookRecord(@PathVariable Long id) {
        return ApiResponse.ok(kitchenManageService.deleteCookRecord(id));
    }
}
