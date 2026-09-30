package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.UsuarioRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        usuarioService = new UsuarioService(usuarioRepository, new BCryptPasswordEncoder(10));
    }

    @Test
    void cadastraUsuarioComDezesseisAnosCompletos() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario usuario = usuarioService.cadastrar("  Júlia Silva  ", LocalDate.now().minusYears(16),
                "  JULIA@EXEMPLO.COM  ", "senha-segura", true);

        assertEquals("Júlia Silva", usuario.getNomeCompleto());
        assertEquals("julia@exemplo.com", usuario.getEmail());
        assertTrue(new BCryptPasswordEncoder().matches("senha-segura", usuario.getSenhaHash()));
        assertFalse(usuario.getSenhaHash().contains("senha-segura"));
        assertTrue(usuario.getSenhaHash().startsWith("$2"));
        assertEquals("10", usuario.getSenhaHash().substring(4, 6));
        assertTrue(usuario.getAceiteTermosEm() != null);
        verify(usuarioRepository).existsByEmailIgnoreCase("julia@exemplo.com");
    }

    @Test
    void rejeitaPessoaComMenosDeDezesseisAnos() {
        assertThrows(IllegalArgumentException.class, () -> usuarioService.cadastrar("Júlia Silva",
                LocalDate.now().minusYears(16).plusDays(1), "julia@exemplo.com", "senha-segura", true));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejeitaEmailJaCadastrado() {
        when(usuarioRepository.existsByEmailIgnoreCase("julia@exemplo.com")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> usuarioService.cadastrar("Júlia Silva",
                LocalDate.now().minusYears(20), " JULIA@EXEMPLO.COM ", "senha-segura", true));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void exigeAceiteDosTermos() {
        assertThrows(IllegalArgumentException.class, () -> usuarioService.cadastrar("Júlia Silva",
                LocalDate.now().minusYears(20), "julia@exemplo.com", "senha-segura", false));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejeitaDadosObrigatoriosVazios() {
        assertThrows(IllegalArgumentException.class, () -> usuarioService.cadastrar("  ",
                LocalDate.now().minusYears(20), "julia@exemplo.com", "senha-segura", true));

        verify(usuarioRepository, never()).save(any());
    }
}
