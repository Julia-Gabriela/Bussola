package br.com.bussola.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.*;
import br.com.bussola.security.SecurityConfig;
import br.com.bussola.service.*;
import br.com.bussola.config.PasswordConfig;
import br.com.bussola.config.RelogioConfig;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PerfilController.class)
@Import({SecurityConfig.class,PerfilService.class,CadastroExceptionHandler.class,PasswordConfig.class,RelogioConfig.class})
class PerfilControllerTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean AutenticacaoService autenticacao;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean SessaoAutenticacaoRepository sessoes;
    @MockitoBean TokenRecuperacaoSenhaRepository recuperacoes;
    Usuario usuario;
    static final String DADOS="""
        {"nomeCompleto":"Pessoa Teste","dataNascimento":"2000-01-01","email":"pessoa@example.invalid"}
        """;
    @BeforeEach void preparar() {
        usuario=new Usuario();usuario.setId(7L);usuario.setNomeCompleto("Pessoa Teste");
        usuario.setDataNascimento(LocalDate.of(2000,1,1));usuario.setEmail("pessoa@example.invalid");
        usuario.setSenhaHash(encoder.encode("senha-atual"));
        when(autenticacao.autenticar("teste")).thenReturn(new UsuarioResponse(7L,"Pessoa Teste","pessoa@example.invalid"));
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarios.buscarParaAtualizar(7L)).thenReturn(Optional.of(usuario));
    }
    @Test void consultaNaoExpoeHashNemRelacionamentos() throws Exception {
        mvc.perform(get("/perfil").header("Authorization","Bearer teste"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control",containsString("no-store")))
            .andExpect(jsonPath("$.dataNascimento").value("2000-01-01"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist()).andExpect(jsonPath("$.decisoes").doesNotExist());
    }
    @Test void dadosUsamIdDoToken() throws Exception {
        mvc.perform(put("/perfil").header("Authorization","Bearer teste").contentType(MediaType.APPLICATION_JSON).content(DADOS))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(usuarios).buscarParaAtualizar(7L);
    }
    @Test void validaDadosAntesDeEscrever() throws Exception {
        for(var corpo:java.util.List.of("{}",DADOS.replace("pessoa@example.invalid","invalido"),DADOS.replace("Pessoa Teste"," "),
                DADOS.replace("2000-01-01","2099-01-01"),DADOS.replace("Pessoa Teste","a".repeat(151)))) {
            mvc.perform(put("/perfil").header("Authorization","Bearer teste").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
        }
        verify(usuarios,never()).saveAndFlush(any());
    }
    @Test void senhaAtualIncorretaNaoExpiraSessao() throws Exception {
        mvc.perform(put("/perfil/senha").header("Authorization","Bearer teste").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"senhaAtual":"errada","novaSenha":"nova-senha","confirmacaoSenha":"nova-senha"}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value("A senha atual está incorreta."));
        verifyNoInteractions(sessoes);
    }
    @Test void alteraSenhaERetornaSemConteudo() throws Exception {
        mvc.perform(put("/perfil/senha").header("Authorization","Bearer teste").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"senhaAtual":"senha-atual","novaSenha":"nova-senha","confirmacaoSenha":"nova-senha"}
                """)).andExpect(status().isNoContent());
        verify(sessoes).revogarTodasDoUsuario(7L);
    }
    @Test void exclusaoPrecisaConfirmacaoESenha() throws Exception {
        mvc.perform(delete("/perfil").header("Authorization","Bearer teste").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"senhaAtual":"senha-atual","confirmar":false}
                """)).andExpect(status().isBadRequest());
        verify(usuarios,never()).delete(any());
        mvc.perform(delete("/perfil").header("Authorization","Bearer teste").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"senhaAtual":"senha-atual","confirmar":true}
                """)).andExpect(status().isNoContent());
        verify(usuarios).delete(usuario);
    }
    @Test void protegeTodasAsOperacoesSemToken() throws Exception {
        mvc.perform(get("/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(put("/perfil").contentType(MediaType.APPLICATION_JSON).content(DADOS)).andExpect(status().isUnauthorized());
        mvc.perform(put("/perfil/senha").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/perfil").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(usuarios,sessoes);
    }
    @Test void corsPermitePutEDeleteSomenteDoFrontendLocal() throws Exception {
        for(var metodo:java.util.List.of("PUT","DELETE"))
            mvc.perform(options("/perfil").header("Origin","http://127.0.0.1:8765").header("Access-Control-Request-Method",metodo)
                .header("Access-Control-Request-Headers","authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:8765"));
        mvc.perform(options("/perfil").header("Origin","https://example.invalid").header("Access-Control-Request-Method","DELETE"))
            .andExpect(status().isForbidden());
    }
}
