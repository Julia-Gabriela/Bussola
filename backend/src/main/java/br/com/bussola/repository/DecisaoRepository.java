package br.com.bussola.repository;

import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DecisaoRepository extends JpaRepository<Decisao, Long> {
    Optional<Decisao> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Decisao d where d.id = :id and d.usuario.id = :usuarioId")
    Optional<Decisao> buscarParaAtualizar(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    long countByUsuarioIdAndStatus(Long usuarioId, StatusDecisao status);

    List<Decisao> findTop4ByUsuarioIdOrderByDataAtualizacaoDescIdDesc(Long usuarioId);

    Optional<Decisao> findFirstByUsuarioIdAndStatusOrderByDataAtualizacaoDescIdDesc(
            Long usuarioId, StatusDecisao status);
}
