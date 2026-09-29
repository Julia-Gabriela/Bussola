package br.com.bussola.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "avaliacoes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_avaliacoes_criterio_opcao",
                columnNames = {"criterio_id", "opcao_id"}
        )
)
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterio_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Criterio criterio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Opcao opcao;

    @Column(name = "valor", nullable = false)
    private Integer valor;

    /**
     * Compara esta avaliação com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code Avaliacao} e já
     * possuem o mesmo {@code id}. Uma avaliação ainda não inserida só é igual a si
     * mesma. Critério e opção não participam da comparação. A unicidade do par
     * critério/opção é garantida pela constraint {@code uk_avaliacoes_criterio_opcao},
     * não por este método.
     *
     * @param outro objeto que será comparado com esta avaliação; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code avaliacoes}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Avaliacao avaliacao)) {
            return false;
        }
        return id != null && id.equals(avaliacao.getId());
    }

    /**
     * Calcula um código de hash estável para esta avaliação.
     * <p>
     * O valor depende apenas da classe {@code Avaliacao}. Não usa o {@code id},
     * o valor numérico nem os relacionamentos com critério e opção.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
