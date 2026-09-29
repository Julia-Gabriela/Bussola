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
@Table(name = "inversoes")
public class Inversao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Opcao opcao;

    @Column(name = "perspectiva", length = 100)
    private String perspectiva;

    @Column(name = "reflexao", columnDefinition = "TEXT")
    private String reflexao;

    /**
     * Compara esta inversão com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code Inversao} e já
     * possuem o mesmo {@code id}. Uma inversão ainda não inserida só é igual a si
     * mesma. Opção, perspectiva e texto da reflexão não participam da comparação.
     *
     * @param outro objeto que será comparado com esta inversão; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code inversoes}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Inversao inversao)) {
            return false;
        }
        return id != null && id.equals(inversao.getId());
    }

    /**
     * Calcula um código de hash estável para esta inversão.
     * <p>
     * O valor depende apenas da classe {@code Inversao}. Não usa o {@code id} nem
     * o texto da reflexão.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
