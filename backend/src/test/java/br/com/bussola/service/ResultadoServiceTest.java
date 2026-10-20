package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import br.com.bussola.dto.response.ResultadoComparativoResponse;
import br.com.bussola.exception.*;
import br.com.bussola.model.entity.*;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.model.enums.TipoProContra;
import br.com.bussola.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResultadoServiceTest {
    final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    final CriterioRepository criterios = mock(CriterioRepository.class);
    final OpcaoRepository opcoes = mock(OpcaoRepository.class);
    final AvaliacaoRepository avaliacoes = mock(AvaliacaoRepository.class);
    final ProContraRepository argumentos = mock(ProContraRepository.class);
    final ItemGutRepository fatores = mock(ItemGutRepository.class);
    final ResultadoRepository resultados = mock(ResultadoRepository.class);
    final AvaliacoesPendentes pendencias = mock(AvaliacoesPendentes.class);
    final ResultadoService service = new ResultadoService(decisoes, criterios, opcoes, avaliacoes,
            argumentos, fatores, resultados, pendencias);
    final Decisao decisao = new Decisao();
    final Opcao a = new Opcao(), b = new Opcao();
    final Map<Long, Resultado> gravados = new ConcurrentHashMap<>();
    long proximoCriterio = 31L;

    @BeforeEach void preparar() {
        decisao.setId(12L); decisao.setTitulo("Uma escolha"); decisao.setEtapaAtual(6);
        decisao.setReflexao101010ConcluidaEm(LocalDateTime.of(2026, 1, 1, 10, 0));
        decisao.setInversaoConcluidaEm(LocalDateTime.of(2026, 1, 1, 11, 0));
        decisao.setDataAtualizacao(LocalDateTime.of(2026, 1, 1, 11, 0));
        a.setId(21L); a.setNome("Trabalhar"); a.setDecisao(decisao);
        b.setId(22L); b.setNome("Estudar"); b.setDecisao(decisao);
        when(decisoes.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(decisao));
        when(decisoes.buscarParaAtualizar(12L, 7L)).thenReturn(Optional.of(decisao));
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a, b));
        when(argumentos.findByOpcaoIdOrderByIdAsc(anyLong())).thenReturn(List.of(argumento(TipoProContra.PRO), argumento(TipoProContra.CONTRA)));
        when(fatores.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(new ItemGut()));
        when(resultados.findByOpcaoId(anyLong())).thenAnswer(inv -> Optional.ofNullable(gravados.get(inv.<Long>getArgument(0))));
        when(resultados.save(any())).thenAnswer(inv -> {
            Resultado r = inv.getArgument(0); gravados.put(r.getOpcao().getId(), r); return r;
        });
        avaliar(new Object[] {"Custo", 3, 4, 2});
    }

    ProContra argumento(TipoProContra tipo) { var item = new ProContra(); item.setTipo(tipo); return item; }

    /**
     * Define os critérios e as notas do cenário.
     *
     * @param linhas cada linha é {@code {nome, peso, notaA, notaB}}
     */
    void avaliar(Object[]... linhas) {
        var lista = new ArrayList<Criterio>(); var notas = new ArrayList<Avaliacao>();
        for (var linha : linhas) {
            var criterio = new Criterio(); criterio.setId(proximoCriterio++);
            criterio.setNome((String) linha[0]); criterio.setPeso((Integer) linha[1]);
            criterio.setPersonalizado(CatalogoCriterios.buscar((String) linha[0]) == null);
            lista.add(criterio);
            notas.add(nota(criterio, a, (Integer) linha[2])); notas.add(nota(criterio, b, (Integer) linha[3]));
        }
        when(criterios.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(lista);
        when(avaliacoes.findByCriterioDecisaoId(12L)).thenReturn(notas);
    }
    Avaliacao nota(Criterio criterio, Opcao opcao, int valor) {
        var item = new Avaliacao(); item.setCriterio(criterio); item.setOpcao(opcao); item.setValor(valor); return item;
    }
    int pontos(ResultadoComparativoResponse r, int alternativa) { return r.alternativas().get(alternativa).pontuacao(); }
    void semEscrita() {
        verify(resultados, never()).save(any()); verify(resultados, never()).saveAndFlush(any());
        verify(decisoes, never()).save(any()); verify(decisoes, never()).saveAndFlush(any());
    }

    // ---------- Finalização (comportamento já existente) ----------

    @Test void finalizarGravaPontuacoesEConclui() {
        var r = service.finalizar(7L, 12L);
        assertEquals(StatusDecisao.CONCLUIDA, decisao.getStatus());
        assertEquals(StatusDecisao.CONCLUIDA, r.status());
        assertNotNull(r.finalizadaEm());
        assertEquals(12, pontos(r, 0)); assertEquals(6, pontos(r, 1));
        assertEquals(12, gravados.get(21L).getPontuacaoTotal()); assertEquals(6, gravados.get(22L).getPontuacaoTotal());
        verify(decisoes).saveAndFlush(decisao);
    }

    @Test void finalizarComPendenciaMantemEmAndamentoSemGravar() {
        when(pendencias.possuiPendencia(12L)).thenReturn(true);
        assertThrows(EtapaInvalidaException.class, () -> service.finalizar(7L, 12L));
        assertEquals(StatusDecisao.EM_ANDAMENTO, decisao.getStatus());
        verify(resultados, never()).save(any()); verify(decisoes, never()).saveAndFlush(any());
    }

    @Test void finalizarSemInversaoConcluidaEhRecusado() {
        decisao.setInversaoConcluidaEm(null);
        assertThrows(EtapaInvalidaException.class, () -> service.finalizar(7L, 12L));
        assertEquals(StatusDecisao.EM_ANDAMENTO, decisao.getStatus());
    }

    @Test void finalizarNovamenteNaoVoltaNemRegrava() {
        service.finalizar(7L, 12L);
        clearInvocations(resultados, decisoes);
        var r = service.finalizar(7L, 12L);
        assertEquals(StatusDecisao.CONCLUIDA, r.status());
        verify(resultados, never()).save(any()); verify(decisoes, never()).saveAndFlush(any());
    }

    @Test void finalizarAtualizaResultadoExistenteSemInserirOutraLinha() {
        var legado = new Resultado(); legado.setId(90L); legado.setOpcao(a); legado.setPontuacaoTotal(99); legado.setRequerAtualizacao(true);
        gravados.put(21L, legado);
        service.finalizar(7L, 12L);
        assertSame(legado, gravados.get(21L), "deve atualizar a linha existente, não criar outra");
        assertEquals(12, legado.getPontuacaoTotal()); assertFalse(legado.isRequerAtualizacao());
    }

    @Test void decisaoDeOutraContaNaoEhFinalizada() {
        assertThrows(DecisaoNaoEncontradaException.class, () -> service.finalizar(99L, 12L));
    }

    // ---------- Empate ----------

    @Test void pontuacoesIguaisSaoEmpateSemRecomendacao() {
        avaliar(new Object[] {"Custo", 3, 4, 4});
        var r = service.consultar(7L, 12L);
        assertTrue(r.empate()); assertNull(r.recomendadaId());
        assertEquals(12, pontos(r, 0)); assertEquals(12, pontos(r, 1));
    }

    @Test void aMaiorQueBRecomendaA() {
        var r = service.consultar(7L, 12L);
        assertFalse(r.empate()); assertEquals(21L, r.recomendadaId());
    }

    @Test void bMaiorQueARecomendaBMesmoSendoCadastradaDepois() {
        avaliar(new Object[] {"Custo", 3, 2, 5});
        var r = service.consultar(7L, 12L);
        assertFalse(r.empate()); assertEquals(22L, r.recomendadaId());
    }

    @Test void empateComDistribuicoesDiferentesDeNotas() {
        avaliar(new Object[] {"Segurança", 3, 5, 1}, new Object[] {"Crescimento", 3, 1, 5},
                new Object[] {"Risco", 2, 2, 2}, new Object[] {"Custo", 1, 3, 3});
        var r = service.consultar(7L, 12L);
        assertEquals(29, pontos(r, 0)); assertEquals(29, pontos(r, 1));
        assertNotEquals(r.alternativas().get(0).criterios(), r.alternativas().get(1).criterios());
        assertTrue(r.empate()); assertNull(r.recomendadaId());
    }

    @Test void empateMisturandoRiscoEPeso() {
        // A: 5*2 + (6-3)*2 = 16 ; B: 3*2 + (6-1)*2 = 16
        avaliar(new Object[] {"Segurança", 2, 5, 3}, new Object[] {"Risco", 2, 3, 1});
        var r = service.consultar(7L, 12L);
        assertEquals(16, pontos(r, 0)); assertEquals(16, pontos(r, 1));
        assertTrue(r.empate()); assertNull(r.recomendadaId());
    }

    @Test void finalizarDecisaoEmpatada() {
        avaliar(new Object[] {"Custo", 3, 4, 4});
        var r = service.finalizar(7L, 12L);
        assertEquals(StatusDecisao.CONCLUIDA, r.status()); assertTrue(r.empate()); assertNull(r.recomendadaId());
        assertEquals(12, gravados.get(21L).getPontuacaoTotal()); assertEquals(12, gravados.get(22L).getPontuacaoTotal());
        var depois = service.consultar(7L, 12L);
        assertTrue(depois.empate()); assertEquals(StatusDecisao.CONCLUIDA, depois.status());
    }

    // ---------- Consulta sem escrita / concorrência ----------

    @Test void consultarEmAndamentoSemResultadoPersistidoNaoGrava() {
        var r = service.consultar(7L, 12L);
        assertEquals(StatusDecisao.EM_ANDAMENTO, r.status()); assertNull(r.finalizadaEm());
        assertTrue(gravados.isEmpty());
        semEscrita();
    }

    @Test void consultarComResultadoAntigoPersistidoUsaNotasAtuaisENaoGrava() {
        var antigo = new Resultado(); antigo.setId(90L); antigo.setOpcao(a); antigo.setPontuacaoTotal(99); antigo.setRequerAtualizacao(true);
        gravados.put(21L, antigo);
        var r = service.consultar(7L, 12L);
        assertEquals(12, pontos(r, 0), "a consulta não pode exibir um total desatualizado");
        assertEquals(99, antigo.getPontuacaoTotal()); assertTrue(antigo.isRequerAtualizacao());
        semEscrita();
    }

    @Test void consultasRepetidasDevolvemOMesmoResultadoSemGravar() {
        var primeira = service.consultar(7L, 12L);
        for (int i = 0; i < 5; i++) assertEquals(primeira, service.consultar(7L, 12L));
        semEscrita();
    }

    @Test void duasConsultasSimultaneasNaoGravamNemFalham() throws Exception {
        var largada = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<ResultadoComparativoResponse> tarefa = () -> { largada.await(); return service.consultar(7L, 12L); };
            var f1 = pool.submit(tarefa); var f2 = pool.submit(tarefa);
            largada.countDown();
            var r1 = f1.get(5, TimeUnit.SECONDS); var r2 = f2.get(5, TimeUnit.SECONDS);
            assertEquals(r1, r2);
        } finally { pool.shutdownNow(); }
        assertTrue(gravados.isEmpty());
        semEscrita();
    }

    @Test void consultarDecisaoConcluidaNaoGravaNemAlteraStatus() {
        service.finalizar(7L, 12L);
        var data = decisao.getDataAtualizacao();
        clearInvocations(resultados, decisoes);
        var r = service.consultar(7L, 12L);
        assertEquals(StatusDecisao.CONCLUIDA, r.status()); assertEquals(data, r.finalizadaEm());
        assertEquals(12, pontos(r, 0));
        semEscrita();
    }

    @Test void consultarConcluidaComTotalLegadoExibeCalculoCorrigidoSemRegravar() {
        service.finalizar(7L, 12L);
        gravados.get(21L).setPontuacaoTotal(13);
        clearInvocations(resultados, decisoes);
        var r = service.consultar(7L, 12L);
        assertEquals(12, pontos(r, 0), "total e detalhes vêm das notas gravadas, sempre coerentes");
        assertEquals(13, gravados.get(21L).getPontuacaoTotal(), "o valor antigo continua intocado no banco");
        semEscrita();
    }

    @Test void consultarDecisaoDeOutraContaEhRecusado() {
        assertThrows(DecisaoNaoEncontradaException.class, () -> service.consultar(99L, 12L));
    }

    // ---------- Risco (MENOR_MELHOR) ----------

    @Test void riscoUmTresECincoComPesoTres() {
        avaliar(new Object[] {"Risco", 3, 1, 3});
        var r = service.consultar(7L, 12L);
        assertEquals(15, pontos(r, 0)); assertEquals(9, pontos(r, 1));
        avaliar(new Object[] {"Risco", 3, 5, 3});
        r = service.consultar(7L, 12L);
        assertEquals(3, pontos(r, 0)); assertEquals(9, pontos(r, 1));
        var detalhe = r.alternativas().get(0).criterios().get(0);
        assertEquals(5, detalhe.nota(), "a nota informada é preservada");
        assertEquals(1, detalhe.notaAjustada()); assertEquals("MENOR_MELHOR", detalhe.sentido());
    }

    @Test void riscoComPesosDiferentes() {
        avaliar(new Object[] {"Risco", 1, 2, 4});
        var r = service.consultar(7L, 12L);
        assertEquals(4, pontos(r, 0)); assertEquals(2, pontos(r, 1)); assertEquals(5, r.pontuacaoMaxima());
        avaliar(new Object[] {"Risco", 5, 2, 4});
        r = service.consultar(7L, 12L);
        assertEquals(20, pontos(r, 0)); assertEquals(10, pontos(r, 1)); assertEquals(25, r.pontuacaoMaxima());
    }

    @Test void riscoBaixoFavoreceERiscoAltoReduz() {
        avaliar(new Object[] {"Risco", 3, 1, 5});
        var r = service.consultar(7L, 12L);
        assertTrue(pontos(r, 0) > pontos(r, 1)); assertEquals(21L, r.recomendadaId());
        avaliar(new Object[] {"Risco", 3, 5, 1});
        r = service.consultar(7L, 12L);
        assertTrue(pontos(r, 0) < pontos(r, 1)); assertEquals(22L, r.recomendadaId());
    }

    @Test void riscoCombinadoComOutrosCriteriosEPersonalizado() {
        avaliar(new Object[] {"Segurança", 3, 1, 5}, new Object[] {"Crescimento", 3, 2, 4},
                new Object[] {"Risco", 3, 3, 3}, new Object[] {"Impacto emocional", 3, 4, 2},
                new Object[] {"Alinhamento a valores", 3, 5, 1}, new Object[] {"Distância do trabalho", 2, 5, 1});
        var r = service.consultar(7L, 12L);
        // A: 3+6+9+12+15 + 10 = 55 ; B: 15+12+9+6+3 + 2 = 47
        assertEquals(55, pontos(r, 0)); assertEquals(47, pontos(r, 1));
        assertEquals(85, r.pontuacaoMaxima());
        var personalizado = r.alternativas().get(0).criterios().get(5);
        assertEquals("MAIOR_MELHOR", personalizado.sentido()); assertEquals(5, personalizado.notaAjustada());
        for (var alternativa : r.alternativas()) {
            int soma = alternativa.criterios().stream().mapToInt(ResultadoComparativoResponse.Criterio::pontuacao).sum();
            assertEquals(alternativa.pontuacao(), soma, "o total é a soma dos critérios exibidos");
            for (var c : alternativa.criterios()) assertEquals(c.notaAjustada() * c.peso(), c.pontuacao());
        }
    }

    @Test void riscoNaoEhInvertidoDuasVezesAoFinalizar() {
        avaliar(new Object[] {"Risco", 3, 1, 5});
        var r = service.finalizar(7L, 12L);
        assertEquals(15, pontos(r, 0)); assertEquals(3, pontos(r, 1));
        assertEquals(15, gravados.get(21L).getPontuacaoTotal()); assertEquals(3, gravados.get(22L).getPontuacaoTotal());
        assertEquals(15, pontos(service.consultar(7L, 12L), 0));
    }

    @Test void catalogoInverteSomenteMenorMelhor() {
        assertEquals("MENOR_MELHOR", CatalogoCriterios.sentido("  risco "));
        assertEquals("MAIOR_MELHOR", CatalogoCriterios.sentido("Segurança"));
        assertEquals("MAIOR_MELHOR", CatalogoCriterios.sentido("Criterio próprio"));
        assertEquals(5, CatalogoCriterios.notaFavoravel(1, "MENOR_MELHOR"));
        assertEquals(3, CatalogoCriterios.notaFavoravel(3, "MENOR_MELHOR"));
        assertEquals(1, CatalogoCriterios.notaFavoravel(5, "MENOR_MELHOR"));
        assertEquals(4, CatalogoCriterios.notaFavoravel(4, "MAIOR_MELHOR"));
    }
}
