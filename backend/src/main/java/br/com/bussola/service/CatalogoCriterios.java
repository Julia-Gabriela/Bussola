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

    /**
     * Informa o sentido de avaliação de um critério salvo.
     * <p>
     * Usa a mesma regra da tela de Critérios: um critério cujo nome coincide com
     * um critério fixo herda o sentido dele (Risco é {@code MENOR_MELHOR});
     * critérios personalizados são sempre {@code MAIOR_MELHOR}.
     *
     * @param nome nome do critério gravado na decisão
     * @return {@code "MENOR_MELHOR"} ou {@code "MAIOR_MELHOR"}
     */
    public static String sentido(String nome) {
        var padrao = buscar(nome);
        return padrao == null ? "MAIOR_MELHOR" : padrao.sentido();
    }

    /**
     * Converte a nota informada (1 a 5) na contribuição favorável usada no resultado.
     * <p>
     * Em {@code MAIOR_MELHOR} a nota é usada como está. Em {@code MENOR_MELHOR}
     * (Risco) a escala é invertida: {@code 6 - nota}, de modo que risco 1 vale 5,
     * risco 3 vale 3 e risco 5 vale 1. A nota gravada nunca é alterada; a inversão
     * acontece só no cálculo e deve ser aplicada uma única vez.
     *
     * @param nota nota informada pelo usuário, de 1 a 5
     * @param sentido sentido do critério, como devolvido por {@link #sentido(String)}
     * @return nota ajustada, de 1 a 5, em que maior é sempre mais favorável
     */
    public static int notaFavoravel(int nota, String sentido) {
        return "MENOR_MELHOR".equals(sentido) ? 6 - nota : nota;
    }
}
