package br.com.bussola.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.repository.AvaliacaoRepository;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.OpcaoRepository;
import br.com.bussola.repository.ResultadoRepository;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.AutenticacaoService;
import br.com.bussola.service.OpcoesService;
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

@WebMvcTest(OpcoesController.class)
@Import({SecurityConfig.class, OpcoesService.class, CadastroExceptionHandler.class})
class OpcoesControllerTest {
    private static final String ROTA = "/decisoes/12/opcoes";
    private static final String CORPO = """
            {"titulo":"Minha decisão", "opcaoA":{"nome":"Trabalhar"}, "opcaoB":{"nome":"Estudar","descricao":"Tempo para aprender"}}
            """;
    @Autowired private MockMvc mvc;
    @MockitoBean private AutenticacaoService autenticacao;
    @MockitoBean private DecisaoRepository decisoes;
    @MockitoBean private OpcaoRepository opcoes;
    @MockitoBean private AvaliacaoRepository avaliacoes;
    @MockitoBean private ResultadoRepository resultados;
    private Decisao decisao;

    @BeforeEach
    void preparar() {
        when(autenticacao.autenticar("token-teste")).thenReturn(new UsuarioResponse(7L,"Pessoa","pessoa@example.com"));
        decisao = new Decisao(); decisao.setId(12L); decisao.setTitulo("Minha decisão"); decisao.setEtapaAtual(3);
        when(decisoes.findByIdAndUsuarioId(12L,7L)).thenReturn(Optional.of(decisao));
        when(decisoes.buscarParaAtualizar(12L,7L)).thenReturn(Optional.of(decisao));
        when(opcoes.saveAll(anyList())).thenAnswer(inv -> {
            List<Opcao> par = inv.getArgument(0);
            par.get(0).setId(21L); par.get(1).setId(22L); return par;
        });
    }

    @Test
    void consultaExigeDonoERetornaContratoSemDadosPrivados() throws Exception {
        mvc.perform(get(ROTA).header("Authorization","Bearer token-teste"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control",containsString("no-store")))
                .andExpect(jsonPath("$.decisaoId").value(12)).andExpect(jsonPath("$.etapaAtual").value(3))
                .andExpect(jsonPath("$.usuario").doesNotExist());
        verify(decisoes).findByIdAndUsuarioId(12L,7L);
    }

    @Test
    void salvaParEConcluiComEstadoCorreto() throws Exception {
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk()).andExpect(jsonPath("$.opcaoA.id").value(21))
                .andExpect(jsonPath("$.opcaoB.nome").value("Estudar")).andExpect(jsonPath("$.etapaAtual").value(3));
        mvc.perform(post(ROTA+"/concluir").header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk()).andExpect(jsonPath("$.etapaAtual").value(4))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"titulo\":\"Título\"}",
        "{\"titulo\":\" \",\"opcaoA\":{\"nome\":\"A\"},\"opcaoB\":{\"nome\":\"B\"}}",
        "{\"titulo\":\"Título\",\"opcaoA\":{\"nome\":\" \"},\"opcaoB\":{\"nome\":\"B\"}}",
        "{\"titulo\":\"Título\",\"opcaoA\":{\"nome\":\"A\"},\"opcaoB\":null}"})
    void validaCamposObrigatorios(String corpo) throws Exception {
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void respeitaLimitesDoBancoAntesDeGravar() throws Exception {
        for (String corpo : List.of(CORPO.replace("Trabalhar", "x".repeat(201)),
                CORPO.replace("Minha decisão", "x".repeat(201)), CORPO.replace("Tempo para aprender", "x".repeat(16001)))) {
            mvc.perform(post(ROTA+"/concluir").header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void rejeitaAlternativasComMesmoNome() throws Exception {
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON)
                .content(CORPO.replace("Estudar","TRABALHAR")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value(containsString("nomes diferentes")));
    }

    @Test
    void rejeitaMudancaDeTituloNoPutENaConclusao() throws Exception {
        for (var requisicao : List.of(put(ROTA), post(ROTA + "/concluir"))) {
            mvc.perform(requisicao.header("Authorization", "Bearer token-teste")
                    .contentType(MediaType.APPLICATION_JSON).content(CORPO.replace("Minha decisão", "Outro nome")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erro").value(containsString("nome da decisão não pode ser alterado")));
        }
        verify(opcoes, never()).saveAll(anyList());
        verify(decisoes, never()).save(any());
    }

    @Test
    void exigeAutenticacaoEmTodasAsOperacoes() throws Exception {
        mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
        mvc.perform(put(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROTA+"/concluir").contentType(MediaType.APPLICATION_JSON).content(CORPO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(opcoes, decisoes);
    }

    @Test
    void retorna404ParaOutroDonoE409ParaEtapaFutura() throws Exception {
        when(decisoes.buscarParaAtualizar(12L,7L)).thenReturn(Optional.empty());
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isNotFound());
        decisao.setEtapaAtual(2);
        mvc.perform(get(ROTA).header("Authorization","Bearer token-teste"))
                .andExpect(status().isConflict());
        verify(opcoes, never()).saveAll(anyList());
    }

    @Test
    void permitePreflightPutDoFrontend() throws Exception {
        mvc.perform(options(ROTA).header("Origin","http://127.0.0.1:8765")
                .header("Access-Control-Request-Method","PUT").header("Access-Control-Request-Headers","authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:8765"))
                .andExpect(header().string("Access-Control-Allow-Methods",containsString("PUT")));
    }
}
