package dev.clebin.authforge.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 2, max = 120) String fullName,
    @NotBlank @Size(min = 12, max = 72) String password
) {
}
