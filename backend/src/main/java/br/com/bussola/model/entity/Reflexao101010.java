package br.com.bussola.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "reflexoes_101010")
public class Reflexao101010 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Opcao opcao;

    @Column(name = "curto_prazo", columnDefinition = "TEXT")
    private String curtoPrazo;

    @Column(name = "medio_prazo", columnDefinition = "TEXT")
    private String medioPrazo;

    @Column(name = "longo_prazo", columnDefinition = "TEXT")
    private String longoPrazo;

    /**
     * Compara esta reflexão 10/10/10 com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code Reflexao101010}
     * e já possuem o mesmo {@code id}. Uma reflexão ainda não inserida só é igual
     * a si mesma. A opção e os textos de prazo não participam da comparação. A
     * relação opcional de uma reflexão por opção é o índice único de {@code opcao_id}.
     *
     * @param outro objeto que será comparado com esta reflexão; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code reflexoes_101010}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Reflexao101010 reflexao)) {
            return false;
        }
        return id != null && id.equals(reflexao.getId());
    }

    /**
     * Calcula um código de hash estável para esta reflexão 10/10/10.
     * <p>
     * O valor depende apenas da classe {@code Reflexao101010}. Não usa o {@code id}
     * nem os textos de curto, médio e longo prazo.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
