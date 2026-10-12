package br.com.bussola.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.bussola.dto.response.*;
import br.com.bussola.exception.*;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.*;
import java.util.List;
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

@WebMvcTest(CriteriosController.class)
@Import({SecurityConfig.class, CadastroExceptionHandler.class})
class CriteriosControllerTest {
    private static final String ROTA = "/decisoes/12/criterios";
    @Autowired private MockMvc mvc;
    @MockitoBean private AutenticacaoService autenticacao;
    @MockitoBean private CriteriosService criterios;

    private String corpo(String nota) {
        return "{\"criterios\":[" + String.join(",",CatalogoCriterios.PADROES.stream()
                .map(p -> "{\"nome\":\"" + p.nome() + "\",\"notaA\":" + nota + ",\"notaB\":5}").toList()) + "]}";
    }
    @BeforeEach
    void preparar() {
        when(autenticacao.autenticar("token-teste")).thenReturn(new UsuarioResponse(7L,"Pessoa","pessoa@example.com"));
        var resposta = new CriteriosResponse(12L,"Minha decisão",4,StatusDecisao.EM_ANDAMENTO,
                new OpcoesResponse.DadosOpcao(21L,"Trabalhar",null), new OpcoesResponse.DadosOpcao(22L,"Estudar",null),
                List.of(new CriteriosResponse.Item(30L,"risco","Risco","Incerteza",false,"MENOR_MELHOR",1,5,null)));
        when(criterios.consultar(7L,12L)).thenReturn(resposta);
        when(criterios.salvar(eq(7L),eq(12L),any())).thenReturn(resposta);
        when(criterios.concluir(eq(7L),eq(12L),any())).thenReturn(resposta);
    }
    @Test
    void consultaContratoComDirecaoDoRiscoSemCache() throws Exception {
        mvc.perform(get(ROTA).header("Authorization","Bearer token-teste"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control",containsString("no-store")))
                .andExpect(jsonPath("$.criterios[0].sentido").value("MENOR_MELHOR"))
                .andExpect(jsonPath("$.criterios[0].notaA").value(5)).andExpect(jsonPath("$.usuario").doesNotExist());
        verify(criterios).consultar(7L,12L);
    }
    @Test
    void permiteParcialEEnviaConclusaoAoServicoComUsuarioDoToken() throws Exception {
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo("null")))
                .andExpect(status().isOk());
        mvc.perform(post(ROTA+"/concluir").header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo("3")))
                .andExpect(status().isOk());
        verify(criterios).salvar(eq(7L),eq(12L),argThat(r -> r.criterios().get(0).notaA() == null));
        verify(criterios).concluir(eq(7L),eq(12L),any());
    }
    @ParameterizedTest
    @ValueSource(strings={"0","6","-1","2.5","\"texto\""})
    void rejeitaNotasInvalidas(String nota) throws Exception {
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo(nota)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(criterios);
    }
    @ParameterizedTest
    @ValueSource(strings={"{}","{\"criterios\":null}","{\"criterios\":[]}","{\"criterios\":[null,null,null,null,null]}"})
    void rejeitaListasInvalidas(String corpo) throws Exception {
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(criterios);
    }
    @Test
    void validaNomeEDescricaoAntesDeChamarServico() throws Exception {
        for (var invalido : List.of(corpo("1").replace("Segurança"," "), corpo("1").replace("Segurança","x".repeat(201)),
                corpo("1").replace("\"notaA\":1", "\"descricao\":\""+"x".repeat(1001)+"\",\"notaA\":1"))) {
            mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(invalido))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(criterios);
    }
    @Test
    void exigeLoginNasTresOperacoes() throws Exception {
        mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
        mvc.perform(put(ROTA).contentType(MediaType.APPLICATION_JSON).content(corpo("3"))).andExpect(status().isUnauthorized());
        mvc.perform(post(ROTA+"/concluir").contentType(MediaType.APPLICATION_JSON).content(corpo("3"))).andExpect(status().isUnauthorized());
        verifyNoInteractions(criterios);
    }
    @Test
    void traduzErrosDeTitularidadeEtapaERegra() throws Exception {
        when(criterios.consultar(7L,12L)).thenThrow(new DecisaoNaoEncontradaException());
        mvc.perform(get(ROTA).header("Authorization","Bearer token-teste")).andExpect(status().isNotFound());
        when(criterios.salvar(eq(7L),eq(12L),any())).thenThrow(new EtapaInvalidaException("Etapa incorreta"));
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo("3"))).andExpect(status().isConflict());
        when(criterios.concluir(eq(7L),eq(12L),any())).thenThrow(new CriteriosInvalidosException("Avalie todos os critérios"));
        mvc.perform(post(ROTA+"/concluir").header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo("null")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value("Avalie todos os critérios"));
    }
    @Test
    void permiteCorsDoFrontend() throws Exception {
        mvc.perform(options(ROTA).header("Origin","http://localhost:8765").header("Access-Control-Request-Method","PUT")
                .header("Access-Control-Request-Headers","authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:8765"));
    }
}
