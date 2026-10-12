package br.com.bussola.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.IdeiaBrainstorming;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.IdeiaBrainstormingRepository;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.AutenticacaoService;
import br.com.bussola.service.FluxoDecisaoService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FluxoDecisaoController.class)
@Import({SecurityConfig.class, FluxoDecisaoService.class, CadastroExceptionHandler.class})
class FluxoDecisaoControllerTest {
    private static final String TOKEN = "Bearer token-teste";
    private static final String IDEIA = "{\"conteudo\":\"  Ganhar experiência  \",\"categoria\":\"FAVORAVEL\"}";
    @Autowired private MockMvc mvc;
    @MockitoBean private AutenticacaoService autenticacao;
    @MockitoBean private DecisaoRepository decisoes;
    @MockitoBean private IdeiaBrainstormingRepository ideias;
    private Decisao decisao;

    @BeforeEach
    void preparar() {
        when(autenticacao.autenticar("token-teste"))
                .thenReturn(new UsuarioResponse(7L, "Teste", "teste@example.com"));
        decisao = new Decisao();
        decisao.setId(12L);
        decisao.setTitulo("Meu projeto");
        when(decisoes.buscarParaAtualizar(12L, 7L)).thenReturn(Optional.of(decisao));
        when(decisoes.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(decisao));
        when(decisoes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ideias.save(any())).thenAnswer(inv -> {
            IdeiaBrainstorming ideia = inv.getArgument(0);
            if (ideia.getId() == null) ideia.setId(20L);
            return ideia;
        });
    }

    @Test
    void salvaContextoSemCriarOutraDecisaoNemAvancar() throws Exception {
        mvc.perform(put("/decisoes/12/contexto").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"  Novo título  \",\"contexto\":\"  Contexto  \",\"etapaAtual\":6}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.titulo").value("Novo título"))
                .andExpect(jsonPath("$.contexto").value("Contexto"))
                .andExpect(jsonPath("$.etapaAtual").value(1));
        assertNotNull(decisao.getDataAtualizacao());
        verify(decisoes, never()).save(any());
    }

    @Test
    void permiteContextoOpcionalEAvancaUmaUnicaEtapa() throws Exception {
        mvc.perform(put("/decisoes/12/contexto").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Projeto\",\"contexto\":\"  \"}"))
                .andExpect(status().isOk());
        assertNull(decisao.getContexto());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/decisoes/12/contexto/concluir").header("Authorization", TOKEN))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.etapaAtual").value(2))
                    .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        }
    }

    @Test
    void bloqueiaBrainstormingAntesDaContextualizacao() throws Exception {
        mvc.perform(get("/decisoes/12/ideias").header("Authorization", TOKEN)).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/ideias").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/brainstorming/concluir").header("Authorization", TOKEN))
                .andExpect(status().isConflict());
        verifyNoInteractions(ideias);
    }

    @Test
    void cadastraIdeiaNaDecisaoAutorizada() throws Exception {
        decisao.setEtapaAtual(2);
        mvc.perform(post("/decisoes/12/ideias").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(IDEIA))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.conteudo").value("Ganhar experiência"))
                .andExpect(jsonPath("$.categoria").value("FAVORAVEL"))
                .andExpect(jsonPath("$.decisao").doesNotExist());
        assertNotNull(decisao.getDataAtualizacao());
        assertEquals(2, decisao.getEtapaAtual());
    }

    @Test
    void consultaIdeiasEditaCategoriaEExclui() throws Exception {
        decisao.setEtapaAtual(2);
        var ideia = new IdeiaBrainstorming();
        ideia.setId(20L);
        ideia.setDecisao(decisao);
        ideia.setConteudo("Ganhar experiência");
        ideia.setCategoria("FAVORAVEL");
        when(ideias.findByIdAndDecisaoId(20L, 12L)).thenReturn(Optional.of(ideia));
        when(ideias.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(ideia));
        mvc.perform(get("/decisoes/12/ideias").header("Authorization", TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(20));
        mvc.perform(put("/decisoes/12/ideias/20").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\":\"  Será que consigo?  \",\"categoria\":\"DUVIDA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo").value("Será que consigo?"))
                .andExpect(jsonPath("$.categoria").value("DUVIDA"));
        mvc.perform(delete("/decisoes/12/ideias/20").header("Authorization", TOKEN)).andExpect(status().isNoContent());
        verify(ideias).delete(ideia);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"conteudo\":\" \u0020\",\"categoria\":\"DUVIDA\"}",
            "{\"conteudo\":\"Texto\"}", "{\"conteudo\":\"Texto\",\"categoria\":\"OUTRA\"}"})
    void rejeitaDadosInvalidosAoCriarEEditar(String corpo) throws Exception {
        decisao.setEtapaAtual(2);
        mvc.perform(post("/decisoes/12/ideias").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
        mvc.perform(put("/decisoes/12/ideias/20").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isBadRequest());
        verifyNoInteractions(ideias);
    }

    @Test
    void rejeitaIdeiaAcimaDoLimiteTecnico() throws Exception {
        mvc.perform(post("/decisoes/12/ideias").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\":\"" + "a".repeat(16001) + "\",\"categoria\":\"DUVIDA\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(ideias);
    }

    @Test
    void impedeAcessoADecisaoDeOutraPessoaEmTodasAsOperacoes() throws Exception {
        mvc.perform(get("/decisoes/99/ideias").header("Authorization", TOKEN)).andExpect(status().isNotFound());
        mvc.perform(post("/decisoes/99/ideias").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isNotFound());
        mvc.perform(put("/decisoes/99/ideias/20").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isNotFound());
        mvc.perform(delete("/decisoes/99/ideias/20").header("Authorization", TOKEN)).andExpect(status().isNotFound());
        mvc.perform(put("/decisoes/99/contexto").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Projeto\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/decisoes/99/contexto/concluir").header("Authorization", TOKEN)).andExpect(status().isNotFound());
        mvc.perform(post("/decisoes/99/brainstorming/concluir").header("Authorization", TOKEN)).andExpect(status().isNotFound());
        verifyNoInteractions(ideias);
    }

    @Test
    void naoEditaOuExcluiIdeiaDeOutraDecisao() throws Exception {
        decisao.setEtapaAtual(2);
        mvc.perform(put("/decisoes/12/ideias/99").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isNotFound());
        mvc.perform(delete("/decisoes/12/ideias/99").header("Authorization", TOKEN)).andExpect(status().isNotFound());
        verify(ideias, never()).save(any());
        verify(ideias, never()).delete(any());
    }

    @Test
    void concluiSemInventarQuantidadeMinimaENaoAvancaDuasVezes() throws Exception {
        decisao.setEtapaAtual(2);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/decisoes/12/brainstorming/concluir").header("Authorization", TOKEN))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.etapaAtual").value(3))
                    .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        }
        verifyNoInteractions(ideias);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6})
    void naoAlteraEtapasAnterioresSemRevisao(int etapa) throws Exception {
        decisao.setEtapaAtual(etapa);
        mvc.perform(put("/decisoes/12/contexto").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Projeto\"}")).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/contexto/concluir").header("Authorization", TOKEN)).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/ideias").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isConflict());
        mvc.perform(put("/decisoes/12/ideias/20").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isConflict());
        mvc.perform(delete("/decisoes/12/ideias/20").header("Authorization", TOKEN)).andExpect(status().isConflict());
        verifyNoInteractions(ideias);
    }

    @Test
    void decisaoConcluidaPermiteConsultaMasNaoAlteracoes() throws Exception {
        decisao.setEtapaAtual(2);
        decisao.setStatus(StatusDecisao.CONCLUIDA);
        mvc.perform(get("/decisoes/12/ideias").header("Authorization", TOKEN)).andExpect(status().isOk());
        mvc.perform(post("/decisoes/12/ideias").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isConflict());
        mvc.perform(put("/decisoes/12/ideias/20").header("Authorization", TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isConflict());
        mvc.perform(delete("/decisoes/12/ideias/20").header("Authorization", TOKEN)).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/brainstorming/concluir").header("Authorization", TOKEN)).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/contexto/concluir").header("Authorization", TOKEN)).andExpect(status().isConflict());
        verify(ideias, never()).save(any());
        verify(ideias, never()).delete(any());
    }

    @Test
    void exigeAutenticacaoEmTodasAsRotas() throws Exception {
        mvc.perform(get("/decisoes/12/ideias")).andExpect(status().isUnauthorized());
        mvc.perform(post("/decisoes/12/ideias").contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isUnauthorized());
        mvc.perform(put("/decisoes/12/ideias/20").contentType(MediaType.APPLICATION_JSON).content(IDEIA)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/decisoes/12/ideias/20")).andExpect(status().isUnauthorized());
        mvc.perform(put("/decisoes/12/contexto").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Projeto\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/decisoes/12/contexto/concluir")).andExpect(status().isUnauthorized());
        mvc.perform(post("/decisoes/12/brainstorming/concluir")).andExpect(status().isUnauthorized());
        verifyNoInteractions(decisoes, ideias);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "DELETE"})
    void permiteNovosMetodosSomenteNasOrigensLocais(String metodo) throws Exception {
        mvc.perform(options("/decisoes/12/ideias/20").header("Origin", "http://127.0.0.1:8765")
                        .header("Access-Control-Request-Method", metodo)
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Methods", containsString(metodo)));
        mvc.perform(options("/decisoes/12/ideias/20").header("Origin", "https://outro.example")
                .header("Access-Control-Request-Method", metodo)).andExpect(status().isForbidden());
    }
}
