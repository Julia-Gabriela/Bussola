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
    /**
     * Item da Matriz GUT. {@code proContraId} indica o pró/contra classificado
     * (nulo nos fatores antigos, digitados à mão); nesse caso a descrição é a
     * do próprio pró/contra, sempre atualizada.
     */
    public record FatorGut(Long id, Long proContraId, String descricao, int gravidade, int urgencia, int tendencia,
            int prioridade) {
        /**
         * Converte a entidade em resposta, calculando a prioridade G × U × T.
         *
         * @param item item GUT persistido, vinculado ou não a um pró/contra
         * @return resposta com a descrição atual do pró/contra vinculado, se houver
         */
        public static FatorGut de(ItemGut item) {
            var vinculado = item.getProContra();
            return new FatorGut(item.getId(), vinculado == null ? null : vinculado.getId(),
                    vinculado == null ? item.getDescricao() : vinculado.getDescricao(),
                    item.getGravidade(), item.getUrgencia(), item.getTendencia(),
                    item.getGravidade() * item.getUrgencia() * item.getTendencia());
        }
    }
}
