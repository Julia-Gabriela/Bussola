package br.com.bussola.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.*;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.*;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.AutenticacaoService;
import br.com.bussola.service.TecnicasAnaliseService;
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

@WebMvcTest(TecnicasAnaliseController.class)
@Import({SecurityConfig.class, TecnicasAnaliseService.class, CadastroExceptionHandler.class})
class TecnicasAnaliseControllerTest {
    private static final String TOKEN = "Bearer teste";
    private static final String ARGUMENTO = "{\"tipo\":\"PRO\",\"descricao\":\"  Aprender  \"}";
    private static final String GUT = "{\"descricao\":\"  Prazo  \",\"gravidade\":5,\"urgencia\":4,\"tendencia\":3,\"prioridade\":999}";
    @Autowired private MockMvc mvc;
    @MockitoBean private AutenticacaoService autenticacao;
    @MockitoBean private DecisaoRepository decisoes;
    @MockitoBean private OpcaoRepository opcoes;
    @MockitoBean private ProContraRepository argumentos;
    @MockitoBean private ItemGutRepository fatores;
    @MockitoBean private ResultadoRepository resultados;
    private Decisao decisao;
    private Opcao a;
    private Opcao b;

    @BeforeEach
    void preparar() {
        when(autenticacao.autenticar("teste")).thenReturn(new UsuarioResponse(7L,"Teste","teste@example.com"));
        decisao = new Decisao(); decisao.setId(12L); decisao.setTitulo("Escolha"); decisao.setEtapaAtual(6);
        a = new Opcao(); a.setId(21L); a.setNome("A"); a.setDecisao(decisao);
        b = new Opcao(); b.setId(22L); b.setNome("B"); b.setDecisao(decisao);
        when(decisoes.findByIdAndUsuarioId(12L,7L)).thenReturn(Optional.of(decisao));
        when(decisoes.buscarParaAtualizar(12L,7L)).thenReturn(Optional.of(decisao));
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a,b));
        when(argumentos.save(any())).thenAnswer(inv -> { ProContra p=inv.getArgument(0); p.setId(30L); return p; });
        when(fatores.save(any())).thenAnswer(inv -> { ItemGut p=inv.getArgument(0); p.setId(40L); return p; });
    }

    @ParameterizedTest @ValueSource(longs={21,22})
    void cadastraPorAlternativaSemMudarProgresso(long opcao) throws Exception {
        mvc.perform(post("/decisoes/12/opcoes/"+opcao+"/pros-contras").header("Authorization",TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(ARGUMENTO))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.descricao").value("Aprender"));
        verify(argumentos).save(argThat(p -> p.getOpcao().getId()==opcao));
        assertEquals(6,decisao.getEtapaAtual()); assertNotNull(decisao.getDataAtualizacao());
    }

    @Test void calculaPrioridadeNoServidorEMarcaResultados() throws Exception {
        Resultado resultado=new Resultado(); resultado.setPontuacaoTotal(20);
        when(resultados.findByOpcaoId(21L)).thenReturn(Optional.of(resultado));
        mvc.perform(post("/decisoes/12/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(GUT))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.prioridade").value(60)).andExpect(jsonPath("$.descricao").value("Prazo"));
        assertTrue(resultado.isRequerAtualizacao()); assertEquals(20,resultado.getPontuacaoTotal());
        verify(resultados).findByOpcaoId(22L);
    }

    @ParameterizedTest @ValueSource(strings={"0","6","1.5","null"})
    void rejeitaNotaInvalida(String nota) throws Exception {
        for(String campo:List.of("gravidade","urgencia","tendencia")) {
            String corpo=GUT.replaceAll("\\\""+campo+"\\\":\\d", "\""+campo+"\":"+nota);
            mvc.perform(post("/decisoes/12/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(fatores);
    }

    @ParameterizedTest @ValueSource(strings={"{\"tipo\":\"OUTRO\",\"descricao\":\"a\"}","{\"tipo\":\"PRO\",\"descricao\":\" \"}","{}"})
    void rejeitaArgumentoInvalido(String corpo) throws Exception {
        mvc.perform(post("/decisoes/12/opcoes/21/pros-contras").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isBadRequest()); verifyNoInteractions(argumentos);
    }

    @Test void ordenaPorPrioridadeEDesempataPeloId() throws Exception {
        ItemGut x=fator(3,1),y=fator(2,5),z=fator(1,5);
        when(fatores.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(x,y,z));
        mvc.perform(get("/decisoes/12/analise").header("Authorization",TOKEN)).andExpect(status().isOk())
            .andExpect(jsonPath("$.fatoresGut[0].id").value(1)).andExpect(jsonPath("$.fatoresGut[0].prioridade").value(125))
            .andExpect(jsonPath("$.fatoresGut[1].id").value(2)).andExpect(jsonPath("$.fatoresGut[2].prioridade").value(1));
    }

    @Test void editaEExcluiRegistrosDoEscopoCorreto() throws Exception {
        ItemGut f=fator(40,1); when(fatores.findByIdAndDecisaoId(40L,12L)).thenReturn(Optional.of(f));
        ProContra p=new ProContra(); p.setId(30L); p.setOpcao(a);
        when(argumentos.findByIdAndOpcaoId(30L,21L)).thenReturn(Optional.of(p));
        mvc.perform(put("/decisoes/12/gut/40").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(GUT))
            .andExpect(status().isOk()).andExpect(jsonPath("$.prioridade").value(60));
        mvc.perform(put("/decisoes/12/opcoes/21/pros-contras/30").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(ARGUMENTO.replace("PRO","CONTRA")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("CONTRA"));
        mvc.perform(delete("/decisoes/12/gut/40").header("Authorization",TOKEN)).andExpect(status().isNoContent());
        mvc.perform(delete("/decisoes/12/opcoes/21/pros-contras/30").header("Authorization",TOKEN)).andExpect(status().isNoContent());
        verify(fatores).delete(f); verify(argumentos).delete(p);
    }

    @Test void naoAcessaOutraDecisaoOuAlternativa() throws Exception {
        mvc.perform(get("/decisoes/99/analise").header("Authorization",TOKEN)).andExpect(status().isNotFound());
        mvc.perform(post("/decisoes/99/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(GUT)).andExpect(status().isNotFound());
        mvc.perform(post("/decisoes/12/opcoes/99/pros-contras").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(ARGUMENTO)).andExpect(status().isNotFound());
        mvc.perform(delete("/decisoes/12/gut/99").header("Authorization",TOKEN)).andExpect(status().isNotFound());
        mvc.perform(delete("/decisoes/12/opcoes/22/pros-contras/30").header("Authorization",TOKEN)).andExpect(status().isNotFound());
        verify(fatores,never()).save(any()); verify(argumentos,never()).save(any());
    }

    @ParameterizedTest @ValueSource(ints={1,2,3,4,5})
    void naoPulaEtapas(int etapa) throws Exception {
        decisao.setEtapaAtual(etapa);
        mvc.perform(get("/decisoes/12/analise").header("Authorization",TOKEN)).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(GUT)).andExpect(status().isConflict());
        verifyNoInteractions(fatores,argumentos);
    }

    @Test void concluidaPermiteConsultaMasNaoAlteracao() throws Exception {
        decisao.setStatus(StatusDecisao.CONCLUIDA);
        mvc.perform(get("/decisoes/12/analise").header("Authorization",TOKEN)).andExpect(status().isOk());
        mvc.perform(post("/decisoes/12/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(GUT)).andExpect(status().isConflict());
        mvc.perform(delete("/decisoes/12/opcoes/21/pros-contras/30").header("Authorization",TOKEN)).andExpect(status().isConflict());
    }

    @Test void exigeAutenticacao() throws Exception {
        mvc.perform(get("/decisoes/12/analise")).andExpect(status().isUnauthorized());
        mvc.perform(post("/decisoes/12/gut").contentType(MediaType.APPLICATION_JSON).content(GUT)).andExpect(status().isUnauthorized());
        mvc.perform(put("/decisoes/12/gut/40").contentType(MediaType.APPLICATION_JSON).content(GUT)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/decisoes/12/gut/40")).andExpect(status().isUnauthorized());
        verifyNoInteractions(decisoes);
    }

    @Test void exigeDuasAlternativas() throws Exception {
        when(opcoes.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(a));
        mvc.perform(get("/decisoes/12/analise").header("Authorization",TOKEN)).andExpect(status().isConflict());
        mvc.perform(post("/decisoes/12/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(GUT))
            .andExpect(status().isConflict());
        verifyNoInteractions(fatores);
    }

    @Test void validaLimitesDosTextos() throws Exception {
        mvc.perform(post("/decisoes/12/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON)
            .content(GUT.replace("  Prazo  ","x".repeat(201)))).andExpect(status().isBadRequest());
        mvc.perform(post("/decisoes/12/gut").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON)
            .content(GUT.replace("  Prazo  "," "))).andExpect(status().isBadRequest());
        mvc.perform(post("/decisoes/12/opcoes/21/pros-contras").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON)
            .content(ARGUMENTO.replace("  Aprender  ","x".repeat(301)))).andExpect(status().isBadRequest());
        verifyNoInteractions(argumentos,fatores);
    }

    @Test void permitePreflightDasEdicoes() throws Exception {
        mvc.perform(options("/decisoes/12/gut/40").header("Origin","http://127.0.0.1:8765")
            .header("Access-Control-Request-Method","PUT").header("Access-Control-Request-Headers","authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:8765"));
    }

    private static final String NOTAS = "{\"gravidade\":5,\"urgencia\":4,\"tendencia\":2}";

    @Test void classificaProContraUsandoADescricaoDeleSemDuplicar() throws Exception {
        ProContra p=argumento(30L,a,"Aprender na prática");
        when(argumentos.findByIdAndOpcaoId(30L,21L)).thenReturn(Optional.of(p));
        mvc.perform(put("/decisoes/12/gut/pros-contras/30").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(NOTAS))
            .andExpect(status().isOk()).andExpect(jsonPath("$.proContraId").value(30))
            .andExpect(jsonPath("$.descricao").value("Aprender na prática")).andExpect(jsonPath("$.prioridade").value(40));
        ItemGut existente=fator(40,1); existente.setProContra(p);
        when(fatores.findByProContraId(30L)).thenReturn(Optional.of(existente));
        mvc.perform(put("/decisoes/12/gut/pros-contras/30").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(NOTAS))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(40)).andExpect(jsonPath("$.prioridade").value(40));
        verify(fatores,times(2)).save(argThat(item -> item.getProContra()==p && item.getDecisao()==decisao));
        assertEquals(5,existente.getGravidade()); assertEquals("Aprender na prática",existente.getDescricao());
    }

    @Test void naoClassificaProContraDeOutraDecisao() throws Exception {
        mvc.perform(put("/decisoes/12/gut/pros-contras/99").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(NOTAS))
            .andExpect(status().isNotFound());
        mvc.perform(put("/decisoes/99/gut/pros-contras/30").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(NOTAS))
            .andExpect(status().isNotFound());
        verify(fatores,never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings={"0","6","1.5","null"})
    void rejeitaNotaInvalidaNaClassificacao(String nota) throws Exception {
        mvc.perform(put("/decisoes/12/gut/pros-contras/30").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON)
            .content(NOTAS.replace("\"urgencia\":4","\"urgencia\":"+nota))).andExpect(status().isBadRequest());
        verifyNoInteractions(fatores);
    }

    @Test void editarProContraAtualizaADescricaoDaClassificacao() throws Exception {
        ProContra p=argumento(30L,a,"Antigo");
        ItemGut vinculado=fator(40,3); vinculado.setProContra(p); vinculado.setDescricao("Antigo");
        when(argumentos.findByIdAndOpcaoId(30L,21L)).thenReturn(Optional.of(p));
        when(fatores.findByProContraId(30L)).thenReturn(Optional.of(vinculado));
        mvc.perform(put("/decisoes/12/opcoes/21/pros-contras/30").header("Authorization",TOKEN).contentType(MediaType.APPLICATION_JSON).content(ARGUMENTO))
            .andExpect(status().isOk());
        assertEquals("Aprender",vinculado.getDescricao()); assertEquals(3,vinculado.getGravidade());
    }

    @Test void excluirProContraExcluiASuaClassificacao() throws Exception {
        ProContra p=argumento(30L,a,"Item");
        ItemGut vinculado=fator(40,3); vinculado.setProContra(p);
        when(argumentos.findByIdAndOpcaoId(30L,21L)).thenReturn(Optional.of(p));
        when(fatores.findByProContraId(30L)).thenReturn(Optional.of(vinculado));
        mvc.perform(delete("/decisoes/12/opcoes/21/pros-contras/30").header("Authorization",TOKEN)).andExpect(status().isNoContent());
        var ordem=inOrder(fatores,argumentos);
        ordem.verify(fatores).delete(vinculado); ordem.verify(argumentos).delete(p);
    }

    @Test void consultaMostraADescricaoAtualDoProContraVinculado() throws Exception {
        ProContra p=argumento(30L,b,"Descrição editada");
        ItemGut vinculado=fator(40,2); vinculado.setProContra(p); vinculado.setDescricao("Descrição antiga");
        ItemGut avulso=fator(41,1);
        when(fatores.findByDecisaoIdOrderByIdAsc(12L)).thenReturn(List.of(vinculado,avulso));
        mvc.perform(get("/decisoes/12/analise").header("Authorization",TOKEN)).andExpect(status().isOk())
            .andExpect(jsonPath("$.fatoresGut[0].proContraId").value(30)).andExpect(jsonPath("$.fatoresGut[0].descricao").value("Descrição editada"))
            .andExpect(jsonPath("$.fatoresGut[1].proContraId").value(org.hamcrest.Matchers.nullValue())).andExpect(jsonPath("$.fatoresGut[1].descricao").value("Fator"));
    }

    private ProContra argumento(long id,Opcao opcao,String descricao) {
        ProContra p=new ProContra(); p.setId(id); p.setOpcao(opcao); p.setTipo(br.com.bussola.model.enums.TipoProContra.PRO); p.setDescricao(descricao); return p;
    }

    private ItemGut fator(long id,int nota) {
        ItemGut item=new ItemGut(); item.setId(id); item.setDecisao(decisao); item.setDescricao("Fator");
        item.setGravidade(nota); item.setUrgencia(nota); item.setTendencia(nota); return item;
    }
}
