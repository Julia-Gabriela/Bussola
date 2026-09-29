package br.com.bussola.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "criterios")
public class Criterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decisao_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Decisao decisao;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "peso", nullable = false)
    private Integer peso = 1;

    @Column(name = "personalizado", nullable = false)
    private boolean personalizado;

    @OneToMany(mappedBy = "criterio", fetch = FetchType.LAZY)
    private List<Avaliacao> avaliacoes = new ArrayList<>();

    /**
     * Compara este critério com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code Criterio} e já
     * possuem o mesmo {@code id}. Um critério ainda não inserido só é igual a si
     * mesmo. A decisão e a lista de avaliações não participam da comparação.
     *
     * @param outro objeto que será comparado com este critério; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code criterios}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Criterio criterio)) {
            return false;
        }
        return id != null && id.equals(criterio.getId());
    }

    /**
     * Calcula um código de hash estável para este critério.
     * <p>
     * O valor depende apenas da classe {@code Criterio}. Não usa o {@code id},
     * o peso nem a coleção de avaliações.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
