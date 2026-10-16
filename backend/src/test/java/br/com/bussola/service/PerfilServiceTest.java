package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import br.com.bussola.dto.request.*;
import br.com.bussola.exception.*;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.repository.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PerfilServiceTest {
    final UsuarioRepository usuarios=mock(UsuarioRepository.class);
    final SessaoAutenticacaoRepository sessoes=mock(SessaoAutenticacaoRepository.class);
    final TokenRecuperacaoSenhaRepository recuperacoes=mock(TokenRecuperacaoSenhaRepository.class);
    final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    final Clock clock=Clock.fixed(Instant.parse("2026-10-15T15:00:00Z"),ZoneOffset.UTC);
    final PerfilService service=new PerfilService(usuarios,sessoes,recuperacoes,encoder,clock);
    final Usuario usuario=new Usuario();
    @BeforeEach void preparar() {
        usuario.setId(7L); usuario.setNomeCompleto("Nome original"); usuario.setEmail("pessoa@example.invalid");
        usuario.setDataNascimento(LocalDate.of(2000,1,1)); usuario.setSenhaHash(encoder.encode("senha-atual"));
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarios.buscarParaAtualizar(7L)).thenReturn(Optional.of(usuario));
    }
    AtualizarPerfilRequest dados(String email,String senha) {
        return new AtualizarPerfilRequest(" Nome atualizado ",LocalDate.of(2000,1,1),email,senha);
    }
    @Test void consultaSomenteContaAutenticada() {
        var resposta=service.consultar(7L);
        assertEquals(usuario.getDataNascimento(),resposta.dataNascimento());
        assertEquals(7L,resposta.id()); verify(usuarios,never()).saveAndFlush(any());
        assertThrows(BadCredentialsException.class,()->service.consultar(8L));
    }
    @Test void atualizaDadosSemSenhaQuandoEmailNaoMuda() {
        var hash=usuario.getSenhaHash();
        var resposta=service.atualizar(7L,dados(" PESSOA@example.invalid ",null));
        assertEquals("Nome atualizado",resposta.nomeCompleto()); assertEquals("pessoa@example.invalid",resposta.email());
        assertEquals(hash,usuario.getSenhaHash()); verifyNoInteractions(sessoes,recuperacoes);
    }
    @Test void idadeMinimaIncluiAniversarioDeHoje() {
        service.atualizar(7L,new AtualizarPerfilRequest("Nome",LocalDate.of(2010,10,15),usuario.getEmail(),null));
        assertThrows(PerfilInvalidoException.class,()->service.atualizar(7L,new AtualizarPerfilRequest("Nome",LocalDate.of(2010,10,16),usuario.getEmail(),null)));
    }
    @Test void emailExigeSenhaAtualERevogaTokensDeRecuperacao() {
        assertThrows(PerfilInvalidoException.class,()->service.atualizar(7L,dados("novo@example.invalid",null)));
        assertThrows(PerfilInvalidoException.class,()->service.atualizar(7L,dados("novo@example.invalid","errada")));
        service.atualizar(7L,dados("NOVO@example.invalid","senha-atual"));
        assertEquals("novo@example.invalid",usuario.getEmail());
        verify(recuperacoes).revogarTodosDoUsuario(7L); verifyNoInteractions(sessoes);
    }
    @Test void emailDuplicadoNaoAlteraDados() {
        when(usuarios.existsByEmailIgnoreCaseAndIdNot("novo@example.invalid",7L)).thenReturn(true);
        assertThrows(EmailJaCadastradoException.class,()->service.atualizar(7L,dados("novo@example.invalid","senha-atual")));
        assertEquals("Nome original",usuario.getNomeCompleto());
        verify(usuarios,never()).saveAndFlush(any());
    }
    @Test void conflitoDeEmailNoBancoRetornaErroControlado() {
        when(usuarios.saveAndFlush(usuario)).thenThrow(new DataIntegrityViolationException("unique"));
        assertThrows(EmailJaCadastradoException.class,()->service.atualizar(7L,dados("novo@example.invalid","senha-atual")));
        verifyNoInteractions(recuperacoes,sessoes);
    }
    @Test void trocaSenhaComHashERevogaTodasAsSessoesETokens() {
        service.alterarSenha(7L,new AlterarSenhaRequest("senha-atual","nova-senha-segura","nova-senha-segura"));
        assertTrue(encoder.matches("nova-senha-segura",usuario.getSenhaHash()));
        assertFalse(encoder.matches("senha-atual",usuario.getSenhaHash()));
        verify(sessoes).revogarTodasDoUsuario(7L); verify(recuperacoes).revogarTodosDoUsuario(7L);
    }
    @Test void senhaIncorretaConfirmacaoDiferenteReusoELimiteNaoRevogamSessao() {
        for(var request:java.util.List.of(
                new AlterarSenhaRequest("errada","nova-senha","nova-senha"),
                new AlterarSenhaRequest("senha-atual","nova-senha","diferente"),
                new AlterarSenhaRequest("senha-atual","senha-atual","senha-atual"),
                new AlterarSenhaRequest("senha-atual","curta","curta"),
                new AlterarSenhaRequest("senha-atual","á".repeat(37),"á".repeat(37)))) {
            assertThrows(PerfilInvalidoException.class,()->service.alterarSenha(7L,request));
        }
        verifyNoInteractions(sessoes,recuperacoes);verify(usuarios,never()).saveAndFlush(any());
    }
    @Test void senhaPreservaEspacosEPermiteLimiteExatoDoBCrypt() {
        String senha=" "+"a".repeat(70)+" ";
        service.alterarSenha(7L,new AlterarSenhaRequest("senha-atual",senha,senha));
        assertTrue(encoder.matches(senha,usuario.getSenhaHash()));
    }
    @Test void exclusaoExigeSenhaEConfirmacaoAntesDeExcluir() {
        assertThrows(PerfilInvalidoException.class,()->service.excluir(7L,new ExcluirContaRequest("senha-atual",false)));
        assertThrows(PerfilInvalidoException.class,()->service.excluir(7L,new ExcluirContaRequest("errada",true)));
        verify(usuarios,never()).delete(any());
        service.excluir(7L,new ExcluirContaRequest("senha-atual",true));
        verify(usuarios).delete(usuario); verify(usuarios).flush();
    }
    @Test void contaInexistenteNaoPodeSerAlteradaOuExcluida() {
        assertThrows(BadCredentialsException.class,()->service.atualizar(8L,dados("a@example.invalid",null)));
        assertThrows(BadCredentialsException.class,()->service.alterarSenha(8L,new AlterarSenhaRequest("a","b","b")));
        assertThrows(BadCredentialsException.class,()->service.excluir(8L,new ExcluirContaRequest("a",true)));
        verify(usuarios,never()).delete(any()); verifyNoInteractions(sessoes,recuperacoes);
    }
}
