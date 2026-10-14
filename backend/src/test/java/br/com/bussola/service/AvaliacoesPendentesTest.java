package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.bussola.repository.OpcaoRepository;
import org.junit.jupiter.api.Test;

class AvaliacoesPendentesTest {
    private final OpcaoRepository opcoes = mock(OpcaoRepository.class);
    private final AvaliacoesPendentes service = new AvaliacoesPendentes(opcoes);

    @Test
    void resultadoNaoEValidoEnquantoHouverPendencia() {
        when(opcoes.existsByDecisaoIdAndAvaliacaoPendenteTrue(12L)).thenReturn(true);
        assertTrue(service.possuiPendencia(12L));
        assertFalse(service.resultadoValido(12L));
    }

    @Test
    void resultadoPodeSerValidoQuandoNaoHaPendencia() {
        when(opcoes.existsByDecisaoIdAndAvaliacaoPendenteTrue(12L)).thenReturn(false);
        assertFalse(service.possuiPendencia(12L));
        assertTrue(service.resultadoValido(12L));
    }
}
