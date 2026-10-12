package br.com.bussola.repository;

import br.com.bussola.model.entity.Criterio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriterioRepository extends JpaRepository<Criterio, Long> {
    List<Criterio> findByDecisaoIdOrderByIdAsc(Long decisaoId);
}
