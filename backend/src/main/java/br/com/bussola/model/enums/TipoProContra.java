package br.com.bussola.model.enums;

import jakarta.persistence.EnumeratedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoProContra {
    PRO(0, "Pró"),
    CONTRA(1, "Contra");

    @EnumeratedValue
    private final int codigo;

    private final String descricao;
}
