package br.com.bussola.model.entity;

import br.com.bussola.model.enums.TipoProContra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pros_contras")
public class ProContra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Opcao opcao;

    @Enumerated(EnumType.ORDINAL)
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "tipo", nullable = false, columnDefinition = "TINYINT")
    private TipoProContra tipo;

    @Column(name = "descricao", nullable = false, length = 300)
    private String descricao;

    /**
     * Compara este pró ou contra com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code ProContra} e já
     * possuem o mesmo {@code id}. Um registro ainda não inserido só é igual a si
     * mesmo. Tipo, descrição e opção não participam da comparação.
     *
     * @param outro objeto que será comparado com este registro; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code pros_contras}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof ProContra proContra)) {
            return false;
        }
        return id != null && id.equals(proContra.getId());
    }

    /**
     * Calcula um código de hash estável para este pró ou contra.
     * <p>
     * O valor depende apenas da classe {@code ProContra}. Não usa o {@code id},
     * o tipo nem a descrição.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
