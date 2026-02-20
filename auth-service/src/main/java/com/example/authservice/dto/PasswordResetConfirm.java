package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirm {
    @NotBlank
    private String token;
    @NotBlank
    @Size(min=5)
    private String newPassword;
}
