package br.com.bussola.dto.response;

import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import java.time.LocalDateTime;

public record DecisaoResumoResponse(Long id, String titulo, StatusDecisao status,
        int etapaAtual, int totalEtapas, int progressoPercentual, LocalDateTime dataAtualizacao) {
    // O contrato acompanha chk_decisoes_etapa_atual da migration V1 (1 a 6).
    public static final int TOTAL_ETAPAS = 6;

    public static DecisaoResumoResponse de(Decisao decisao) {
        int etapa = decisao.getEtapaAtual();
        int progresso = decisao.getStatus() == StatusDecisao.CONCLUIDA
                ? 100 : (int) Math.round(etapa * 100.0 / TOTAL_ETAPAS);
        return new DecisaoResumoResponse(decisao.getId(), decisao.getTitulo(), decisao.getStatus(),
                etapa, TOTAL_ETAPAS, progresso, decisao.getDataAtualizacao());
    }
}
