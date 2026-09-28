package br.com.bussola.model.entity;

import br.com.bussola.model.enums.StatusDecisao;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "decisao")
public class Decisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "contexto", columnDefinition = "TEXT")
    private String contexto;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private StatusDecisao status = StatusDecisao.EM_ANDAMENTO;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "decisao")
    private List<Opcao> opcoes = new ArrayList<>();

    @OneToMany(mappedBy = "decisao")
    private List<Criterio> criterios = new ArrayList<>();

    @OneToMany(mappedBy = "decisao")
    private List<IdeiaBrainstorming> ideiasBrainstorming = new ArrayList<>();

    @OneToMany(mappedBy = "decisao")
    private List<ItemGut> itensGut = new ArrayList<>();

    @PrePersist
    protected void preencherDatas() {
        LocalDateTime agora = LocalDateTime.now();
        dataCriacao = agora;
        dataAtualizacao = agora;
    }

    @PreUpdate
    protected void atualizarData() {
        dataAtualizacao = LocalDateTime.now();
    }
}
