package br.com.bussola.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfirmarAvaliacaoRequest(
        @NotNull @Pattern(regexp = "[AB]") String alternativa,
        @NotNull Long versaoAvaliacao) {
}
