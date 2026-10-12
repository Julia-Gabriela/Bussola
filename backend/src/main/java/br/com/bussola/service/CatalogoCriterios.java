package br.com.bussola.service;

import java.util.List;
import java.util.Locale;

/** Fonte dos critérios fixos. A nota bruta de Risco cresce com o risco, não com a preferência. */
public final class CatalogoCriterios {
    private CatalogoCriterios() {}
    public record Padrao(String codigo, String nome, String descricao, String sentido) {}
    public static final List<Padrao> PADROES = List.of(
        new Padrao("seguranca", "Segurança", "Estabilidade e previsibilidade", "MAIOR_MELHOR"),
        new Padrao("crescimento", "Crescimento", "Potencial de desenvolvimento pessoal e profissional", "MAIOR_MELHOR"),
        new Padrao("risco", "Risco", "Nível de incerteza e consequências negativas", "MENOR_MELHOR"),
        new Padrao("emocional", "Impacto emocional", "Como afeta seu bem-estar e saúde mental", "MAIOR_MELHOR"),
        new Padrao("valores", "Alinhamento a valores", "Consonância com seus princípios e metas de vida", "MAIOR_MELHOR")
    );
    public static String chave(String nome) { return nome.strip().toLowerCase(Locale.ROOT); }
    public static Padrao buscar(String nome) {
        return PADROES.stream().filter(p -> chave(p.nome()).equals(chave(nome))).findFirst().orElse(null);
    }
}
