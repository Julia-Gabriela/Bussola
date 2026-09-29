package br.com.bussola.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tokens_recuperacao_senha")
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    /**
     * Preenche a data de criação do token quando ela ainda não foi informada.
     * <p>
     * O banco também grava {@code criado_em} com {@code CURRENT_TIMESTAMP}. Este
     * callback evita inserir nulo pela aplicação e preserva um valor que já tenha
     * sido atribuído antes do persist. Não envia e-mail nem valida o token.
     */
    @PrePersist
    protected void preencherCriadoEm() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    /**
     * Compara este token com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe
     * {@code TokenRecuperacaoSenha} e já possuem o mesmo {@code id}. Um token ainda
     * não inserido só é igual a si mesmo. O hash, a validade e o usuário não
     * participam da comparação. A unicidade do hash fica na coluna {@code token_hash}.
     *
     * @param outro objeto que será comparado com este token; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code tokens_recuperacao_senha}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof TokenRecuperacaoSenha token)) {
            return false;
        }
        return id != null && id.equals(token.getId());
    }

    /**
     * Calcula um código de hash estável para este token.
     * <p>
     * O valor depende apenas da classe {@code TokenRecuperacaoSenha}. Não usa o
     * {@code id} nem o hash armazenado, então o segredo do token não entra em
     * estruturas de hash da JVM.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
