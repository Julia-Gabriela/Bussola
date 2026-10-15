package br.com.bussola.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemGutRequest(
        @NotBlank @Size(max = 200) @Schema(example = "Queda no rendimento acadêmico") String descricao,
        @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0) @Schema(example = "4") BigDecimal gravidade,
        @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0) @Schema(example = "3") BigDecimal urgencia,
        @NotNull @DecimalMin("1") @DecimalMax("5") @Digits(integer = 1, fraction = 0) @Schema(example = "3") BigDecimal tendencia) {}
