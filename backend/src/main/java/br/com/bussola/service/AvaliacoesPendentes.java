package br.com.bussola.service;

import br.com.bussola.repository.OpcaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta se as notas de uma decisão ainda podem sustentar um resultado.
 * <p>
 * Não calcula pontuação e não apaga resultados já gravados. Pesos e Análise
 * devem chamar {@link #resultadoValido(Long)} antes de tratar um resultado
 * como definitivo. Enquanto uma alternativa estiver pendente, o resultado
 * não é válido, mesmo que a tabela {@code resultados} ainda tenha uma linha.
 */
@Service
public class AvaliacoesPendentes {
    private final OpcaoRepository opcoes;

    public AvaliacoesPendentes(OpcaoRepository opcoes) {
        this.opcoes = opcoes;
    }

    /**
     * Informa se A ou B ainda precisa de confirmação das notas.
     *
     * @param decisaoId identificador da decisão já autorizada para o usuário
     * @return {@code true} quando alguma opção dessa decisão está com
     *         {@code avaliacao_pendente}
     */
    @Transactional(readOnly = true)
    public boolean possuiPendencia(Long decisaoId) {
        return opcoes.existsByDecisaoIdAndAvaliacaoPendenteTrue(decisaoId);
    }

    /**
     * Diz se um resultado futuro pode ser considerado válido.
     * <p>
     * A resposta é negativa enquanto existir pendência. Não lê nem altera a
     * pontuação persistida. Quem recalcular o resultado no futuro é quem deve
     * limpar {@code resultados.requer_atualizacao}.
     *
     * @param decisaoId identificador da decisão
     * @return {@code true} somente quando não há avaliação pendente
     */
    @Transactional(readOnly = true)
    public boolean resultadoValido(Long decisaoId) {
        return !possuiPendencia(decisaoId);
    }
}
