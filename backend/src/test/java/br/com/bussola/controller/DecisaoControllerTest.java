package br.com.bussola.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.UsuarioRepository;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.AutenticacaoService;
import br.com.bussola.service.DecisaoService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DecisaoController.class)
@Import({SecurityConfig.class, DecisaoService.class, CadastroExceptionHandler.class})
class DecisaoControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AutenticacaoService autenticacao;
    @MockitoBean private DecisaoRepository decisoes;
    @MockitoBean private UsuarioRepository usuarios;

    @BeforeEach
    void preparar() {
        when(autenticacao.autenticar("token-teste"))
                .thenReturn(new UsuarioResponse(7L, "Usuario", "usuario@example.com"));
        var usuario = new Usuario();
        usuario.setId(7L);
        when(usuarios.getReferenceById(7L)).thenReturn(usuario);
        when(decisoes.save(any())).thenAnswer(inv -> {
            Decisao decisao = inv.getArgument(0);
            decisao.setId(12L);
            decisao.setDataCriacao(LocalDateTime.of(2026, 10, 8, 12, 0));
            decisao.setDataAtualizacao(decisao.getDataCriacao());
            return decisao;
        });
    }

    @Test
    void criaDecisaoDoUsuarioAutenticadoComEstadoInicial() throws Exception {
        mvc.perform(post("/decisoes").header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"  Mudar de emprego?  ","contexto":"  Recebi uma proposta.  ",
                                 "usuarioId":99,"status":"CONCLUIDA","etapaAtual":6}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/decisoes/12"))
                .andExpect(jsonPath("$.titulo").value("Mudar de emprego?"))
                .andExpect(jsonPath("$.contexto").value("Recebi uma proposta."))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.etapaAtual").value(1))
                .andExpect(jsonPath("$.dataCriacao").exists())
                .andExpect(jsonPath("$.usuario").doesNotExist());
        var captor = ArgumentCaptor.forClass(Decisao.class);
        verify(decisoes).save(captor.capture());
        assertEquals(7L, captor.getValue().getUsuario().getId());
    }

    @Test
    void aceitaCriacaoSemContexto() throws Exception {
        mvc.perform(post("/decisoes").header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Meu projeto\"}"))
                .andExpect(status().isCreated());
        var captor = ArgumentCaptor.forClass(Decisao.class);
        verify(decisoes).save(captor.capture());
        assertNull(captor.getValue().getContexto());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"titulo\":\"\"}", "{\"titulo\":\"   \"}"})
    void rejeitaTituloAusenteOuVazio(String corpo) throws Exception {
        mvc.perform(post("/decisoes").header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        verify(decisoes, never()).save(any());
    }

    @Test
    void rejeitaTituloMaiorQueLimiteDoBanco() throws Exception {
        mvc.perform(post("/decisoes").header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest());
        verify(decisoes, never()).save(any());
    }

    @Test
    void rejeitaContextoMaiorQueLimite() throws Exception {
        mvc.perform(post("/decisoes").header("Authorization", "Bearer token-teste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Projeto\",\"contexto\":\"" + "a".repeat(16001) + "\"}"))
                .andExpect(status().isBadRequest());
        verify(decisoes, never()).save(any());
    }

    @Test
    void exigeAutenticacaoParaCriarEConsultar() throws Exception {
        mvc.perform(post("/decisoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Projeto\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/decisoes/12")).andExpect(status().isUnauthorized());
        verify(decisoes, never()).save(any());
    }

    @Test
    void consultaSomenteDecisaoDoProprioUsuario() throws Exception {
        var decisao = new Decisao();
        decisao.setId(12L);
        decisao.setTitulo("Projeto");
        when(decisoes.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.of(decisao));
        mvc.perform(get("/decisoes/12").header("Authorization", "Bearer token-teste"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(12));
        verify(decisoes).findByIdAndUsuarioId(12L, 7L);
    }

    @Test
    void ocultaDecisaoDeOutroUsuarioOuInexistente() throws Exception {
        when(decisoes.findByIdAndUsuarioId(12L, 7L)).thenReturn(Optional.empty());
        mvc.perform(get("/decisoes/12").header("Authorization", "Bearer token-teste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Decisão não encontrada."));
        verify(decisoes, never()).findById(any());
    }

    @Test
    void permiteFrontendEnviarToken() throws Exception {
        mvc.perform(options("/decisoes").header("Origin", "http://127.0.0.1:8765")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }
}
