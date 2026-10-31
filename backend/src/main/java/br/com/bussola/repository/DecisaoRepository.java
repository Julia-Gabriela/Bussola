package br.com.bussola.repository;

import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DecisaoRepository extends JpaRepository<Decisao, Long> {
    @Query("""
            select d from Decisao d where d.usuario.id = :usuarioId
            and (:status is null or d.status = :status)
            and lower(d.titulo) like lower(:busca) escape '!'
            """)
    Page<Decisao> pesquisarHistorico(@Param("usuarioId") Long usuarioId,
            @Param("status") StatusDecisao status, @Param("busca") String busca, Pageable pagina);

    Optional<Decisao> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Decisao d where d.id = :id and d.usuario.id = :usuarioId")
    Optional<Decisao> buscarParaAtualizar(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    /**
     * Registra, uma única vez, que uma decisão em andamento chegou ao Resultado Comparativo.
     * <p>
     * Atualiza apenas {@code resultado_alcancado_em}, e só quando a decisão está com o
     * status informado (em andamento) e a coluna ainda é nula. Por ser um UPDATE direto,
     * não sobrescreve outros campos alterados por requisições concorrentes; chamadas
     * repetidas ou simultâneas não têm efeito depois da primeira. {@code data_atualizacao}
     * é reatribuída a si mesma para que o MySQL não a renove: ver o resultado não é uma edição.
     *
     * @param id identificador da decisão (a posse já deve ter sido validada)
     * @param emAndamento status exigido, normalmente {@link StatusDecisao#EM_ANDAMENTO}
     * @param agora instante gravado como chegada ao resultado
     * @return quantidade de linhas alteradas: 1 na primeira chegada, 0 nas demais
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Decisao d set d.resultadoAlcancadoEm = :agora, d.dataAtualizacao = d.dataAtualizacao
            where d.id = :id and d.status = :emAndamento and d.resultadoAlcancadoEm is null
            """)
    int registrarResultadoAlcancado(@Param("id") Long id, @Param("emAndamento") StatusDecisao emAndamento,
            @Param("agora") LocalDateTime agora);

    long countByUsuarioIdAndStatus(Long usuarioId, StatusDecisao status);

    List<Decisao> findTop4ByUsuarioIdOrderByDataAtualizacaoDescIdDesc(Long usuarioId);

    Optional<Decisao> findFirstByUsuarioIdAndStatusOrderByDataAtualizacaoDescIdDesc(
            Long usuarioId, StatusDecisao status);
}
