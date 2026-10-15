package br.com.bussola.dto.request;

import br.com.bussola.model.enums.TipoProContra;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProContraRequest(@NotNull TipoProContra tipo,
        @NotBlank @Size(max = 300) @Schema(example = "Ganhar experiência profissional") String descricao) {}
