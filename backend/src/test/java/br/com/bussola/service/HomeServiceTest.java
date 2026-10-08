package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.bussola.dto.response.DecisaoResumoResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HomeServiceTest {
    private final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    private final HomeService service = new HomeService(decisoes);
    private final UsuarioResponse usuario = new UsuarioResponse(7L, "Luiza Teste", "luiza@example.com");

    @Test
    void contaNovaRetornaZerosListaVaziaESemDecisaoParaContinuar() {
        var home = service.consultar(usuario);
        assertEquals(usuario, home.usuario());
        assertEquals(0, home.decisoesConcluidas());
        assertEquals(0, home.decisoesEmAndamento());
        assertTrue(home.decisoesRecentes().isEmpty());
        assertNull(home.decisaoEmAndamento());
    }

    @Test
    void consultaTudoPeloUsuarioAutenticadoEUsaAsSeisEtapasDoBanco() {
        var emAndamento = decisao(42L, StatusDecisao.EM_ANDAMENTO, 3);
        var concluida = decisao(41L, StatusDecisao.CONCLUIDA, 6);
        when(decisoes.countByUsuarioIdAndStatus(7L, StatusDecisao.CONCLUIDA)).thenReturn(8L);
        when(decisoes.countByUsuarioIdAndStatus(7L, StatusDecisao.EM_ANDAMENTO)).thenReturn(1L);
        when(decisoes.findTop4ByUsuarioIdOrderByDataAtualizacaoDescIdDesc(7L))
                .thenReturn(List.of(emAndamento, concluida));
        when(decisoes.findFirstByUsuarioIdAndStatusOrderByDataAtualizacaoDescIdDesc(7L, StatusDecisao.EM_ANDAMENTO))
                .thenReturn(Optional.of(emAndamento));
        var home = service.consultar(usuario);
        assertEquals(8, home.decisoesConcluidas());
        assertEquals(1, home.decisoesEmAndamento());
        assertEquals(List.of(42L, 41L), home.decisoesRecentes().stream().map(DecisaoResumoResponse::id).toList());
        assertEquals(6, home.decisaoEmAndamento().totalEtapas());
        assertEquals(50, home.decisaoEmAndamento().progressoPercentual());
        assertEquals(100, home.decisoesRecentes().get(1).progressoPercentual());
        verify(decisoes).findTop4ByUsuarioIdOrderByDataAtualizacaoDescIdDesc(7L);
        verify(decisoes).findFirstByUsuarioIdAndStatusOrderByDataAtualizacaoDescIdDesc(7L, StatusDecisao.EM_ANDAMENTO);
        verify(decisoes).countByUsuarioIdAndStatus(7L, StatusDecisao.CONCLUIDA);
        verify(decisoes).countByUsuarioIdAndStatus(7L, StatusDecisao.EM_ANDAMENTO);
        verifyNoMoreInteractions(decisoes);
    }

    @Test
    void buscaDecisaoParaContinuarMesmoQuandoNaoEstaEntreAsRecentes() {
        when(decisoes.findTop4ByUsuarioIdOrderByDataAtualizacaoDescIdDesc(7L))
                .thenReturn(List.of(decisao(99L, StatusDecisao.CONCLUIDA, 6)));
        when(decisoes.findFirstByUsuarioIdAndStatusOrderByDataAtualizacaoDescIdDesc(7L, StatusDecisao.EM_ANDAMENTO))
                .thenReturn(Optional.of(decisao(1L, StatusDecisao.EM_ANDAMENTO, 1)));
        assertEquals(1L, service.consultar(usuario).decisaoEmAndamento().id());
    }

    @Test
    void estarNaUltimaEtapaNaoConcluiADecisao() {
        var resumo = DecisaoResumoResponse.de(decisao(1L, StatusDecisao.EM_ANDAMENTO, 6));
        assertEquals(StatusDecisao.EM_ANDAMENTO, resumo.status());
        assertEquals(100, resumo.progressoPercentual());
    }

    private Decisao decisao(long id, StatusDecisao status, int etapa) {
        var decisao = new Decisao();
        decisao.setId(id);
        decisao.setTitulo("Uma escolha de teste");
        decisao.setStatus(status);
        decisao.setEtapaAtual(etapa);
        decisao.setDataAtualizacao(LocalDateTime.of(2026, 10, 7, 12, 0));
        return decisao;
    }
}
