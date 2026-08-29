package com.bitalep.controller;

import com.bitalep.dto.ApiSuccessResponse;
import com.bitalep.dto.UserDtos;
import com.bitalep.service.impl.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiSuccessResponse<UserDtos.LoginResponse> login(@Valid @RequestBody UserDtos.LoginRequest req) {
        return ApiSuccessResponse.of(authService.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<UserDtos.LoginResponse>> register(
            @Valid @RequestBody UserDtos.RegisterRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(authService.register(req)));
    }

    @PostMapping("/refresh")
    public ApiSuccessResponse<UserDtos.TokenPairResponse> refresh(@Valid @RequestBody UserDtos.RefreshRequest req) {
        return ApiSuccessResponse.of(authService.refresh(req.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) UserDtos.LogoutRequest req) {
        authService.logout(req == null ? null : req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ApiSuccessResponse<Void> forgot(@Valid @RequestBody UserDtos.ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return ApiSuccessResponse.of(null);
    }

    @PostMapping("/reset-password")
    public ApiSuccessResponse<Void> reset(@Valid @RequestBody UserDtos.ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.password());
        return ApiSuccessResponse.of(null);
    }
}
