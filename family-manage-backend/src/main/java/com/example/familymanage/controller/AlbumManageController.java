package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.AlbumPhotoRequest;
import com.example.familymanage.dto.AlbumThemeRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.AlbumManageService;
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
@RequestMapping("/api/album")
public class AlbumManageController {

    private final AlbumManageService albumManageService;

    public AlbumManageController(AlbumManageService albumManageService) {
        this.albumManageService = albumManageService;
    }

    @GetMapping("/themes")
    @RequirePermission("album:theme")
    public ApiResponse<List<Map<String, Object>>> themes() {
        return ApiResponse.ok(albumManageService.listThemes());
    }

    @PostMapping("/themes")
    @RequirePermission("album:theme")
    public ApiResponse<Map<String, Object>> createTheme(@RequestBody AlbumThemeRequest request) {
        return ApiResponse.ok(albumManageService.createTheme(request));
    }

    @PutMapping("/themes/{id}")
    @RequirePermission("album:theme")
    public ApiResponse<Map<String, Object>> updateTheme(@PathVariable Long id, @RequestBody AlbumThemeRequest request) {
        return ApiResponse.ok(albumManageService.updateTheme(id, request));
    }

    @DeleteMapping("/themes/{id}")
    @RequirePermission("album:theme")
    public ApiResponse<Boolean> deleteTheme(@PathVariable Long id) {
        return ApiResponse.ok(albumManageService.deleteTheme(id));
    }

    @GetMapping("/cards")
    @RequirePermission("album:photo")
    public ApiResponse<Map<String, Object>> cards() {
        return ApiResponse.ok(albumManageService.listCards());
    }

    @GetMapping("/photos")
    @RequirePermission("album:photo")
    public ApiResponse<Map<String, Object>> photos(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) Long themeId) {
        return ApiResponse.ok(albumManageService.listPhotos(page, size, themeId));
    }

    @PostMapping("/photos")
    @RequirePermission("album:photo")
    public ApiResponse<Map<String, Object>> createPhoto(@RequestBody AlbumPhotoRequest request) {
        return ApiResponse.ok(albumManageService.createPhoto(request));
    }

    @PostMapping("/photos/batch")
    @RequirePermission("album:photo")
    public ApiResponse<List<Map<String, Object>>> createPhotosBatch(@RequestBody List<AlbumPhotoRequest> requests) {
        return ApiResponse.ok(albumManageService.createPhotosBatch(requests));
    }

    @PutMapping("/photos/{id}")
    @RequirePermission("album:photo")
    public ApiResponse<Map<String, Object>> updatePhoto(@PathVariable Long id, @RequestBody AlbumPhotoRequest request) {
        return ApiResponse.ok(albumManageService.updatePhoto(id, request));
    }

    @DeleteMapping("/photos/{id}")
    @RequirePermission("album:photo")
    public ApiResponse<Boolean> deletePhoto(@PathVariable Long id) {
        return ApiResponse.ok(albumManageService.deletePhoto(id));
    }
}
