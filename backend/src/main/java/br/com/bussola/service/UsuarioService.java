package br.com.bussola.service;

import br.com.bussola.exception.CadastroInvalidoException;
import br.com.bussola.exception.EmailJaCadastradoException;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario cadastrar(String nomeCompleto, LocalDate dataNascimento, String email,
            String senha, boolean aceitouTermos) {
        if (nomeCompleto == null || nomeCompleto.isBlank()
                || dataNascimento == null || email == null || email.isBlank()
                || senha == null || senha.isBlank()) {
            throw new CadastroInvalidoException("Todos os dados de cadastro são obrigatórios.");
        }
        if (!aceitouTermos) {
            throw new CadastroInvalidoException("É necessário aceitar os termos de uso.");
        }
        if (dataNascimento.isAfter(LocalDate.now().minusYears(16))) {
            throw new CadastroInvalidoException("A idade mínima para cadastro é 16 anos.");
        }

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new EmailJaCadastradoException("Este e-mail já está cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(nomeCompleto.trim());
        usuario.setDataNascimento(dataNascimento);
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setAceiteTermosEm(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }
}
