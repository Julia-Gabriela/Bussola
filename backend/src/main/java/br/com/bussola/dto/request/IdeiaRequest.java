package br.com.bussola.dto.request;

import br.com.bussola.model.enums.CategoriaIdeia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IdeiaRequest(
        @NotBlank @Size(max = 16000) @Schema(example = "Ganhar experiência profissional") String conteudo,
        @NotNull @Schema(example = "FAVORAVEL") CategoriaIdeia categoria) {
}
