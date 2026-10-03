package br.com.bussola.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "swagger@example.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "SenhaDeTeste123!", format = "password") @NotBlank String senha) {
}
