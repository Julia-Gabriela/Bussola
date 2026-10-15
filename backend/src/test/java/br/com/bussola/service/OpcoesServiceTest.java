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
import br.com.bussola.model.entity.Resultado;
import br.com.bussola.repository.AvaliacaoRepository;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.OpcaoRepository;
import br.com.bussola.repository.ResultadoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpcoesServiceTest {
    private final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    private final OpcaoRepository opcoes = mock(OpcaoRepository.class);
    private final AvaliacaoRepository avaliacoes = mock(AvaliacaoRepository.class);
    private final ResultadoRepository resultados = mock(ResultadoRepository.class);
    private final OpcoesService service = new OpcoesService(decisoes, opcoes, avaliacoes, resultados);
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
        when(resultados.findByOpcaoId(any())).thenReturn(Optional.empty());
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
        for (int etapa : new int[]{1, 2}) {
            decisao.setEtapaAtual(etapa);
            assertThrows(EtapaInvalidaException.class, () -> service.salvar(7L, 12L, dados));
        }
        decisao.setEtapaAtual(2);
        assertThrows(EtapaInvalidaException.class, () -> service.consultar(7L, 12L));
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void revisaoSalvaOpcoesSemAvancarEtapaNemDuplicarIds() {
        var a = opcao(21L, "Antiga A");
        var b = opcao(32L, "Antiga B");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        decisao.setEtapaAtual(5);
        var resposta = service.salvar(7L, 12L, dados);
        assertEquals(5, resposta.etapaAtual());
        assertEquals(5, decisao.getEtapaAtual());
        assertEquals(21L, resposta.opcaoA().id());
        assertEquals(32L, resposta.opcaoB().id());
        assertEquals("Trabalhar", a.getNome());
        verify(opcoes, times(1)).saveAll(anyList());
        assertThrows(EtapaInvalidaException.class, () -> service.concluir(7L, 12L, dados));
        assertEquals(5, decisao.getEtapaAtual());
    }

    @Test
    void decisaoConcluidaPermiteConsultaMasNaoAlteracao() {
        decisao.setStatus(StatusDecisao.CONCLUIDA);
        decisao.setEtapaAtual(6);
        assertEquals(StatusDecisao.CONCLUIDA, service.consultar(7L, 12L).status());
        assertThrows(EtapaInvalidaException.class, () -> service.salvar(7L, 12L, dados));
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

    @Test
    void alterarNomeDeAMarcaSomenteAQuandoJaAvaliada() {
        var a = opcao(21L, "Antiga A");
        var b = opcao(32L, "Antiga B");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(21L)).thenReturn(true);
        when(avaliacoes.existsByOpcaoId(32L)).thenReturn(true);
        decisao.setEtapaAtual(5);
        var resposta = service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao(" Nova A ", "Aprender na prática"), new DadosOpcao("Antiga B", null)));
        assertTrue(a.isAvaliacaoPendente());
        assertFalse(b.isAvaliacaoPendente());
        assertTrue(resposta.avaliacaoPendenteA());
        assertFalse(resposta.avaliacaoPendenteB());
        assertFalse(resposta.recomendacaoRevisarA());
        assertEquals(1L, decisao.getVersaoAvaliacao());
        assertEquals(5, decisao.getEtapaAtual());
        assertEquals(21L, a.getId());
        assertEquals("Nova A", a.getNome());
    }

    @Test
    void alterarNomeDeBMarcaSomenteB() {
        var a = opcao(21L, "Antiga A");
        var b = opcao(32L, "Antiga B");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(21L)).thenReturn(true);
        when(avaliacoes.existsByOpcaoId(32L)).thenReturn(true);
        decisao.setEtapaAtual(4);
        service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao("Antiga A", null), new DadosOpcao("Nova B", null)));
        assertFalse(a.isAvaliacaoPendente());
        assertTrue(b.isAvaliacaoPendente());
        assertEquals(4, decisao.getEtapaAtual());
    }

    @Test
    void alterarAmbosOsNomesMarcaAsDuasAlternativasAvaliadas() {
        var a = opcao(21L, "Antiga A");
        var b = opcao(32L, "Antiga B");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(anyLong())).thenReturn(true);
        decisao.setEtapaAtual(5);
        service.salvar(7L, 12L, dados);
        assertTrue(a.isAvaliacaoPendente());
        assertTrue(b.isAvaliacaoPendente());
        assertEquals(1L, decisao.getVersaoAvaliacao());
    }

    @Test
    void alterarSoDescricaoNaoCriaPendenciaObrigatoria() {
        var a = opcao(21L, "Trabalhar");
        a.setDescricao("Antes");
        var b = opcao(32L, "Estudar");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(21L)).thenReturn(true);
        decisao.setEtapaAtual(5);
        var resposta = service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao("Trabalhar", "  Depois  "), new DadosOpcao("Estudar", null)));
        assertFalse(a.isAvaliacaoPendente());
        assertFalse(b.isAvaliacaoPendente());
        assertTrue(resposta.recomendacaoRevisarA());
        assertFalse(resposta.recomendacaoRevisarB());
        assertEquals(0L, decisao.getVersaoAvaliacao());
        assertEquals("Depois", a.getDescricao());
    }

    @Test
    void reenviarOsMesmosDadosNaoCriaPendencia() {
        var a = opcao(21L, "Trabalhar");
        a.setDescricao("Aprender na prática");
        a.setAvaliacaoPendente(true);
        var b = opcao(32L, "Estudar");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(anyLong())).thenReturn(true);
        decisao.setEtapaAtual(5);
        decisao.setVersaoAvaliacao(3);
        var resposta = service.salvar(7L, 12L, dados);
        assertTrue(a.isAvaliacaoPendente());
        assertFalse(b.isAvaliacaoPendente());
        assertFalse(resposta.recomendacaoRevisarA());
        assertEquals(3L, decisao.getVersaoAvaliacao());
    }

    @Test
    void espacosNasPontasNaoCriamPendenciaMesmoComAvaliacao() {
        var a = opcao(21L, "Trabalhar");
        var b = opcao(32L, "Estudar");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(anyLong())).thenReturn(true);
        decisao.setEtapaAtual(5);
        service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao("  Trabalhar  ", null), new DadosOpcao("Estudar", "   ")));
        assertFalse(a.isAvaliacaoPendente());
        assertFalse(b.isAvaliacaoPendente());
        assertEquals(0L, decisao.getVersaoAvaliacao());
    }

    @Test
    void novaAlteracaoDeNomeRecriaAPendencia() {
        var a = opcao(21L, "Antiga A");
        var b = opcao(32L, "Antiga B");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(21L)).thenReturn(true);
        decisao.setEtapaAtual(5);
        service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao("Primeira", null), new DadosOpcao("Antiga B", null)));
        a.setAvaliacaoPendente(false);
        service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao("Segunda", null), new DadosOpcao("Antiga B", null)));
        assertTrue(a.isAvaliacaoPendente());
        assertFalse(b.isAvaliacaoPendente());
        assertEquals(2L, decisao.getVersaoAvaliacao());
        assertEquals(5, decisao.getEtapaAtual());
    }

    @Test
    void nomeSemAvaliacaoNaoCriaPendenciaMesmoComEspacos() {
        var a = opcao(21L, "Trabalhar");
        var b = opcao(32L, "Estudar");
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        decisao.setEtapaAtual(5);
        var resposta = service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao("  Trabalhar  ", null), new DadosOpcao("Outro nome", null)));
        assertFalse(resposta.avaliacaoPendenteA());
        assertFalse(resposta.avaliacaoPendenteB());
        assertEquals(0L, decisao.getVersaoAvaliacao());
        assertEquals("Outro nome", b.getNome());
    }

    @Test
    void nomeAlteradoSinalizaResultadoSemApagar() {
        var a = opcao(21L, "Antiga A");
        var b = opcao(32L, "Antiga B");
        var resultado = new Resultado();
        resultado.setId(9L);
        resultado.setOpcao(a);
        resultado.setPontuacaoTotal(10);
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(avaliacoes.existsByOpcaoId(21L)).thenReturn(true);
        when(resultados.findByOpcaoId(21L)).thenReturn(Optional.of(resultado));
        decisao.setEtapaAtual(5);
        service.salvar(7L, 12L, new DefinirOpcoesRequest("Minha decisão",
                new DadosOpcao("Nova A", null), new DadosOpcao("Antiga B", null)));
        assertTrue(resultado.isRequerAtualizacao());
        assertEquals(10, resultado.getPontuacaoTotal());
        verify(resultados, never()).delete(any());
    }

    @Test
    void revisarAlternativaExigeNovaConclusaoDoMetodo101010() {
        service.salvar(7L, 12L, dados);
        var concluida = java.time.LocalDateTime.now();
        decisao.setReflexao101010ConcluidaEm(concluida);
        service.salvar(7L, 12L, dados);
        assertEquals(concluida, decisao.getReflexao101010ConcluidaEm());
        var diferentes = new DefinirOpcoesRequest(dados.titulo(),
                new DadosOpcao("Outra alternativa", dados.opcaoA().descricao()), dados.opcaoB());
        service.salvar(7L, 12L, diferentes);
        assertNull(decisao.getReflexao101010ConcluidaEm());
    }

    private Opcao opcao(long id, String nome) {
        var opcao = new Opcao(); opcao.setId(id); opcao.setNome(nome); opcao.setDecisao(decisao); return opcao;
    }
}
