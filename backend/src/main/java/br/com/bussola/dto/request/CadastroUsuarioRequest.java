package br.com.bussola.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CadastroUsuarioRequest(
        @Schema(example = "Usuario Swagger") @NotBlank @Size(max = 150) String nomeCompleto,
        @Schema(example = "2000-01-01") @NotNull @Past LocalDate dataNascimento,
        @Schema(example = "swagger@example.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "SenhaDeTeste123!", format = "password") @NotBlank String senha,
        @Schema(example = "true") @NotNull @AssertTrue Boolean aceitouTermos) {
}
