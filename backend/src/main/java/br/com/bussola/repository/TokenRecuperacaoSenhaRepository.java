package br.com.bussola.repository;

import br.com.bussola.model.entity.TokenRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório da entidade {@link TokenRecuperacaoSenha}.
 * <p>
 * Expõe apenas as operações herdadas de {@link JpaRepository}: incluir, buscar
 * por identificador, listar, verificar existência, contar e excluir tokens já
 * persistidos na tabela {@code tokens_recuperacao_senha}. O identificador é
 * {@link Long}. Não há consulta derivada nem {@code @Query}.
 * Geração do token, envio de e-mail, expiração e marcação de uso ficam fora
 * deste repositório.
 */
public interface TokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenha, Long> {
}
