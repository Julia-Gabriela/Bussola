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
import jakarta.persistence.Index;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "decisoes",
        indexes = @Index(
                name = "idx_decisoes_usuario_status_atualizacao",
                columnList = "usuario_id, status, data_atualizacao"
        )
)
public class Decisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "contexto", columnDefinition = "TEXT")
    private String contexto;

    @Enumerated(EnumType.ORDINAL)
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private StatusDecisao status = StatusDecisao.EM_ANDAMENTO;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "etapa_atual", nullable = false, columnDefinition = "TINYINT")
    private Integer etapaAtual = 1;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "decisao", fetch = FetchType.LAZY)
    private List<Opcao> opcoes = new ArrayList<>();

    @OneToMany(mappedBy = "decisao", fetch = FetchType.LAZY)
    private List<Criterio> criterios = new ArrayList<>();

    @OneToMany(mappedBy = "decisao", fetch = FetchType.LAZY)
    private List<IdeiaBrainstorming> ideiasBrainstorming = new ArrayList<>();

    @OneToMany(mappedBy = "decisao", fetch = FetchType.LAZY)
    private List<ItemGut> itensGut = new ArrayList<>();

    /**
     * Preenche as datas de criação e de atualização no momento da primeira inserção.
     * <p>
     * Os dois campos recebem o mesmo instante local. A coluna {@code data_criacao}
     * não é atualizada depois disso. O banco também possui {@code CURRENT_TIMESTAMP}
     * como valor padrão; este callback mantém a instância em memória alinhada ao
     * que será gravado.
     */
    @PrePersist
    protected void preencherDatas() {
        LocalDateTime agora = LocalDateTime.now();
        dataCriacao = agora;
        dataAtualizacao = agora;
    }

    /**
     * Atualiza {@code dataAtualizacao} imediatamente antes de uma alteração.
     * <p>
     * A data de criação permanece intacta. O banco também renova
     * {@code data_atualizacao} com {@code ON UPDATE CURRENT_TIMESTAMP}.
     */
    @PreUpdate
    protected void atualizarData() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Compara esta decisão com outro objeto usando somente a identidade persistente.
     * <p>
     * Duas instâncias são iguais quando ambas são da classe {@code Decisao} e já
     * possuem o mesmo {@code id}. Uma decisão ainda não inserida só é igual a si
     * mesma. Usuário, opções, critérios e as demais coleções não participam da
     * comparação, para evitar recursão e carregamento LAZY.
     *
     * @param outro objeto que será comparado com esta decisão; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code decisoes}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Decisao decisao)) {
            return false;
        }
        return id != null && id.equals(decisao.getId());
    }

    /**
     * Calcula um código de hash estável para esta decisão.
     * <p>
     * O valor depende apenas da classe {@code Decisao}. Não usa o {@code id} nem
     * as coleções, então a entidade pode ser colocada em estruturas de hash antes
     * de o identificador existir e sem inicializar relacionamentos LAZY.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
