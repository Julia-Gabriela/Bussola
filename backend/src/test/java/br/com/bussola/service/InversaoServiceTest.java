package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import br.com.bussola.dto.request.SalvarInversaoRequest;
import br.com.bussola.dto.request.SalvarInversaoRequest.Reflexao;
import br.com.bussola.exception.*;
import br.com.bussola.model.entity.*;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InversaoServiceTest {
    final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    final OpcaoRepository opcoes = mock(OpcaoRepository.class);
    final InversaoRepository inversoes = mock(InversaoRepository.class);
    final ResultadoRepository resultados = mock(ResultadoRepository.class);
    final InversaoService service = new InversaoService(decisoes, opcoes, inversoes, resultados);
    final Decisao decisao = new Decisao();
    final Opcao a = new Opcao(), b = new Opcao();
    final List<Inversao> salvas = new ArrayList<>();
    final SalvarInversaoRequest completa = new SalvarInversaoRequest(
            new Reflexao(21L, "Sobrecarga", "Planejar horários"), new Reflexao(22L, "Isolamento", "Buscar apoio"));

    @BeforeEach void preparar() {
        decisao.setId(12L); decisao.setTitulo("Uma escolha"); decisao.setEtapaAtual(6);
        a.setId(21L); a.setNome("Trabalhar"); a.setDecisao(decisao);
        b.setId(22L); b.setNome("Estudar"); b.setDecisao(decisao);
        when(decisoes.findByIdAndUsuarioId(12L,7L)).thenReturn(Optional.of(decisao));
        when(decisoes.buscarParaAtualizar(12L,7L)).thenReturn(Optional.of(decisao));
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a,b));
        when(inversoes.findByOpcaoDecisaoIdOrderByIdAsc(12L)).thenAnswer(inv -> new ArrayList<>(salvas));
        when(inversoes.saveAll(anyList())).thenAnswer(inv -> {
            List<Inversao> itens = inv.getArgument(0);
            for (var item : itens) if (item.getId() == null) {
                item.setId(100L + salvas.size()); salvas.add(item);
            }
            return itens;
        });
    }

    @Test void consultaNaoCriaTextos() {
        var r = service.consultar(7L,12L);
        assertNull(r.opcaoA().fracassos()); assertNull(r.opcaoB().prevencoes());
        assertFalse(r.concluida()); verify(inversoes,never()).saveAll(anyList());
    }

    @Test void parcialNormalizaPreservaIdELimpaSemApagarOutrasPerspectivas() {
        var parcial = new SalvarInversaoRequest(new Reflexao(21L,"  Risco\nreal  ",null),new Reflexao(22L,null," "));
        var primeira = service.salvar(7L,12L,parcial);
        assertEquals("Risco\nreal",primeira.opcaoA().fracassos()); assertEquals(1,salvas.size());
        var vazia = service.salvar(7L,12L,new SalvarInversaoRequest(new Reflexao(21L,null,null),parcial.opcaoB()));
        assertNull(vazia.opcaoA().fracassos()); assertEquals(primeira.opcaoA().fracassosId(),vazia.opcaoA().fracassosId());
        assertEquals(1,salvas.size()); assertFalse(vazia.concluida());
    }

    @Test void conclusaoIdempotenteNaoEncerraDecisaoNemAfeta101010() {
        var data101010 = java.time.LocalDateTime.of(2026,1,1,12,0);
        decisao.setReflexao101010ConcluidaEm(data101010);
        var primeira = service.concluir(7L,12L,completa);
        assertTrue(primeira.concluida()); assertEquals(primeira,service.concluir(7L,12L,completa));
        assertEquals(primeira,service.salvar(7L,12L,completa));
        assertEquals(4,salvas.size()); assertEquals(6,decisao.getEtapaAtual());
        assertEquals(StatusDecisao.EM_ANDAMENTO,decisao.getStatus());
        assertEquals(data101010,decisao.getReflexao101010ConcluidaEm());
        verify(inversoes,times(1)).saveAll(anyList()); verify(decisoes,times(1)).save(decisao);
    }

    @Test void exigeTodosOsQuatroCamposAntesDeGravar() {
        for (int i=0;i<4;i++) {
            String[] campos={"a","b","c","d"}; campos[i]=" ";
            var r=new SalvarInversaoRequest(new Reflexao(21L,campos[0],campos[1]),new Reflexao(22L,campos[2],campos[3]));
            assertThrows(ReflexoesInvalidasException.class,()->service.concluir(7L,12L,r));
        }
        verify(inversoes,never()).saveAll(anyList()); assertNull(decisao.getInversaoConcluidaEm());
    }

    @Test void rejeitaIdsInvalidosCamposAusentesELimite() {
        assertThrows(ReflexoesInvalidasException.class,()->service.salvar(7L,12L,null));
        assertThrows(ReflexoesInvalidasException.class,()->service.salvar(7L,12L,new SalvarInversaoRequest(null,completa.opcaoB())));
        assertThrows(ReflexoesInvalidasException.class,()->service.salvar(7L,12L,new SalvarInversaoRequest(completa.opcaoB(),completa.opcaoA())));
        assertThrows(ReflexoesInvalidasException.class,()->service.salvar(7L,12L,new SalvarInversaoRequest(new Reflexao(99L,"a","b"),completa.opcaoB())));
        assertThrows(ReflexoesInvalidasException.class,()->service.salvar(7L,12L,new SalvarInversaoRequest(new Reflexao(21L,"x".repeat(16001),null),completa.opcaoB())));
        verify(inversoes,never()).saveAll(anyList());
    }

    @Test void protegeDonoEtapaEDecisaoConcluida() {
        assertThrows(DecisaoNaoEncontradaException.class,()->service.consultar(8L,12L));
        assertThrows(DecisaoNaoEncontradaException.class,()->service.salvar(8L,12L,completa));
        for (int etapa=1;etapa<6;etapa++) {
            decisao.setEtapaAtual(etapa);
            assertThrows(EtapaInvalidaException.class,()->service.consultar(7L,12L));
            assertThrows(EtapaInvalidaException.class,()->service.concluir(7L,12L,completa));
        }
        decisao.setEtapaAtual(6); decisao.setStatus(StatusDecisao.CONCLUIDA);
        assertNotNull(service.consultar(7L,12L));
        assertThrows(EtapaInvalidaException.class,()->service.salvar(7L,12L,completa));
        assertThrows(EtapaInvalidaException.class,()->service.concluir(7L,12L,completa));
        verify(inversoes,never()).saveAll(anyList());
    }

    @Test void revisaoPreservaIdsEConteudoDaOutraOpcaoESinalizaResultado() {
        var original=service.concluir(7L,12L,completa);
        var resultado=new Resultado(); resultado.setPontuacaoTotal(42);
        when(resultados.findByOpcaoId(21L)).thenReturn(Optional.of(resultado)); clearInvocations(resultados);
        var revisada=service.salvar(7L,12L,new SalvarInversaoRequest(new Reflexao(21L,"Outro risco",completa.opcaoA().prevencoes()),completa.opcaoB()));
        assertFalse(revisada.concluida()); assertEquals(original.opcaoA().fracassosId(),revisada.opcaoA().fracassosId());
        assertEquals(original.opcaoB(),revisada.opcaoB()); assertEquals(4,salvas.size());
        assertTrue(resultado.isRequerAtualizacao()); assertEquals(42,resultado.getPontuacaoTotal());
        verify(resultados,never()).findByOpcaoId(22L);
    }

    @Test void preservaRegistrosLegadosERecusaDuplicatasConhecidas() {
        var legado=new Inversao(); legado.setId(1L); legado.setOpcao(a); legado.setPerspectiva("Outra perspectiva"); legado.setReflexao("Texto antigo"); salvas.add(legado);
        service.concluir(7L,12L,completa);
        assertEquals("Texto antigo",legado.getReflexao()); assertEquals(5,salvas.size());
        var duplicada=new Inversao(); duplicada.setId(999L); duplicada.setOpcao(a); duplicada.setPerspectiva("FRACASSOS"); salvas.add(duplicada);
        clearInvocations(inversoes);
        assertThrows(EtapaInvalidaException.class,()->service.salvar(7L,12L,completa));
        verify(inversoes,never()).saveAll(anyList());
    }

    @Test void exigeDuasOpcoesDefinidas() {
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a));
        assertThrows(EtapaInvalidaException.class,()->service.consultar(7L,12L));
    }
}
