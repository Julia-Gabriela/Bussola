package br.com.bussola.service;

import br.com.bussola.dto.request.AtualizarPerfilRequest;
import br.com.bussola.dto.request.AlterarSenhaRequest;
import br.com.bussola.dto.response.PerfilResponse;
import br.com.bussola.exception.EmailJaCadastradoException;
import br.com.bussola.exception.PerfilInvalidoException;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {
    private final UsuarioRepository usuarios;
    private final SessaoAutenticacaoRepository sessoes;
    private final TokenRecuperacaoSenhaRepository recuperacoes;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public PerfilService(UsuarioRepository usuarios, SessaoAutenticacaoRepository sessoes,
            TokenRecuperacaoSenhaRepository recuperacoes, PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios; this.sessoes = sessoes; this.recuperacoes = recuperacoes;
        this.encoder = encoder; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PerfilResponse consultar(Long usuarioId) {
        return PerfilResponse.de(usuarios.findById(usuarioId)
                .orElseThrow(() -> new BadCredentialsException("Conta indisponível.")));
    }

    @Transactional
    public PerfilResponse atualizar(Long usuarioId, AtualizarPerfilRequest request) {
        var usuario = bloquear(usuarioId);
        if (request == null || request.nomeCompleto() == null || request.nomeCompleto().isBlank()
                || request.nomeCompleto().length() > 150 || request.email() == null || request.email().isBlank()
                || request.email().length() > 150 || request.dataNascimento() == null)
            throw new PerfilInvalidoException("Preencha seus dados pessoais.");
        if (request.dataNascimento().isAfter(LocalDate.now(clock).minusYears(16)))
            throw new PerfilInvalidoException("A idade mínima para usar o Bússola é 16 anos.");
        var email = request.email().strip().toLowerCase(Locale.ROOT);
        boolean mudouEmail = !email.equalsIgnoreCase(usuario.getEmail());
        if (mudouEmail) {
            conferirSenha(usuario, request.senhaAtual());
            if (usuarios.existsByEmailIgnoreCaseAndIdNot(email, usuarioId))
                throw new EmailJaCadastradoException("Este e-mail já está cadastrado.");
        }
        usuario.setNomeCompleto(request.nomeCompleto().strip());
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setEmail(email);
        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException exception) {
            // A restrição UNIQUE também protege dois salvamentos simultâneos.
            throw new EmailJaCadastradoException("Não foi possível usar este e-mail. Escolha outro.");
        }
        if (mudouEmail) recuperacoes.revogarTodosDoUsuario(usuarioId);
        return PerfilResponse.de(usuario);
    }

    @Transactional
    public void alterarSenha(Long usuarioId, AlterarSenhaRequest request) {
        var usuario = bloquear(usuarioId);
        if (request == null) throw new PerfilInvalidoException("Informe a senha atual e a nova senha.");
        conferirSenha(usuario, request.senhaAtual());
        var nova = request.novaSenha();
        if (nova == null || nova.isBlank() || nova.length() < 8
                || nova.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new PerfilInvalidoException("A nova senha deve ter pelo menos 8 caracteres e até 72 bytes.");
        if (!nova.equals(request.confirmacaoSenha()))
            throw new PerfilInvalidoException("A confirmação não corresponde à nova senha.");
        if (encoder.matches(nova, usuario.getSenhaHash()))
            throw new PerfilInvalidoException("Escolha uma senha diferente da atual.");
        usuario.setSenhaHash(encoder.encode(nova));
        usuarios.saveAndFlush(usuario);
        sessoes.revogarTodasDoUsuario(usuarioId);
        recuperacoes.revogarTodosDoUsuario(usuarioId);
    }

    @Transactional
    public void excluir(Long usuarioId, br.com.bussola.dto.request.ExcluirContaRequest request) {
        var usuario = bloquear(usuarioId);
        if (request == null || !Boolean.TRUE.equals(request.confirmar()))
            throw new PerfilInvalidoException("Confirme a exclusão definitiva da conta e dos dados.");
        conferirSenha(usuario, request.senhaAtual());
        // As FKs existentes removem decisões, análises, sessões e tokens em cascata.
        usuarios.delete(usuario);
        usuarios.flush();
    }

    private Usuario bloquear(Long usuarioId) {
        return usuarios.buscarParaAtualizar(usuarioId)
                .orElseThrow(() -> new BadCredentialsException("Conta indisponível."));
    }

    private void conferirSenha(Usuario usuario, String senha) {
        if (senha == null || senha.isBlank() || senha.getBytes(StandardCharsets.UTF_8).length > 72
                || !encoder.matches(senha, usuario.getSenhaHash()))
            throw new PerfilInvalidoException("A senha atual está incorreta.");
    }
}
