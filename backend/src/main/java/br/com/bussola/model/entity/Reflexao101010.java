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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reflexao_101010")
public class Reflexao101010 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id", nullable = false, unique = true)
    private Opcao opcao;

    @Column(name = "curto_prazo", columnDefinition = "TEXT")
    private String curtoPrazo;

    @Column(name = "medio_prazo", columnDefinition = "TEXT")
    private String medioPrazo;

    @Column(name = "longo_prazo", columnDefinition = "TEXT")
    private String longoPrazo;
}
