package br.com.bussola.dto.request;

import jakarta.validation.constraints.*;

public record AlterarSenhaRequest(
        @NotBlank @Size(max = 1000) String senhaAtual,
        @NotBlank @Size(min = 8, max = 72) String novaSenha,
        @NotBlank @Size(max = 72) String confirmacaoSenha) {}
