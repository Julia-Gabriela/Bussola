package br.com.bussola.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.*;
import br.com.bussola.repository.*;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.AutenticacaoService;
import br.com.bussola.service.Reflexoes101010Service;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(Reflexoes101010Controller.class)
@Import({SecurityConfig.class, Reflexoes101010Service.class, CadastroExceptionHandler.class})
class Reflexoes101010ControllerTest {
    private static final String ROTA="/decisoes/12/reflexoes-101010";
    private static final String CORPO="""
        {"opcaoA":{"opcaoId":21,"curtoPrazo":"Alívio","medioPrazo":"Aprendizado","longoPrazo":"Experiência"},
         "opcaoB":{"opcaoId":22,"curtoPrazo":"Calma","medioPrazo":"Estabilidade","longoPrazo":"Equilíbrio"}}
        """;
    @Autowired MockMvc mvc;
    @MockitoBean AutenticacaoService autenticacao;
    @MockitoBean DecisaoRepository decisoes;
    @MockitoBean OpcaoRepository opcoes;
    @MockitoBean Reflexao101010Repository reflexoes;
    @MockitoBean ResultadoRepository resultados;
    Decisao decisao;

    @BeforeEach void preparar() {
        when(autenticacao.autenticar("token-teste")).thenReturn(new UsuarioResponse(7L,"Pessoa","pessoa@example.invalid"));
        decisao=new Decisao();decisao.setId(12L);decisao.setTitulo("Minha decisão");decisao.setEtapaAtual(6);
        when(decisoes.findByIdAndUsuarioId(12L,7L)).thenReturn(Optional.of(decisao));
        when(decisoes.buscarParaAtualizar(12L,7L)).thenReturn(Optional.of(decisao));
        var a=new Opcao();a.setId(21L);a.setNome("Trabalhar");a.setDecisao(decisao);
        var b=new Opcao();b.setId(22L);b.setNome("Estudar");b.setDecisao(decisao);
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a,b));
        when(reflexoes.saveAll(anyList())).thenAnswer(inv->{List<Reflexao101010> lista=inv.getArgument(0);for(int i=0;i<lista.size();i++)lista.get(i).setId(100L+i);return lista;});
    }
    @Test void getNaoGravaERetornaCamposVaziosSemExporUsuario() throws Exception {
        mvc.perform(get(ROTA).header("Authorization","Bearer token-teste"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control",containsString("no-store")))
            .andExpect(jsonPath("$.opcaoA.id").value(21)).andExpect(jsonPath("$.opcaoB.curtoPrazo").isEmpty())
            .andExpect(jsonPath("$.concluida").value(false)).andExpect(jsonPath("$.usuario").doesNotExist());
        verify(reflexoes,never()).saveAll(anyList());
    }
    @Test void putParcialEConclusaoComSeisReflexoes() throws Exception {
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON)
            .content("{\"opcaoA\":{\"opcaoId\":21},\"opcaoB\":{\"opcaoId\":22}}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.concluida").value(false));
        mvc.perform(post(ROTA+"/concluir").header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(CORPO))
            .andExpect(status().isOk()).andExpect(jsonPath("$.concluida").value(true))
            .andExpect(jsonPath("$.etapaAtual").value(6)).andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }
    @Test void rejeitaConclusaoIncompletaELimites() throws Exception {
        for(var corpo:List.of("{}",CORPO.replace("Alívio"," "),CORPO.replace("Alívio","x".repeat(16001)),CORPO.replace("21","99"))) {
            mvc.perform(post(ROTA+"/concluir").header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        }
        verify(reflexoes,never()).saveAll(anyList());
    }
    @Test void exigeAutenticacaoEmTodasAsRotas() throws Exception {
        mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
        mvc.perform(put(ROTA).contentType(MediaType.APPLICATION_JSON).content(CORPO)).andExpect(status().isUnauthorized());
        mvc.perform(post(ROTA+"/concluir").contentType(MediaType.APPLICATION_JSON).content(CORPO)).andExpect(status().isUnauthorized());
        verifyNoInteractions(reflexoes,decisoes);
    }
    @Test void rejeitaOutraContaEEtapaAnterior() throws Exception {
        when(decisoes.findByIdAndUsuarioId(12L,7L)).thenReturn(Optional.empty());
        mvc.perform(get(ROTA).header("Authorization","Bearer token-teste")).andExpect(status().isNotFound());
        decisao.setEtapaAtual(5);
        mvc.perform(put(ROTA).header("Authorization","Bearer token-teste").contentType(MediaType.APPLICATION_JSON).content(CORPO)).andExpect(status().isConflict());
        verify(reflexoes,never()).saveAll(anyList());
    }
    @Test void permitePreflightDoFrontend() throws Exception {
        mvc.perform(options(ROTA).header("Origin","http://127.0.0.1:8765")
            .header("Access-Control-Request-Method","PUT").header("Access-Control-Request-Headers","authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:8765"));
    }
}
