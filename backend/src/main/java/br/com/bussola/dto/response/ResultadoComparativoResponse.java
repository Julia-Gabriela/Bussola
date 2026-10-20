package br.com.bussola.dto.response;

import br.com.bussola.model.enums.StatusDecisao;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado comparativo das duas alternativas de uma decisão.
 * <p>
 * {@code finalizadaEm} só é preenchida quando {@code status} é {@code CONCLUIDA}.
 * {@code empate} é verdadeiro quando as duas pontuações são iguais; nesse caso
 * {@code recomendadaId} é nulo, porque não há critério automático de desempate.
 * Em cada critério, {@code nota} é a nota informada pelo usuário e
 * {@code notaAjustada} é a contribuição favorável usada no cálculo (no Risco,
 * {@code 6 - nota}); {@code pontuacao} é {@code notaAjustada * peso}.
 */
public record ResultadoComparativoResponse(Long decisaoId, String titulo, int etapaAtual,
        StatusDecisao status, LocalDateTime finalizadaEm, int pontuacaoMaxima, boolean empate,
        Long recomendadaId, List<Alternativa> alternativas) {
    public record Alternativa(Long id, String nome, int pontuacao, List<Criterio> criterios) {}
    public record Criterio(String nome, int peso, int nota, String sentido, int notaAjustada, int pontuacao) {}
}
