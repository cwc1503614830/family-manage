package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.LoginRequest;
import com.example.familymanage.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(authService.current());
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (token == null && authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        authService.logout(token);
        return ApiResponse.ok(true);
    }
}
