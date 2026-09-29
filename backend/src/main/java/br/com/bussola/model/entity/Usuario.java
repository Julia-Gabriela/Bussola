package br.com.bussola.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "aceite_termos_em", nullable = false)
    private LocalDateTime aceiteTermosEm;

    @Column(name = "exclusao_solicitada_em")
    private LocalDateTime exclusaoSolicitadaEm;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Decisao> decisoes = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<TokenRecuperacaoSenha> tokensRecuperacaoSenha = new ArrayList<>();

    /**
     * Compara esta conta com outro objeto usando somente a identidade persistente.
     * <p>
     * A comparação considera duas instâncias iguais quando ambas são da classe
     * {@code Usuario} e já possuem o mesmo {@code id} gerado pelo banco. Uma conta
     * ainda não inserida, com {@code id} nulo, só é igual a si mesma. Assim, dois
     * cadastros novos não são tratados como o mesmo registro.
     * Decisões, tokens e demais relacionamentos ficam de fora para não percorrer
     * coleções, não disparar carregamento preguiçoso e não provocar recursão.
     *
     * @param outro objeto que será comparado com esta conta; pode ser nulo
     * @return {@code true} se for a mesma instância ou a mesma linha já persistida
     *         de {@code usuarios}; {@code false} caso contrário
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Usuario usuario)) {
            return false;
        }
        return id != null && id.equals(usuario.getId());
    }

    /**
     * Calcula um código de hash estável para esta conta.
     * <p>
     * O valor depende apenas da classe {@code Usuario}, e não do {@code id} nem
     * das coleções. A conta pode entrar em um {@code HashSet} ou {@code HashMap}
     * antes de o banco gerar o identificador, sem mudar de bucket depois da
     * inserção e sem inicializar relacionamentos LAZY.
     *
     * @return código de hash da classe concreta desta entidade
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
