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
@Table(name = "ideias_brainstorming")
public class IdeiaBrainstorming {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decisao_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Decisao decisao;

    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "categoria", length = 50)
    private String categoria;

    /**
     * Compara esta ideia com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code IdeiaBrainstorming}
     * e já possuem o mesmo {@code id}. Uma ideia ainda não inserida só é igual a si
     * mesma. A decisão associada não participa da comparação.
     *
     * @param outro objeto que será comparado com esta ideia; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code ideias_brainstorming}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof IdeiaBrainstorming ideia)) {
            return false;
        }
        return id != null && id.equals(ideia.getId());
    }

    /**
     * Calcula um código de hash estável para esta ideia.
     * <p>
     * O valor depende apenas da classe {@code IdeiaBrainstorming}. Não usa o
     * {@code id}, o conteúdo nem a decisão.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
