package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.bussola.model.entity.SessaoAutenticacao;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.SessaoAutenticacaoRepository;
import br.com.bussola.repository.UsuarioRepository;
import br.com.bussola.security.JwtTokenProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AutenticacaoServiceTest {
    private final Instant agora = Instant.parse("2026-10-03T15:00:00Z");
    private final Clock clock = Clock.fixed(agora, ZoneOffset.UTC);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final SessaoAutenticacaoRepository sessoes = mock(SessaoAutenticacaoRepository.class);
    private final JwtTokenProvider tokens = new JwtTokenProvider(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", clock);
    private AutenticacaoService service;
    private SessaoAutenticacao sessao;
    private String token;

    @BeforeEach
    void preparar() {
        service = new AutenticacaoService(usuarios, sessoes, new BCryptPasswordEncoder(10), tokens, clock);
        var usuario = new Usuario();
        usuario.setId(1L);
        sessao = new SessaoAutenticacao();
        sessao.setId("sessao-teste");
        sessao.setUsuario(usuario);
        when(sessoes.buscarParaAutenticar(anyString())).thenReturn(Optional.of(sessao));
        token = tokens.gerar(1L, sessao.getId());
    }

    @Test
    void renovaAtividadeAntesDeCompletarSessentaMinutos() {
        sessao.setUltimaAtividade(LocalDateTime.now(clock).minusMinutes(60).plusSeconds(1));
        service.autenticar(token);
        assertEquals(LocalDateTime.now(clock), sessao.getUltimaAtividade());
    }

    @Test
    void expiraExatamenteAosSessentaMinutos() {
        var ultimaAtividade = LocalDateTime.now(clock).minusMinutes(60);
        sessao.setUltimaAtividade(ultimaAtividade);
        assertThrows(BadCredentialsException.class, () -> service.autenticar(token));
        assertEquals(ultimaAtividade, sessao.getUltimaAtividade());
    }

    @Test
    void rejeitaSessaoAusente() {
        when(sessoes.buscarParaAutenticar(anyString())).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> service.autenticar(token));
    }

    @Test
    void rejeitaTokenDeOutroUsuarioNaMesmaSessao() {
        sessao.setUltimaAtividade(LocalDateTime.now(clock));
        String outro = tokens.gerar(2L, sessao.getId());
        assertThrows(BadCredentialsException.class, () -> service.autenticar(outro));
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        var outroProvider = new JwtTokenProvider(
                "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=", clock);
        assertThrows(BadCredentialsException.class,
                () -> service.autenticar(outroProvider.gerar(1L, sessao.getId())));
    }
}
