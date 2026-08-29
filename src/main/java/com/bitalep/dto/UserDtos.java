package com.bitalep.dto;

import com.bitalep.entity.Department;
import com.bitalep.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {}

    public record UserResponse(
            UUID id,
            String name,
            String surname,
            String email,
            UserRole role,
            Department department,
            UUID tenantId,
            Instant createdDate
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String surname,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank @Size(max = 200) String companyName
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(String refreshToken) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {}

    public record LoginResponse(String token, String refreshToken, UserResponse user) {}

    public record TokenPairResponse(String token, String refreshToken) {}

    public record CreateUserRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String surname,
            @NotBlank @Email String email,
            UserRole role,
            Department department,
            @Size(min = 8, max = 128) String password
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String surname
    ) {}

    public record UpdateRoleRequest(UserRole role) {}

    public record SetActiveRequest(Boolean active) {}
}
