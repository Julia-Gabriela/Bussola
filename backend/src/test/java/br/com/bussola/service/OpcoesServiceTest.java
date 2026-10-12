package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.bussola.dto.request.DefinirOpcoesRequest;
import br.com.bussola.dto.request.DefinirOpcoesRequest.DadosOpcao;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.exception.OpcoesInvalidasException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.OpcaoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpcoesServiceTest {
    private final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    private final OpcaoRepository opcoes = mock(OpcaoRepository.class);
    private final OpcoesService service = new OpcoesService(decisoes, opcoes);
    private final Decisao decisao = new Decisao();
    private final DefinirOpcoesRequest dados = new DefinirOpcoesRequest("  Minha decisão  ",
            new DadosOpcao("  Trabalhar  ", "  Aprender na prática  "), new DadosOpcao("Estudar", "  "));

    @BeforeEach
    void preparar() {
        decisao.setId(12L);
        decisao.setTitulo("Minha decisão");
        decisao.setEtapaAtual(3);
        when(decisoes.buscarParaAtualizar(12L, 7L)).thenReturn(Optional.of(decisao));
        when(decisoes.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(decisao));
        when(opcoes.saveAll(anyList())).thenAnswer(inv -> {
            List<Opcao> par = inv.getArgument(0);
            for (int i = 0; i < par.size(); i++) if (par.get(i).getId() == null) par.get(i).setId(100L + i);
            when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(par);
            return par;
        });
    }

    @Test
    void consultaSemOpcoesNaoCriaDados() {
        var resposta = service.consultar(7L, 12L);
        assertNull(resposta.opcaoA());
        assertNull(resposta.opcaoB());
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void salvaParNaMesmaDecisaoSemAvancarENormalizaCampos() {
        var resposta = service.salvar(7L, 12L, dados);
        assertEquals("Minha decisão", resposta.titulo());
        assertEquals("Trabalhar", resposta.opcaoA().nome());
        assertEquals("Aprender na prática", resposta.opcaoA().descricao());
        assertNull(resposta.opcaoB().descricao());
        assertEquals(3, resposta.etapaAtual());
        assertNotNull(decisao.getDataAtualizacao());
        verify(decisoes).buscarParaAtualizar(12L, 7L);
        assertTrue(opcoes.findByDecisaoIdOrderByIdAsc(12L).stream().allMatch(o -> o.getDecisao() == decisao));
    }

    @Test
    void naoPermiteRenomearDecisaoAoSalvarOuConcluirOpcoes() {
        var alterados = new DefinirOpcoesRequest("Outro nome", dados.opcaoA(), dados.opcaoB());
        assertThrows(EtapaInvalidaException.class, () -> service.salvar(7L, 12L, alterados));
        assertThrows(EtapaInvalidaException.class, () -> service.concluir(7L, 12L, alterados));
        assertEquals("Minha decisão", decisao.getTitulo());
        assertEquals(3, decisao.getEtapaAtual());
        verify(opcoes, never()).saveAll(anyList());
        verify(decisoes, never()).save(any());
    }

    @Test
    void editarPreservaIdsEAssociacoesSemDuplicar() {
        var a = opcao(21L, "Antiga A");
        var b = opcao(32L, "Antiga B");
        var avaliacoesA = a.getAvaliacoes();
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        var primeira = service.salvar(7L, 12L, dados);
        var segunda = service.salvar(7L, 12L, dados);
        assertEquals(21L, primeira.opcaoA().id());
        assertEquals(32L, segunda.opcaoB().id());
        assertSame(avaliacoesA, a.getAvaliacoes());
        assertEquals(2, opcoes.findByDecisaoIdOrderByIdAsc(12L).size());
        verify(opcoes, never()).deleteAll();
    }

    @Test
    void concluirSalvaEAvancaUmaVezSemFinalizarDecisao() {
        var primeira = service.concluir(7L, 12L, dados);
        var repetida = service.concluir(7L, 12L, dados);
        assertEquals(4, primeira.etapaAtual());
        assertEquals(StatusDecisao.EM_ANDAMENTO, primeira.status());
        assertEquals(primeira, repetida);
        verify(opcoes, times(1)).saveAll(anyList());
    }

    @Test
    void repeticaoComConteudoDiferenteNaoEditaEtapaJaConcluida() {
        service.concluir(7L, 12L, dados);
        var diferentes = new DefinirOpcoesRequest("Outro título", dados.opcaoA(), dados.opcaoB());
        assertThrows(EtapaInvalidaException.class, () -> service.concluir(7L, 12L, diferentes));
        assertEquals("Minha decisão", decisao.getTitulo());
        verify(opcoes, times(1)).saveAll(anyList());
    }

    @Test
    void naoPermitePularBrainstormingNemEditarEtapasPosteriores() {
        for (int etapa : new int[]{1, 2, 4, 5, 6}) {
            decisao.setEtapaAtual(etapa);
            assertThrows(EtapaInvalidaException.class, () -> service.salvar(7L, 12L, dados));
        }
        decisao.setEtapaAtual(2);
        assertThrows(EtapaInvalidaException.class, () -> service.consultar(7L, 12L));
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void decisaoConcluidaPermiteConsultaMasNaoAlteracao() {
        decisao.setStatus(StatusDecisao.CONCLUIDA);
        decisao.setEtapaAtual(6);
        assertEquals(StatusDecisao.CONCLUIDA, service.consultar(7L, 12L).status());
        assertThrows(EtapaInvalidaException.class, () -> service.concluir(7L, 12L, dados));
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void naoAcessaOpcoesDeOutroUsuario() {
        assertThrows(DecisaoNaoEncontradaException.class, () -> service.salvar(99L, 12L, dados));
        assertThrows(DecisaoNaoEncontradaException.class, () -> service.consultar(99L, 12L));
        verifyNoInteractions(opcoes);
    }

    @Test
    void rejeitaNomesIguaisIgnorandoCaixaEEspacos() {
        var iguais = new DefinirOpcoesRequest("Minha decisão", new DadosOpcao(" Estudar ", null), new DadosOpcao("ESTUDAR", null));
        assertThrows(OpcoesInvalidasException.class, () -> service.salvar(7L, 12L, iguais));
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void naoApagaOpcoesExtrasPreexistentes() {
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(opcao(1L,"A"), opcao(2L,"B"), opcao(3L,"C")));
        assertThrows(EtapaInvalidaException.class, () -> service.salvar(7L, 12L, dados));
        verify(opcoes, never()).saveAll(anyList());
    }

    private Opcao opcao(long id, String nome) {
        var opcao = new Opcao(); opcao.setId(id); opcao.setNome(nome); opcao.setDecisao(decisao); return opcao;
    }
}
