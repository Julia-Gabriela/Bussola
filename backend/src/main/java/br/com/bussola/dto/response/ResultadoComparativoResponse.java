package br.com.bussola.dto.response;

import br.com.bussola.model.enums.StatusDecisao;
import java.util.List;

public record ResultadoComparativoResponse(Long decisaoId, String titulo, int etapaAtual,
        StatusDecisao status, int pontuacaoMaxima, List<Alternativa> alternativas) {
    public record Alternativa(Long id, String nome, int pontuacao, List<Criterio> criterios) {}
    public record Criterio(String nome, int peso, int nota, int pontuacao) {}
}