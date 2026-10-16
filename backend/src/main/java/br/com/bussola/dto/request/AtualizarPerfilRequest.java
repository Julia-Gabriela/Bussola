package br.com.bussola.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record AtualizarPerfilRequest(
        @NotBlank @Size(max = 150) String nomeCompleto,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 1000) String senhaAtual) {}
