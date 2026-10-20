package br.com.bussola.dto.response;

import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import java.time.LocalDateTime;

public record DecisaoResumoResponse(Long id, String titulo, StatusDecisao status,
        int etapaAtual, int totalEtapas, int progressoPercentual, LocalDateTime dataAtualizacao,
        LocalDateTime finalizadaEm) {
    // O contrato acompanha chk_decisoes_etapa_atual da migration V1 (1 a 6).
    public static final int TOTAL_ETAPAS = 6;

    public static DecisaoResumoResponse de(Decisao decisao) {
        int etapa = decisao.getEtapaAtual();
        boolean concluida = decisao.getStatus() == StatusDecisao.CONCLUIDA;
        int progresso = concluida ? 100 : (int) Math.round(etapa * 100.0 / TOTAL_ETAPAS);
        // Uma decisão concluída não aceita mais escritas, então a última atualização é a finalização.
        return new DecisaoResumoResponse(decisao.getId(), decisao.getTitulo(), decisao.getStatus(),
                etapa, TOTAL_ETAPAS, progresso, decisao.getDataAtualizacao(),
                concluida ? decisao.getDataAtualizacao() : null);
    }
}
