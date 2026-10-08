package br.com.bussola.service;

import br.com.bussola.dto.response.LoginResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.model.entity.SessaoAutenticacao;
import br.com.bussola.repository.SessaoAutenticacaoRepository;
import br.com.bussola.repository.UsuarioRepository;
import br.com.bussola.security.JwtTokenProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {
    private static final Duration INATIVIDADE_MAXIMA = Duration.ofMinutes(60);
    private final UsuarioRepository usuarios;
    private final SessaoAutenticacaoRepository sessoes;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider tokens;
    private final Clock clock;
    private final String hashSimulado;

    public AutenticacaoService(UsuarioRepository usuarios, SessaoAutenticacaoRepository sessoes,
            PasswordEncoder encoder, JwtTokenProvider tokens, Clock clock) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.encoder = encoder;
        this.tokens = tokens;
        this.clock = clock;
        this.hashSimulado = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginResponse login(String email, String senha) {
        var usuario = usuarios.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT));
        boolean senhaValida = encoder.matches(senha, usuario.map(u -> u.getSenhaHash()).orElse(hashSimulado));
        if (usuario.isEmpty() || !senhaValida) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }
        var sessao = new SessaoAutenticacao();
        sessao.setId(UUID.randomUUID().toString());
        sessao.setUsuario(usuario.get());
        sessao.setUltimaAtividade(LocalDateTime.now(clock));
        sessoes.save(sessao);
        return new LoginResponse(tokens.gerar(usuario.get().getId(), sessao.getId()),
                "Bearer", INATIVIDADE_MAXIMA.toSeconds(), UsuarioResponse.de(usuario.get()));
    }

    /** Revoga somente a sessão apresentada; outros dispositivos permanecem conectados. */
    @Transactional
    public void encerrarSessao(String token, Long usuarioId) {
        var jwt = tokens.decodificar(token);
        var sessao = sessoes.buscarParaAutenticar(jwt.getId());
        if (sessao.isPresent()) {
            if (!sessao.get().getUsuario().getId().equals(usuarioId)
                    || !usuarioId.toString().equals(jwt.getSubject())) {
                throw new BadCredentialsException("Sessão inválida ou expirada.");
            }
            sessoes.delete(sessao.get());
        }
    }

    @Transactional
    public UsuarioResponse autenticar(String token) {
        try {
            var jwt = tokens.decodificar(token);
            if (jwt.getId() == null || jwt.getSubject() == null || jwt.getIssuedAt() == null
                    || jwt.getIssuedAt().isAfter(clock.instant())) {
                throw new BadCredentialsException("Sessão inválida ou expirada.");
            }
            var sessao = sessoes.buscarParaAutenticar(jwt.getId())
                    .orElseThrow(() -> new BadCredentialsException("Sessão inválida ou expirada."));
            var agora = LocalDateTime.now(clock);
            if (!sessao.getUsuario().getId().toString().equals(jwt.getSubject())
                    || !sessao.getUltimaAtividade().plus(INATIVIDADE_MAXIMA).isAfter(agora)) {
                throw new BadCredentialsException("Sessão inválida ou expirada.");
            }
            sessao.setUltimaAtividade(agora);
            return UsuarioResponse.de(sessao.getUsuario());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Sessão inválida ou expirada.");
        }
    }
}
