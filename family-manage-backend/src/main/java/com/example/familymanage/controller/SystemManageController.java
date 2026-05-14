package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.MenuSaveRequest;
import com.example.familymanage.dto.RoleSaveRequest;
import com.example.familymanage.dto.UserSaveRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.SystemManageService;
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
@RequestMapping("/api/system")
public class SystemManageController {

    private final SystemManageService systemManageService;

    public SystemManageController(SystemManageService systemManageService) {
        this.systemManageService = systemManageService;
    }

    @GetMapping("/users")
    @RequirePermission("system:user")
    public ApiResponse<List<Map<String, Object>>> users() {
        return ApiResponse.ok(systemManageService.listUsers());
    }

    @PostMapping("/users")
    @RequirePermission("system:user")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody UserSaveRequest request) {
        return ApiResponse.ok(systemManageService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @RequirePermission("system:user")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody UserSaveRequest request) {
        return ApiResponse.ok(systemManageService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    @RequirePermission("system:user")
    public ApiResponse<Boolean> deleteUser(@PathVariable Long id) {
        return ApiResponse.ok(systemManageService.deleteUser(id));
    }

    @GetMapping("/roles")
    @RequirePermission("system:role")
    public ApiResponse<List<Map<String, Object>>> roles() {
        return ApiResponse.ok(systemManageService.listRoles());
    }

    @PostMapping("/roles")
    @RequirePermission("system:role")
    public ApiResponse<Map<String, Object>> createRole(@RequestBody RoleSaveRequest request) {
        return ApiResponse.ok(systemManageService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    @RequirePermission("system:role")
    public ApiResponse<Map<String, Object>> updateRole(@PathVariable Long id, @RequestBody RoleSaveRequest request) {
        return ApiResponse.ok(systemManageService.updateRole(id, request));
    }

    @DeleteMapping("/roles/{id}")
    @RequirePermission("system:role")
    public ApiResponse<Boolean> deleteRole(@PathVariable Long id) {
        return ApiResponse.ok(systemManageService.deleteRole(id));
    }

    @GetMapping("/menus")
    @RequirePermission("system:menu")
    public ApiResponse<List<Map<String, Object>>> menus() {
        return ApiResponse.ok(systemManageService.listMenus());
    }

    @PostMapping("/menus")
    @RequirePermission("system:menu")
    public ApiResponse<Map<String, Object>> createMenu(@RequestBody MenuSaveRequest request) {
        return ApiResponse.ok(systemManageService.createMenu(request));
    }

    @PutMapping("/menus/{id}")
    @RequirePermission("system:menu")
    public ApiResponse<Map<String, Object>> updateMenu(@PathVariable Long id, @RequestBody MenuSaveRequest request) {
        return ApiResponse.ok(systemManageService.updateMenu(id, request));
    }

    @DeleteMapping("/menus/{id}")
    @RequirePermission("system:menu")
    public ApiResponse<Boolean> deleteMenu(@PathVariable Long id) {
        return ApiResponse.ok(systemManageService.deleteMenu(id));
    }
}
