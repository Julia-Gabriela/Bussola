package br.com.bussola.repository;

import br.com.bussola.model.entity.IdeiaBrainstorming;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeiaBrainstormingRepository extends JpaRepository<IdeiaBrainstorming, Long> {
    List<IdeiaBrainstorming> findByDecisaoIdOrderByIdAsc(Long decisaoId);

    Optional<IdeiaBrainstorming> findByIdAndDecisaoId(Long id, Long decisaoId);
}
