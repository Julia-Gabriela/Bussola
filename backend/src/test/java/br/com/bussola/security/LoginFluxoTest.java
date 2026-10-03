package br.com.bussola.security;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.bussola.config.OpenApiConfig;
import br.com.bussola.config.PasswordConfig;
import br.com.bussola.config.RelogioConfig;
import br.com.bussola.controller.AutenticacaoController;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.SessaoAutenticacao;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.SessaoAutenticacaoRepository;
import br.com.bussola.repository.UsuarioRepository;
import br.com.bussola.service.AutenticacaoService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = LoginFluxoTest.AplicacaoTeste.class,
        properties = "bussola.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
class LoginFluxoTest {
    @Configuration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({AutenticacaoController.class, AutenticacaoService.class, JwtTokenProvider.class,
            SecurityConfig.class, PasswordConfig.class, RelogioConfig.class,
            CadastroExceptionHandler.class, OpenApiConfig.class})
    static class AplicacaoTeste {
    }

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private UsuarioRepository usuarios;
    @MockitoBean private SessaoAutenticacaoRepository sessoes;
    private final Map<String, SessaoAutenticacao> registros = new HashMap<>();

    @BeforeEach
    void preparar() {
        registros.clear();
        var usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNomeCompleto("Usuario Teste");
        usuario.setEmail("teste@example.com");
        usuario.setSenhaHash(encoder.encode("SenhaDeTeste123!"));
        when(usuarios.findByEmailIgnoreCase("teste@example.com")).thenReturn(Optional.of(usuario));
        when(sessoes.save(any())).thenAnswer(inv -> {
            SessaoAutenticacao sessao = inv.getArgument(0);
            registros.put(sessao.getId(), sessao);
            return sessao;
        });
        when(sessoes.buscarParaAutenticar(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(registros.get(inv.getArgument(0))));
    }

    private String entrar() throws Exception {
        var resultado = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"TESTE@example.com","senha":"SenhaDeTeste123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.inatividadeMaximaSegundos").value(3600))
                .andExpect(jsonPath("$.usuario.id").value(1))
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
                .andReturn();
        return JsonMapper.builder().build().readTree(resultado.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void loginPermiteConsultarUsuarioComToken() throws Exception {
        String token = entrar();
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("teste@example.com"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void senhaIncorretaEEmailInexistenteRetornamMesmoErro() throws Exception {
        for (String email : new String[]{"teste@example.com", "ausente@example.com"}) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"senha\":\"incorreta\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."));
        }
        verify(sessoes, never()).save(any());
    }

    @Test
    void exigeCredenciaisPreenchidas() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaJsonMalformado() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exigeTokenParaConsultarUsuario() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaTokenComAssinaturaAlterada() throws Exception {
        String token = entrar();
        int inicio = token.lastIndexOf('.') + 1;
        String adulterado = token.substring(0, inicio)
                + (token.charAt(inicio) == 'A' ? "B" : "A") + token.substring(inicio + 1);
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaSessaoExpiradaSemRenovarAtividade() throws Exception {
        String token = entrar();
        var sessao = registros.values().iterator().next();
        var anterior = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(61);
        sessao.setUltimaAtividade(anterior);
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertEquals(anterior, sessao.getUltimaAtividade());
    }

    @Test
    void documentaLoginEAutorizacaoBearer() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/auth/login'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void permitePreflightComAuthorizationNaOrigemDoFrontend() throws Exception {
        mvc.perform(options("/auth/me").header("Origin", "http://localhost:8765")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8765"));
    }

    @Test
    void loginPublicoIgnoraTokenAntigo() throws Exception {
        mvc.perform(post("/auth/login").header("Authorization", "Bearer antigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teste@example.com\",\"senha\":\"SenhaDeTeste123!\"}"))
                .andExpect(status().isOk());
    }
}
