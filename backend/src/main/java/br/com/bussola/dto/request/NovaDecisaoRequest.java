package br.com.bussola.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovaDecisaoRequest(
        @NotBlank @Size(max = 200) @Schema(example = "Devo mudar de emprego?") String titulo,
        @Size(max = 16000) @Schema(example = "Recebi uma proposta em outra cidade.") String contexto) {
}
