package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.bussola.dto.request.AvaliarCriteriosRequest;
import br.com.bussola.dto.request.AvaliarCriteriosRequest.NotaCriterio;
import br.com.bussola.dto.request.ConfirmarAvaliacaoRequest;
import br.com.bussola.exception.*;
import br.com.bussola.model.entity.*;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CriteriosServiceTest {
    private final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    private final OpcaoRepository opcoes = mock(OpcaoRepository.class);
    private final CriterioRepository criterios = mock(CriterioRepository.class);
    private final AvaliacaoRepository avaliacoes = mock(AvaliacaoRepository.class);
    private final ResultadoRepository resultados = mock(ResultadoRepository.class);
    private final CriteriosService service = new CriteriosService(decisoes, opcoes, criterios, avaliacoes, resultados);
    private final Decisao decisao = new Decisao();
    private Opcao alternativaA;
    private Opcao alternativaB;
    private final List<Criterio> bancoCriterios = new ArrayList<>();
    private final List<Avaliacao> bancoNotas = new ArrayList<>();
    private long sequencia = 100;

    @BeforeEach
    void preparar() {
        decisao.setId(12L); decisao.setTitulo("Minha decisão"); decisao.setEtapaAtual(4);
        when(decisoes.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(decisao));
        when(decisoes.buscarParaAtualizar(12L, 7L)).thenReturn(Optional.of(decisao));
        alternativaA = new Opcao(); alternativaA.setId(21L); alternativaA.setNome("Trabalhar"); alternativaA.setDecisao(decisao);
        alternativaB = new Opcao(); alternativaB.setId(22L); alternativaB.setNome("Estudar"); alternativaB.setDecisao(decisao);
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(alternativaA, alternativaB));
        when(resultados.findByOpcaoId(any())).thenReturn(Optional.empty());
        when(opcoes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(criterios.findByDecisaoIdOrderByIdAsc(12L)).thenAnswer(inv -> new ArrayList<>(bancoCriterios));
        when(avaliacoes.findByCriterioDecisaoId(12L)).thenAnswer(inv -> new ArrayList<>(bancoNotas));
        when(criterios.saveAll(anyList())).thenAnswer(inv -> {
            List<Criterio> lista = inv.getArgument(0);
            for (var c : lista) { if (c.getId() == null) c.setId(++sequencia); if (!bancoCriterios.contains(c)) bancoCriterios.add(c); }
            return lista;
        });
        when(avaliacoes.saveAll(anyList())).thenAnswer(inv -> {
            List<Avaliacao> lista = inv.getArgument(0);
            for (var n : lista) { if (n.getId() == null) n.setId(++sequencia); if (!bancoNotas.contains(n)) bancoNotas.add(n); }
            return lista;
        });
        doAnswer(inv -> { bancoNotas.removeAll(inv.getArgument(0)); return null; }).when(avaliacoes).deleteAll(anyList());
        doAnswer(inv -> { bancoCriterios.removeAll(inv.getArgument(0)); return null; }).when(criterios).deleteAll(anyList());
    }

    private AvaliarCriteriosRequest dados(Integer a, Integer b) {
        return new AvaliarCriteriosRequest(CatalogoCriterios.PADROES.stream()
                .map(p -> new NotaCriterio(p.nome(), p.descricao(), a, b)).toList());
    }
    private AvaliarCriteriosRequest personalizado(Integer a, Integer b) {
        var lista = new ArrayList<>(dados(a,b).criterios());
        lista.add(new NotaCriterio("  Tempo disponível  ", "  Tempo para a família  ", a, b));
        return new AvaliarCriteriosRequest(lista);
    }

    @Test
    void consultaRetornaCatalogoSemGravarNemInventarNotas() {
        var r = service.consultar(7L,12L);
        assertEquals(5, r.criterios().size());
        assertTrue(r.criterios().stream().allMatch(c -> c.id() == null && c.notaA() == null && c.notaB() == null && c.peso() == 1));
        assertEquals("MENOR_MELHOR", r.criterios().get(2).sentido());
        verify(criterios, never()).saveAll(anyList()); verify(avaliacoes, never()).saveAll(anyList());
    }
    @Test
    void salvaParcialSemAvancarENaoTransformaRisco() {
        var r = service.salvar(7L,12L,dados(5,null));
        assertEquals(4,r.etapaAtual()); assertEquals(5,bancoNotas.size());
        assertEquals(5,r.criterios().get(2).notaA()); assertNull(r.criterios().get(2).notaB());
        assertTrue(bancoCriterios.stream().allMatch(c -> c.getDecisao() == decisao));
        verify(decisoes).buscarParaAtualizar(12L,7L); assertNotNull(decisao.getDataAtualizacao());
    }
    @Test
    void reenvioPreservaIdsEPesosSemDuplicar() {
        service.salvar(7L,12L,personalizado(2,3));
        var ids = bancoCriterios.stream().map(Criterio::getId).toList();
        var notasIds = bancoNotas.stream().map(Avaliacao::getId).toList();
        bancoCriterios.get(0).setPeso(4);
        service.salvar(7L,12L,personalizado(4,5));
        assertEquals(ids,bancoCriterios.stream().map(Criterio::getId).toList());
        assertEquals(notasIds,bancoNotas.stream().map(Avaliacao::getId).toList());
        assertEquals(4,bancoCriterios.get(0).getPeso()); assertEquals(12,bancoNotas.size());
        assertEquals("Tempo disponível",bancoCriterios.get(5).getNome());
    }
    @Test
    void removePersonalizadoESuasNotasELimpaNotaSemApagarCriteriosFixos() {
        service.salvar(7L,12L,personalizado(2,3));
        service.salvar(7L,12L,dados(null,4));
        assertEquals(5,bancoCriterios.size()); assertEquals(5,bancoNotas.size());
        assertTrue(bancoNotas.stream().allMatch(n -> n.getOpcao().getId() == 22L));
    }
    @Test
    void conclusaoCompletaAvancaApenasUmaVezSemFinalizarDecisao() {
        var primeira = service.concluir(7L,12L,personalizado(3,5));
        var repetida = service.concluir(7L,12L,personalizado(3,5));
        assertEquals(primeira,repetida); assertEquals(5,primeira.etapaAtual());
        assertEquals(StatusDecisao.EM_ANDAMENTO,primeira.status());
        verify(criterios,times(1)).saveAll(anyList());
    }
    @Test
    void repeticaoDiferenteNaoEditaEtapaDePesos() {
        service.concluir(7L,12L,dados(3,5));
        assertThrows(EtapaInvalidaException.class,() -> service.concluir(7L,12L,dados(4,5)));
        assertEquals(3,service.consultar(7L,12L).criterios().get(0).notaA());
    }
    @Test
    void incompletaOuNotasForaDaEscalaNaoGravamNada() {
        assertThrows(CriteriosInvalidosException.class,() -> service.concluir(7L,12L,dados(1,null)));
        for (int nota : new int[]{0,6}) assertThrows(CriteriosInvalidosException.class,() -> service.salvar(7L,12L,dados(nota,3)));
        verify(criterios,never()).saveAll(anyList()); assertEquals(4,decisao.getEtapaAtual());
    }
    @Test
    void rejeitaNomesRepetidosMesmoComCaixaEEspacosDiferentes() {
        var lista = new ArrayList<>(dados(1,5).criterios()); lista.add(new NotaCriterio(" SEGURANÇA ",null,1,5));
        assertThrows(CriteriosInvalidosException.class,() -> service.salvar(7L,12L,new AvaliarCriteriosRequest(lista)));
        verify(criterios,never()).saveAll(anyList());
    }
    @Test
    void naoPermiteRetirarCriterioFixoOuExcederVinte() {
        var lista = new ArrayList<>(personalizado(1,5).criterios()); lista.remove(0);
        var semFixo = new AvaliarCriteriosRequest(lista);
        assertThrows(CriteriosInvalidosException.class,() -> service.salvar(7L,12L,semFixo));
        lista = new ArrayList<>(dados(1,5).criterios());
        for (int i=0;i<16;i++) lista.add(new NotaCriterio("Personalizado "+i,null,1,5));
        var muitos = new AvaliarCriteriosRequest(lista);
        assertThrows(CriteriosInvalidosException.class,() -> service.salvar(7L,12L,muitos));
        verify(criterios,never()).saveAll(anyList());
    }
    @Test
    void titularidadeVerificadaAntesDeConsultarDados() {
        when(decisoes.findByIdAndUsuarioId(12L,7L)).thenReturn(Optional.empty());
        when(decisoes.buscarParaAtualizar(12L,7L)).thenReturn(Optional.empty());
        assertThrows(DecisaoNaoEncontradaException.class,() -> service.consultar(7L,12L));
        assertThrows(DecisaoNaoEncontradaException.class,() -> service.salvar(7L,12L,dados(1,5)));
        verifyNoInteractions(opcoes,criterios,avaliacoes);
    }
    @Test
    void bloqueiaEtapasAnterioresPosterioresEDecisaoConcluida() {
        for (int etapa : new int[]{1,2,3}) {
            decisao.setEtapaAtual(etapa);
            assertThrows(EtapaInvalidaException.class,() -> service.salvar(7L,12L,dados(1,5)));
        }
        decisao.setEtapaAtual(3);
        assertThrows(EtapaInvalidaException.class,() -> service.consultar(7L,12L));
        decisao.setEtapaAtual(6); decisao.setStatus(StatusDecisao.CONCLUIDA);
        assertEquals(StatusDecisao.CONCLUIDA,service.consultar(7L,12L).status());
        assertThrows(EtapaInvalidaException.class,() -> service.salvar(7L,12L,dados(1,5)));
        assertThrows(EtapaInvalidaException.class,() -> service.concluir(7L,12L,dados(1,5)));
        verify(criterios,never()).saveAll(anyList());
    }
    @Test
    void revisaoSalvaCriteriosSemAvancarEtapa() {
        decisao.setEtapaAtual(5);
        var resposta = service.salvar(7L, 12L, dados(2, 4));
        assertEquals(5, resposta.etapaAtual());
        assertEquals(5, decisao.getEtapaAtual());
        assertEquals(5, resposta.criterios().size());
        verify(criterios, times(1)).saveAll(anyList());
        assertThrows(EtapaInvalidaException.class, () -> service.concluir(7L, 12L, dados(1, 1)));
        assertEquals(5, decisao.getEtapaAtual());
    }

    @Test
    void salvarNaoConfirmaAvaliacaoPendenteNemMudaEtapa() {
        service.salvar(7L, 12L, dados(3, 3));
        long notaId = bancoNotas.get(0).getId();
        int valor = bancoNotas.get(0).getValor();
        alternativaA.setAvaliacaoPendente(true);
        var resposta = service.salvar(7L, 12L, dados(3, 3));
        assertTrue(alternativaA.isAvaliacaoPendente());
        assertFalse(alternativaB.isAvaliacaoPendente());
        assertTrue(resposta.avaliacaoPendenteA());
        assertEquals(4, decisao.getEtapaAtual());
        assertEquals(notaId, bancoNotas.get(0).getId());
        assertEquals(valor, bancoNotas.get(0).getValor());
        verify(opcoes, never()).save(any());
    }

    @Test
    void confirmarANaoConfirmaBMesmoSemAlterarNotas() {
        service.salvar(7L, 12L, dados(2, 4));
        var ids = bancoNotas.stream().map(Avaliacao::getId).toList();
        alternativaA.setAvaliacaoPendente(true);
        alternativaB.setAvaliacaoPendente(true);
        var resposta = service.confirmar(7L, 12L, new ConfirmarAvaliacaoRequest("A", decisao.getVersaoAvaliacao()));
        assertFalse(alternativaA.isAvaliacaoPendente());
        assertTrue(alternativaB.isAvaliacaoPendente());
        assertFalse(resposta.avaliacaoPendenteA());
        assertTrue(resposta.avaliacaoPendenteB());
        assertEquals(4, decisao.getEtapaAtual());
        assertEquals(ids, bancoNotas.stream().map(Avaliacao::getId).toList());
        verify(opcoes, times(1)).save(alternativaA);
        verify(opcoes, never()).save(alternativaB);
    }

    @Test
    void novaAlteracaoDeNotaRecriaANecessidadeDeVersao() {
        service.salvar(7L, 12L, dados(2, 4));
        long versao = decisao.getVersaoAvaliacao();
        alternativaA.setAvaliacaoPendente(false);
        service.salvar(7L, 12L, dados(5, 4));
        assertEquals(versao + 1, decisao.getVersaoAvaliacao());
        assertEquals(4, decisao.getEtapaAtual());
    }

    @Test
    void requisicaoDesatualizadaNaoConfirma() {
        alternativaA.setAvaliacaoPendente(true);
        decisao.setVersaoAvaliacao(4);
        assertThrows(EtapaInvalidaException.class,
                () -> service.confirmar(7L, 12L, new ConfirmarAvaliacaoRequest("A", 3L)));
        assertTrue(alternativaA.isAvaliacaoPendente());
        assertEquals(4, decisao.getEtapaAtual());
        verify(opcoes, never()).save(any());
    }

    @Test
    void outroUsuarioEDecisaoConcluidaNaoConfirmam() {
        assertThrows(DecisaoNaoEncontradaException.class,
                () -> service.confirmar(8L, 12L, new ConfirmarAvaliacaoRequest("B", 0L)));
        decisao.setStatus(StatusDecisao.CONCLUIDA);
        decisao.setEtapaAtual(6);
        alternativaB.setAvaliacaoPendente(true);
        assertThrows(EtapaInvalidaException.class,
                () -> service.confirmar(7L, 12L, new ConfirmarAvaliacaoRequest("B", 0L)));
        assertTrue(alternativaB.isAvaliacaoPendente());
    }

    @Test
    void conclusaoFicaBloqueadaEnquantoHouverPendencia() {
        alternativaA.setAvaliacaoPendente(true);
        assertThrows(EtapaInvalidaException.class, () -> service.concluir(7L, 12L, dados(1, 1)));
        assertEquals(4, decisao.getEtapaAtual());
        assertTrue(alternativaA.isAvaliacaoPendente());
    }

    @Test
    void exigeExatamenteDuasOpcoes() {
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of());
        assertThrows(EtapaInvalidaException.class,() -> service.consultar(7L,12L));
        assertThrows(EtapaInvalidaException.class,() -> service.salvar(7L,12L,dados(1,5)));
        verify(criterios,never()).saveAll(anyList());
    }
}
