package br.com.bussola.dto.response;
import br.com.bussola.model.enums.StatusDecisao;
import java.util.List;
public record PesosResponse(Long decisaoId, String titulo, int etapaAtual, StatusDecisao status, List<Item> criterios) {
    public record Item(Long id, String nome, String descricao, boolean personalizado, int peso) {}
}
