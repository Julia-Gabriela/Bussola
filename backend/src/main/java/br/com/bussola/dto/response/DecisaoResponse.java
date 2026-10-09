package br.com.bussola.dto.response;

import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import java.time.LocalDateTime;

public record DecisaoResponse(Long id, String titulo, String contexto, StatusDecisao status,
        int etapaAtual, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
    public static DecisaoResponse de(Decisao decisao) {
        return new DecisaoResponse(decisao.getId(), decisao.getTitulo(), decisao.getContexto(),
                decisao.getStatus(), decisao.getEtapaAtual(), decisao.getDataCriacao(),
                decisao.getDataAtualizacao());
    }
}
