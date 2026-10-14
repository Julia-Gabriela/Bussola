package br.com.bussola.dto.response;

import br.com.bussola.model.enums.StatusDecisao;
import java.util.List;

public record CriteriosResponse(Long decisaoId, String titulo, int etapaAtual, StatusDecisao status,
        OpcoesResponse.DadosOpcao opcaoA, OpcoesResponse.DadosOpcao opcaoB, List<Item> criterios,
        boolean avaliacaoPendenteA, boolean avaliacaoPendenteB, long versaoAvaliacao) {
    public record Item(Long id, String codigo, String nome, String descricao, boolean personalizado,
            String sentido, Integer peso, Integer notaA, Integer notaB) {}
}
