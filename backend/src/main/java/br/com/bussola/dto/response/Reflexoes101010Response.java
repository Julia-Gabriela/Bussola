package br.com.bussola.dto.response;

import br.com.bussola.model.enums.StatusDecisao;
import java.time.LocalDateTime;

public record Reflexoes101010Response(Long decisaoId, String titulo, int etapaAtual, StatusDecisao status,
        boolean concluida, LocalDateTime concluidaEm, Alternativa opcaoA, Alternativa opcaoB) {
    public record Alternativa(Long id, String nome, String descricao, Long reflexaoId,
            String curtoPrazo, String medioPrazo, String longoPrazo) {}
}
