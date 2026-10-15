package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.bussola.dto.request.SalvarReflexoes101010Request;
import br.com.bussola.dto.request.SalvarReflexoes101010Request.Reflexao;
import br.com.bussola.exception.*;
import br.com.bussola.model.entity.*;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Reflexoes101010ServiceTest {
    final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    final OpcaoRepository opcoes = mock(OpcaoRepository.class);
    final Reflexao101010Repository reflexoes = mock(Reflexao101010Repository.class);
    final ResultadoRepository resultados = mock(ResultadoRepository.class);
    final Reflexoes101010Service service = new Reflexoes101010Service(decisoes, opcoes, reflexoes, resultados);
    final Decisao decisao = new Decisao();
    final List<Reflexao101010> salvas = new ArrayList<>();
    final SalvarReflexoes101010Request completa = new SalvarReflexoes101010Request(
            new Reflexao(21L, "  Alívio  ", "Aprendizado", "Experiência"),
            new Reflexao(22L, "Tranquilidade", "Estabilidade", "Equilíbrio"));

    @BeforeEach void preparar() {
        decisao.setId(12L); decisao.setTitulo("Minha decisão"); decisao.setEtapaAtual(6);
        when(decisoes.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(decisao));
        when(decisoes.buscarParaAtualizar(12L, 7L)).thenReturn(Optional.of(decisao));
        var a = new Opcao(); a.setId(21L); a.setNome("Trabalhar"); a.setDecisao(decisao);
        var b = new Opcao(); b.setId(22L); b.setNome("Estudar"); b.setDecisao(decisao);
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a,b));
        when(reflexoes.findByOpcaoDecisaoId(12L)).thenReturn(salvas);
        when(reflexoes.saveAll(anyList())).thenAnswer(inv -> {
            List<Reflexao101010> lista = inv.getArgument(0);
            for (var item : lista) if (item.getId() == null) { item.setId(100L + salvas.size()); salvas.add(item); }
            return lista;
        });
    }

    @Test void consultarNaoCriaReflexoesNemInventaTextos() {
        var resposta = service.consultar(7L,12L);
        assertEquals("Trabalhar",resposta.opcaoA().nome()); assertNull(resposta.opcaoA().curtoPrazo());
        assertNull(resposta.opcaoB().reflexaoId()); assertFalse(resposta.concluida());
        verify(reflexoes,never()).saveAll(anyList()); verify(decisoes,never()).save(any());
    }
    @Test void salvaParcialNormalizaELimpaSemApagarIds() {
        var parcial = new SalvarReflexoes101010Request(new Reflexao(21L,"  Calma  ",null,"  "),new Reflexao(22L,null,null,null));
        var resposta=service.salvar(7L,12L,parcial);
        assertEquals("Calma",resposta.opcaoA().curtoPrazo()); assertNull(resposta.opcaoA().longoPrazo());
        assertNull(resposta.opcaoB().reflexaoId()); assertFalse(resposta.concluida());
        var limpa=service.salvar(7L,12L,new SalvarReflexoes101010Request(new Reflexao(21L,null,null,null),parcial.opcaoB()));
        assertEquals(resposta.opcaoA().reflexaoId(),limpa.opcaoA().reflexaoId()); assertNull(limpa.opcaoA().curtoPrazo());
        assertEquals(6,decisao.getEtapaAtual()); assertEquals(StatusDecisao.EM_ANDAMENTO,decisao.getStatus());
    }
    @Test void concluiAtomicamenteEReenvioNaoDuplicaNemAtualizaData() {
        var primeira=service.concluir(7L,12L,completa);
        var repetida=service.concluir(7L,12L,completa);
        assertTrue(primeira.concluida()); assertNotNull(primeira.concluidaEm()); assertEquals(primeira,repetida);
        assertEquals(2,salvas.size()); assertEquals(6,decisao.getEtapaAtual());
        assertEquals(StatusDecisao.EM_ANDAMENTO,decisao.getStatus()); verify(reflexoes,times(1)).saveAll(anyList());
        verify(decisoes,times(1)).save(decisao);
        assertTrue(salvas.stream().allMatch(r->r.getOpcao().getDecisao()==decisao));
    }
    @Test void exigeCadaUmaDasSeisRespostasSemSalvarParcialNaConclusao() {
        for(int ausente=0;ausente<6;ausente++) {
            var valores=new String[]{"a","b","c","d","e","f"};valores[ausente]="  ";
            var request=new SalvarReflexoes101010Request(new Reflexao(21L,valores[0],valores[1],valores[2]),new Reflexao(22L,valores[3],valores[4],valores[5]));
            assertThrows(ReflexoesInvalidasException.class,()->service.concluir(7L,12L,request));
        }
        assertTrue(salvas.isEmpty());assertNull(decisao.getReflexao101010ConcluidaEm());
        verify(reflexoes,never()).saveAll(anyList());verify(decisoes,never()).save(any());
    }
    @Test void rejeitaOpcoesInvertidasOuDeOutraDecisao() {
        var invertida=new SalvarReflexoes101010Request(completa.opcaoB(),completa.opcaoA());
        var estrangeira=new SalvarReflexoes101010Request(new Reflexao(99L,"a","b","c"),completa.opcaoB());
        assertThrows(ReflexoesInvalidasException.class,()->service.salvar(7L,12L,invertida));
        assertThrows(ReflexoesInvalidasException.class,()->service.concluir(7L,12L,estrangeira));
        verify(reflexoes,never()).saveAll(anyList());
    }
    @Test void rejeitaTextoAcimaDoLimiteSemGravar() {
        var invalida=new SalvarReflexoes101010Request(new Reflexao(21L,"x".repeat(16001),null,null),completa.opcaoB());
        assertThrows(ReflexoesInvalidasException.class,()->service.salvar(7L,12L,invalida));
        verify(reflexoes,never()).saveAll(anyList());
    }
    @Test void protegeTitularidadeEOrdemDasEtapas() {
        assertThrows(DecisaoNaoEncontradaException.class,()->service.consultar(8L,12L));
        assertThrows(DecisaoNaoEncontradaException.class,()->service.salvar(8L,12L,completa));
        for(int etapa=1;etapa<6;etapa++) {
            decisao.setEtapaAtual(etapa);
            assertThrows(EtapaInvalidaException.class,()->service.consultar(7L,12L));
            assertThrows(EtapaInvalidaException.class,()->service.concluir(7L,12L,completa));
        }
        verifyNoInteractions(reflexoes,resultados);
    }
    @Test void decisaoConcluidaPermiteApenasConsulta() {
        service.concluir(7L,12L,completa); decisao.setStatus(StatusDecisao.CONCLUIDA);
        assertTrue(service.consultar(7L,12L).concluida());
        assertThrows(EtapaInvalidaException.class,()->service.salvar(7L,12L,completa));
        assertThrows(EtapaInvalidaException.class,()->service.concluir(7L,12L,completa));
        verify(reflexoes,times(1)).saveAll(anyList());
    }
    @Test void revisaoPreservaIdsExigeNovaConclusaoEMarcaSomenteResultadoAfetado() {
        var primeira=service.concluir(7L,12L,completa);
        var resultado=new Resultado();resultado.setId(70L);resultado.setPontuacaoTotal(32);
        when(resultados.findByOpcaoId(21L)).thenReturn(Optional.of(resultado));
        clearInvocations(resultados);
        var alterada=new SalvarReflexoes101010Request(new Reflexao(21L,"Outra perspectiva","Aprendizado","Experiência"),completa.opcaoB());
        var revisada=service.salvar(7L,12L,alterada);
        assertEquals(primeira.opcaoA().reflexaoId(),revisada.opcaoA().reflexaoId());
        assertEquals(primeira.opcaoB(),revisada.opcaoB()); assertFalse(revisada.concluida());
        assertTrue(resultado.isRequerAtualizacao()); assertEquals(32,resultado.getPontuacaoTotal());
        verify(resultados,never()).findByOpcaoId(22L);
        assertTrue(service.concluir(7L,12L,alterada).concluida());
    }
    @Test void reenviarSalvamentoIdenticoNaoDesfazConclusao() {
        var primeira=service.concluir(7L,12L,completa);
        assertEquals(primeira,service.salvar(7L,12L,completa));
        verify(reflexoes,times(1)).saveAll(anyList());
    }
    @Test void exigeExatamenteDuasOpcoes() {
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of());
        assertThrows(EtapaInvalidaException.class,()->service.salvar(7L,12L,completa));
        verifyNoInteractions(reflexoes);
    }
}
