package com.bitalep.controller;

import com.bitalep.dto.ApiSuccessResponse;
import com.bitalep.dto.MiscDtos;
import com.bitalep.service.impl.CompanyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public ApiSuccessResponse<MiscDtos.CompanyResponse> get() {
        return ApiSuccessResponse.of(companyService.get());
    }

    @PutMapping
    public ApiSuccessResponse<MiscDtos.CompanyResponse> put(@RequestBody MiscDtos.UpdateCompanyRequest req) {
        return ApiSuccessResponse.of(companyService.update(req));
    }
}
