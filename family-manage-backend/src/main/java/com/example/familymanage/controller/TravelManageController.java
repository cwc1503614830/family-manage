package com.example.familymanage.controller;

import com.example.familymanage.common.ApiResponse;
import com.example.familymanage.dto.TravelDomesticNoteRequest;
import com.example.familymanage.dto.TravelDomesticRequest;
import com.example.familymanage.dto.TravelWuhanSpotRequest;
import com.example.familymanage.security.RequirePermission;
import com.example.familymanage.service.TravelManageService;
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
@RequestMapping("/api/travel")
public class TravelManageController {

    private final TravelManageService travelManageService;

    public TravelManageController(TravelManageService travelManageService) {
        this.travelManageService = travelManageService;
    }

    @GetMapping("/wuhan/spots")
    @RequirePermission("travel:wuhan")
    public ApiResponse<List<Map<String, Object>>> wuhanSpots() {
        return ApiResponse.ok(travelManageService.listWuhanSpots());
    }

    @PostMapping("/wuhan/spots")
    @RequirePermission("travel:wuhan")
    public ApiResponse<Map<String, Object>> createWuhanSpot(@RequestBody TravelWuhanSpotRequest request) {
        return ApiResponse.ok(travelManageService.createWuhanSpot(request));
    }

    @PutMapping("/wuhan/spots/{id}")
    @RequirePermission("travel:wuhan")
    public ApiResponse<Map<String, Object>> updateWuhanSpot(@PathVariable Long id, @RequestBody TravelWuhanSpotRequest request) {
        return ApiResponse.ok(travelManageService.updateWuhanSpot(id, request));
    }

    @DeleteMapping("/wuhan/spots/{id}")
    @RequirePermission("travel:wuhan")
    public ApiResponse<Boolean> deleteWuhanSpot(@PathVariable Long id) {
        return ApiResponse.ok(travelManageService.deleteWuhanSpot(id));
    }

    @GetMapping("/domestic")
    @RequirePermission("travel:domestic")
    public ApiResponse<List<Map<String, Object>>> domesticList() {
        return ApiResponse.ok(travelManageService.listDomestic());
    }

    @PostMapping("/domestic")
    @RequirePermission("travel:domestic")
    public ApiResponse<Map<String, Object>> createDomestic(@RequestBody TravelDomesticRequest request) {
        return ApiResponse.ok(travelManageService.createDomestic(request));
    }

    @PutMapping("/domestic/{id}")
    @RequirePermission("travel:domestic")
    public ApiResponse<Map<String, Object>> updateDomestic(@PathVariable Long id, @RequestBody TravelDomesticRequest request) {
        return ApiResponse.ok(travelManageService.updateDomestic(id, request));
    }

    @DeleteMapping("/domestic/{id}")
    @RequirePermission("travel:domestic")
    public ApiResponse<Boolean> deleteDomestic(@PathVariable Long id) {
        return ApiResponse.ok(travelManageService.deleteDomestic(id));
    }

    @GetMapping("/domestic/{id}/note")
    @RequirePermission("travel:domestic")
    public ApiResponse<Map<String, Object>> getDomesticNote(@PathVariable Long id) {
        return ApiResponse.ok(travelManageService.getDomesticNote(id));
    }

    @PutMapping("/domestic/{id}/note")
    @RequirePermission("travel:domestic")
    public ApiResponse<Map<String, Object>> saveDomesticNote(@PathVariable Long id, @RequestBody TravelDomesticNoteRequest request) {
        return ApiResponse.ok(travelManageService.saveDomesticNote(id, request));
    }

    @GetMapping("/options")
    @RequirePermission("travel:domestic")
    public ApiResponse<Map<String, Object>> options(@RequestParam(required = false) String province) {
        return ApiResponse.ok(travelManageService.getOptions(province));
    }
}
