package br.com.bussola.repository;

import br.com.bussola.model.entity.Avaliacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByCriterioDecisaoId(Long decisaoId);
}
