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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pro_contra")
public class ProContra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false)
    private Opcao opcao;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "tipo", nullable = false, columnDefinition = "TINYINT")
    private TipoProContra tipo;

    @Column(name = "descricao", nullable = false)
    private String descricao;
}
