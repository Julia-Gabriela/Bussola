package br.com.bussola.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DefinirOpcoesRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotNull @Valid DadosOpcao opcaoA,
        @NotNull @Valid DadosOpcao opcaoB) {
    public record DadosOpcao(
            @NotBlank @Size(max = 200) String nome,
            @Size(max = 16000) String descricao) {}
}
