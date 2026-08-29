package com.bitalep.controller;

import com.bitalep.dto.ApiSuccessResponse;
import com.bitalep.dto.MiscDtos;
import com.bitalep.service.impl.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiSuccessResponse<MiscDtos.DashboardResponse> get() {
        return ApiSuccessResponse.of(dashboardService.stats());
    }
}
