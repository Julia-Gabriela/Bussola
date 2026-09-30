package br.com.bussola.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.bussola.exception.CadastroExceptionHandler;
import br.com.bussola.exception.CadastroInvalidoException;
import br.com.bussola.exception.EmailJaCadastradoException;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.security.SecurityConfig;
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
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void cadastraSemAutenticacaoERetornaApenasDadosPublicos() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNomeCompleto("Júlia Silva");
        usuario.setEmail("julia@exemplo.com");
        usuario.setSenhaHash("hash-interno");
        when(usuarioService.cadastrar(eq("Júlia Silva"), eq(LocalDate.of(2000, 1, 1)),
                eq("julia@exemplo.com"), eq("senha-segura"), eq(true))).thenReturn(usuario);

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nomeCompleto").value("Júlia Silva"))
                .andExpect(jsonPath("$.email").value("julia@exemplo.com"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void rejeitaEmailComFormatoInvalido() throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroValido().replace("julia@exemplo.com", "email-invalido")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void rejeitaCadastroSemAceiteDosTermos() throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroValido().replace("\"aceitouTermos\": true", "\"aceitouTermos\": false")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void rejeitaIdadeAbaixoDoMinimo() throws Exception {
        when(usuarioService.cadastrar(any(), any(), any(), any(), eq(true)))
                .thenThrow(new CadastroInvalidoException("A idade mínima para cadastro é 16 anos."));

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A idade mínima para cadastro é 16 anos."));
    }

    @Test
    void retornaConflitoParaEmailDuplicado() throws Exception {
        when(usuarioService.cadastrar(any(), any(), any(), any(), eq(true)))
                .thenThrow(new EmailJaCadastradoException("Este e-mail já está cadastrado."));

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Este e-mail já está cadastrado."));
    }

    private String cadastroValido() {
        return """
                {
                  "nomeCompleto": "Júlia Silva",
                  "dataNascimento": "2000-01-01",
                  "email": "julia@exemplo.com",
                  "senha": "senha-segura",
                  "aceitouTermos": true
                }
                """;
    }
}
