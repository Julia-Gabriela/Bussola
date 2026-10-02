package br.com.bussola.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.bussola.controller.UsuarioController;
import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.service.UsuarioService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, CadastroExceptionHandler.class})
class CadastroCorsTest {

    private static final String ORIGEM_FRONTEND = "http://127.0.0.1:8765";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void preflightDoCadastroAceitaOrigemDoFrontend() throws Exception {
        mockMvc.perform(options("/auth/cadastro")
                        .header("Origin", ORIGEM_FRONTEND)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,accept"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_FRONTEND))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("content-type")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("accept")));
    }

    @Test
    void preflightDoCadastroAceitaLocalhostNaMesmaPorta() throws Exception {
        mockMvc.perform(options("/auth/cadastro")
                        .header("Origin", "http://localhost:8765")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8765"));
    }

    @Test
    void postDoCadastroDevolveOrigemPermitida() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNomeCompleto("Usuaria Teste");
        usuario.setEmail("cors.teste@example.com");
        when(usuarioService.cadastrar(eq("Usuaria Teste"), eq(LocalDate.of(2000, 1, 1)),
                eq("cors.teste@example.com"), eq("senha-segura"), eq(true))).thenReturn(usuario);

        mockMvc.perform(post("/auth/cadastro")
                        .header("Origin", ORIGEM_FRONTEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeCompleto": "Usuaria Teste",
                                  "dataNascimento": "2000-01-01",
                                  "email": "cors.teste@example.com",
                                  "senha": "senha-segura",
                                  "aceitouTermos": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_FRONTEND));
    }

    @Test
    void preflightRejeitaOrigemNaoListada() throws Exception {
        mockMvc.perform(options("/auth/cadastro")
                        .header("Origin", "https://example.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
