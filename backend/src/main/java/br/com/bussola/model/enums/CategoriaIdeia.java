package br.com.bussola.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoriaIdeia {
    FAVORAVEL(1, "Favorável"),
    DESFAVORAVEL(2, "Desfavorável"),
    DUVIDA(3, "Dúvida");

    private final int codigo;
    private final String descricao;
}
