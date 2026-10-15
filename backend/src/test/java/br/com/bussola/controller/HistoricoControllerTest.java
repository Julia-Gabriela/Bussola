package br.com.bussola.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.AutenticacaoService;
import br.com.bussola.service.HistoricoService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HistoricoController.class)
@Import({HistoricoService.class,SecurityConfig.class,CadastroExceptionHandler.class})
class HistoricoControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private DecisaoRepository decisoes;
    @MockitoBean private AutenticacaoService autenticacao;
    private Decisao decisao;
    private static final String TOKEN="Bearer teste";

    @BeforeEach void preparar() {
        when(autenticacao.autenticar("teste")).thenReturn(new UsuarioResponse(7L,"Teste","teste@example.com"));
        decisao=new Decisao();decisao.setId(12L);decisao.setTitulo("Minha escolha");decisao.setEtapaAtual(3);decisao.setDataAtualizacao(LocalDateTime.of(2026,10,14,12,0));
        when(decisoes.pesquisarHistorico(eq(7L),any(),anyString(),any(Pageable.class))).thenAnswer(inv->new PageImpl<>(List.of(decisao),inv.getArgument(3),1));
        when(decisoes.buscarParaAtualizar(12L,7L)).thenReturn(Optional.of(decisao));
        when(decisoes.countByUsuarioIdAndStatus(7L,StatusDecisao.EM_ANDAMENTO)).thenReturn(1L);
    }

    @Test void listaSomenteUsuarioAutenticadoSemExporEntidade() throws Exception {
        mvc.perform(get("/decisoes").header("Authorization",TOKEN).param("usuarioId","99"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.decisoes[0].id").value(12)).andExpect(jsonPath("$.decisoes[0].usuario").doesNotExist())
            .andExpect(jsonPath("$.totalRegistros").value(1)).andExpect(jsonPath("$.emAndamento").value(1));
        verify(decisoes).pesquisarHistorico(eq(7L),isNull(),eq("%%"),any());
    }

    @Test void combinaFiltrosEEscapaCuringasSemMudarContagemGlobal() throws Exception {
        mvc.perform(get("/decisoes").header("Authorization",TOKEN).param("busca","  50%_!  ")
            .param("status","CONCLUIDA").param("ordem","titulo").param("pagina","2").param("tamanho","5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.emAndamento").value(1));
        var pagina=ArgumentCaptor.forClass(Pageable.class);
        verify(decisoes).pesquisarHistorico(eq(7L),eq(StatusDecisao.CONCLUIDA),eq("%50!%!_!!%"),pagina.capture());
        assertEquals(2,pagina.getValue().getPageNumber());assertEquals(5,pagina.getValue().getPageSize());
        assertEquals("titulo: ASC,id: ASC",pagina.getValue().getSort().toString().replace(", ",","));
    }

    @ParameterizedTest @ValueSource(strings={"recentes","antigas","titulo"})
    void permiteApenasOrdenacoesPrevistas(String ordem) throws Exception {
        mvc.perform(get("/decisoes").header("Authorization",TOKEN).param("ordem",ordem)).andExpect(status().isOk());
        var pagina=ArgumentCaptor.forClass(Pageable.class);
        verify(decisoes).pesquisarHistorico(eq(7L),isNull(),eq("%%"),pagina.capture());
        assertEquals(ordem.equals("recentes")?"DESC":"ASC",pagina.getValue().getSort().iterator().next().getDirection().name());
    }

    @ParameterizedTest @ValueSource(strings={"pagina=-1","tamanho=0","tamanho=51","ordem=senha","status=INVENTADO","pagina=abc"})
    void rejeitaFiltrosInvalidos(String parametro) throws Exception {
        String[] partes=parametro.split("=");
        mvc.perform(get("/decisoes").header("Authorization",TOKEN).param(partes[0],partes[1])).andExpect(status().isBadRequest());
        verify(decisoes,never()).pesquisarHistorico(any(),any(),any(),any());
    }

    @Test void pesquisaVaziaRetornaListaVazia() throws Exception {
        when(decisoes.pesquisarHistorico(eq(7L),any(),anyString(),any())).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/decisoes").header("Authorization",TOKEN)).andExpect(status().isOk())
            .andExpect(jsonPath("$.decisoes").isEmpty()).andExpect(jsonPath("$.totalRegistros").value(0));
    }

    @Test void exigeConfirmacaoAntesDeExcluir() throws Exception {
        mvc.perform(delete("/decisoes/12").header("Authorization",TOKEN)).andExpect(status().isBadRequest());
        mvc.perform(delete("/decisoes/12").header("Authorization",TOKEN).param("confirmar","false")).andExpect(status().isBadRequest());
        verify(decisoes,never()).delete(any());
    }

    @ParameterizedTest @ValueSource(strings={"EM_ANDAMENTO","CONCLUIDA"})
    void excluiDecisaoPropriaConfirmada(String status) throws Exception {
        decisao.setStatus(StatusDecisao.valueOf(status));
        mvc.perform(delete("/decisoes/12").header("Authorization",TOKEN).param("confirmar","true")).andExpect(status().isNoContent());
        verify(decisoes).delete(decisao);verify(decisoes).flush();
    }

    @Test void naoExcluiDecisaoDeOutraPessoa() throws Exception {
        mvc.perform(delete("/decisoes/99").header("Authorization",TOKEN).param("confirmar","true")).andExpect(status().isNotFound());
        verify(decisoes,never()).delete(any());
    }

    @Test void exigeAutenticacaoEPermiteCors() throws Exception {
        mvc.perform(get("/decisoes")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/decisoes/12").param("confirmar","true")).andExpect(status().isUnauthorized());
        mvc.perform(options("/decisoes/12").header("Origin","http://127.0.0.1:8765").header("Access-Control-Request-Method","DELETE"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:8765"));
        verifyNoInteractions(decisoes);
    }
}
