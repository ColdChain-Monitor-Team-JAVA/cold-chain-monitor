package com.coldchain.monitor.dto;

import com.coldchain.monitor.entity.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Dtos {
    private Dtos() {}

    public record LoginRequest(@NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") String email,
                               @NotBlank(message = "Password is required.") String password, Role role) {}
    public record MfaVerifyRequest(@NotBlank String mfaToken, @NotBlank(message = "MFA code is required.") String code) {}
    public record MfaResendRequest(@NotBlank String mfaToken) {}
    public record MfaChallenge(String mfaToken, long expiresInSeconds, String note) {}

    public record UserResponse(Long id, String name, String username, String email, Role role, Status status, Instant lastLogin, Instant createdAt) {
        public static UserResponse of(User u) {
            return new UserResponse(u.id, u.name, u.username, u.email, u.role, u.status, u.lastLogin, u.createdAt);
        }
    }
    public record AuthResponse(String token, UserResponse user) {}

    public record CreateUserRequest(@NotBlank(message = "Name is required.") @Size(max = 100) String name,
                                    @NotBlank(message = "Username is required.") @Size(min = 3, max = 50) String username,
                                    @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") String email,
                                    @NotBlank(message = "Password is required.") @Size(min = 6, max = 100, message = "Password must be 6-100 characters.") String password,
                                    @NotNull(message = "Role is required.") Role role, Status status) {}
    public record UpdateUserRequest(@NotBlank(message = "Name is required.") @Size(max = 100) String name,
                                    @NotBlank(message = "Username is required.") @Size(min = 3, max = 50) String username,
                                    @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") String email,
                                    @NotNull(message = "Role is required.") Role role,
                                    @NotNull(message = "Status is required.") Status status) {}
}
