package br.com.bussola.repository;

import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisaoRepository extends JpaRepository<Decisao, Long> {
    Optional<Decisao> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioIdAndStatus(Long usuarioId, StatusDecisao status);

    List<Decisao> findTop4ByUsuarioIdOrderByDataAtualizacaoDescIdDesc(Long usuarioId);

    Optional<Decisao> findFirstByUsuarioIdAndStatusOrderByDataAtualizacaoDescIdDesc(
            Long usuarioId, StatusDecisao status);
}
