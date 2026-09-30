package br.com.bussola.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CadastroUsuarioRequest(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank String senha,
        @NotNull @AssertTrue Boolean aceitouTermos) {
}
