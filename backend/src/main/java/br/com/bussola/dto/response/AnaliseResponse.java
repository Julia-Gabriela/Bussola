package br.com.bussola.dto.response;

import br.com.bussola.model.entity.ItemGut;
import br.com.bussola.model.entity.ProContra;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.model.enums.TipoProContra;
import java.util.List;

public record AnaliseResponse(Long decisaoId, String titulo, int etapaAtual, StatusDecisao status,
        List<Alternativa> opcoes, List<FatorGut> fatoresGut) {
    public record Alternativa(Long id, String nome, List<Argumento> prosContras) {}
    public record Argumento(Long id, TipoProContra tipo, String descricao) {
        public static Argumento de(ProContra item) {
            return new Argumento(item.getId(), item.getTipo(), item.getDescricao());
        }
    }
    public record FatorGut(Long id, String descricao, int gravidade, int urgencia, int tendencia, int prioridade) {
        public static FatorGut de(ItemGut item) {
            return new FatorGut(item.getId(), item.getDescricao(), item.getGravidade(), item.getUrgencia(),
                    item.getTendencia(), item.getGravidade() * item.getUrgencia() * item.getTendencia());
        }
    }
}
