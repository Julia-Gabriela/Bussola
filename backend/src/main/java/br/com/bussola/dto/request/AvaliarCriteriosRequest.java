package br.com.bussola.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

public record AvaliarCriteriosRequest(
        @NotNull @Size(min = 5, max = 20) List<@NotNull @Valid NotaCriterio> criterios) {
    public record NotaCriterio(@NotBlank @Size(max = 200) String nome,
            @Size(max = 1000) String descricao,
            @JsonDeserialize(using = NotaInteiraDeserializer.class) @Min(1) @Max(5) Integer notaA,
            @JsonDeserialize(using = NotaInteiraDeserializer.class) @Min(1) @Max(5) Integer notaB) {}
}
