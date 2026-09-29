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
@Table(name = "resultados")
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Opcao opcao;

    @Column(name = "pontuacao_total")
    private Integer pontuacaoTotal;

    @Column(name = "data_calculo")
    private LocalDateTime dataCalculo;

    /**
     * Compara este resultado com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code Resultado} e já
     * possuem o mesmo {@code id}. Um resultado ainda não inserido só é igual a si
     * mesmo. Pontuação, data de cálculo e opção não participam da comparação. A
     * relação de um resultado por opção é o índice único de {@code opcao_id}.
     *
     * @param outro objeto que será comparado com este resultado; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code resultados}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Resultado resultado)) {
            return false;
        }
        return id != null && id.equals(resultado.getId());
    }

    /**
     * Calcula um código de hash estável para este resultado.
     * <p>
     * O valor depende apenas da classe {@code Resultado}. Não usa o {@code id} nem
     * a pontuação total.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
