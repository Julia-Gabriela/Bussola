package br.com.bussola.dto.response;

import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.enums.StatusDecisao;

public record OpcoesResponse(Long decisaoId, String titulo, int etapaAtual, StatusDecisao status,
        DadosOpcao opcaoA, DadosOpcao opcaoB) {
    public record DadosOpcao(Long id, String nome, String descricao) {
        public static DadosOpcao de(Opcao opcao) {
            return new DadosOpcao(opcao.getId(), opcao.getNome(), opcao.getDescricao());
        }
    }
}
