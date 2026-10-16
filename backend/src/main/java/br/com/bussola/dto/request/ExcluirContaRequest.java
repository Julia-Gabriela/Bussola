package br.com.bussola.dto.request;

import jakarta.validation.constraints.*;

public record ExcluirContaRequest(@NotBlank @Size(max = 1000) String senhaAtual,
        @NotNull @AssertTrue Boolean confirmar) {}
