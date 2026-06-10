package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.TodoItemRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.TodoManageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/todo")
public class TodoManageController {

    private final TodoManageService todoManageService;

    public TodoManageController(TodoManageService todoManageService) {
        this.todoManageService = todoManageService;
    }

    @GetMapping("/items")
    @RequirePermission("todo:item")
    public ApiResponse<Map<String, Object>> items(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(todoManageService.listItems(page, size));
    }

    @PostMapping("/items")
    @RequirePermission("todo:item")
    public ApiResponse<Map<String, Object>> createItem(@RequestBody TodoItemRequest request) {
        return ApiResponse.ok(todoManageService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @RequirePermission("todo:item")
    public ApiResponse<Map<String, Object>> updateItem(@PathVariable Long id, @RequestBody TodoItemRequest request) {
        return ApiResponse.ok(todoManageService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("todo:item")
    public ApiResponse<Boolean> deleteItem(@PathVariable Long id) {
        return ApiResponse.ok(todoManageService.deleteItem(id));
    }
}
