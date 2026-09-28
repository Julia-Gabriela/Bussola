package br.com.bussola.model.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusDecisao {
    EM_ANDAMENTO(0, "Em andamento"),
    CONCLUIDA(1, "Concluída");

    @EnumeratedValue
    private final int codigo;

    private final String descricao;
}
