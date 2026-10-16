package br.com.bussola.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SalvarInversaoRequest(@NotNull @Valid Reflexao opcaoA, @NotNull @Valid Reflexao opcaoB) {
    public record Reflexao(@NotNull @Positive Long opcaoId,
            @Size(max = 16000) String fracassos, @Size(max = 16000) String prevencoes) {}
}
