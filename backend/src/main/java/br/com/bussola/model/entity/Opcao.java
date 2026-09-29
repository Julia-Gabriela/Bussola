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
import jakarta.persistence.OneToOne;
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
@Table(name = "opcoes")
public class Opcao {

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

    @OneToMany(mappedBy = "opcao", fetch = FetchType.LAZY)
    private List<Avaliacao> avaliacoes = new ArrayList<>();

    @OneToMany(mappedBy = "opcao", fetch = FetchType.LAZY)
    private List<ProContra> prosContras = new ArrayList<>();

    @OneToMany(mappedBy = "opcao", fetch = FetchType.LAZY)
    private List<Inversao> inversoes = new ArrayList<>();

    @OneToOne(mappedBy = "opcao", fetch = FetchType.LAZY)
    private Reflexao101010 reflexao101010;

    @OneToOne(mappedBy = "opcao", fetch = FetchType.LAZY)
    private Resultado resultado;

    /**
     * Compara esta opção com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code Opcao} e já
     * possuem o mesmo {@code id}. Uma opção ainda não inserida só é igual a si
     * mesma. Decisão, avaliações, prós e contras, inversões, reflexão e resultado
     * ficam de fora da comparação.
     *
     * @param outro objeto que será comparado com esta opção; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code opcoes}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Opcao opcao)) {
            return false;
        }
        return id != null && id.equals(opcao.getId());
    }

    /**
     * Calcula um código de hash estável para esta opção.
     * <p>
     * O valor depende apenas da classe {@code Opcao}. Não usa o {@code id} nem os
     * relacionamentos, então a entidade permanece no mesmo bucket de um conjunto
     * hash depois que o banco gera o identificador.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
