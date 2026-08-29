package com.bitalep.controller;

import com.bitalep.dto.ApiSuccessResponse;
import com.bitalep.dto.UserDtos;
import com.bitalep.entity.Department;
import com.bitalep.entity.UserRole;
import com.bitalep.service.impl.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiSuccessResponse<UserDtos.UserResponse> me() {
        return ApiSuccessResponse.of(userService.me());
    }

    @PutMapping("/me")
    public ApiSuccessResponse<UserDtos.UserResponse> updateMe(@Valid @RequestBody UserDtos.UpdateProfileRequest req) {
        return ApiSuccessResponse.of(userService.updateMe(req));
    }

    @GetMapping
    public ApiSuccessResponse<java.util.List<UserDtos.UserResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Department department
    ) {
        var result = userService.list(page, pageSize, sortBy, sortOrder, keyword, role, department);
        return ApiSuccessResponse.page(result.data(), result.meta());
    }

    @GetMapping("/{id}")
    public ApiSuccessResponse<UserDtos.UserResponse> get(@PathVariable UUID id) {
        return ApiSuccessResponse.of(userService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<UserDtos.UserResponse>> create(
            @Valid @RequestBody UserDtos.CreateUserRequest req,
            @RequestHeader(value = "X-Demo-Seed", required = false) String demoSeed
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(userService.create(req, demoSeed)));
    }

    @PutMapping("/{id}/role")
    public ApiSuccessResponse<UserDtos.UserResponse> role(
            @PathVariable UUID id,
            @RequestBody UserDtos.UpdateRoleRequest req
    ) {
        return ApiSuccessResponse.of(userService.updateRole(id, req.role()));
    }

    @PutMapping("/{id}/active")
    public ApiSuccessResponse<UserDtos.UserResponse> setActive(
            @PathVariable UUID id,
            @RequestBody UserDtos.SetActiveRequest req
    ) {
        return ApiSuccessResponse.of(userService.setActive(id, req.active()));
    }
}
