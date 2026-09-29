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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "itens_gut")
public class ItemGut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decisao_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Decisao decisao;

    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao;

    @Column(name = "gravidade", nullable = false)
    private Integer gravidade;

    @Column(name = "urgencia", nullable = false)
    private Integer urgencia;

    @Column(name = "tendencia", nullable = false)
    private Integer tendencia;

    /**
     * Compara este item GUT com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code ItemGut} e já
     * possuem o mesmo {@code id}. Um item ainda não inserido só é igual a si mesmo.
     * Gravidade, urgência, tendência e a decisão não participam da comparação.
     *
     * @param outro objeto que será comparado com este item; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code itens_gut}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof ItemGut item)) {
            return false;
        }
        return id != null && id.equals(item.getId());
    }

    /**
     * Calcula um código de hash estável para este item GUT.
     * <p>
     * O valor depende apenas da classe {@code ItemGut}. Não usa o {@code id} nem
     * as notas de gravidade, urgência e tendência.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
