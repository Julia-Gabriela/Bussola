package br.com.bussola.repository;

import br.com.bussola.model.entity.ItemGut;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemGutRepository extends JpaRepository<ItemGut, Long> {
    List<ItemGut> findByDecisaoIdOrderByIdAsc(Long decisaoId);
    Optional<ItemGut> findByIdAndDecisaoId(Long id, Long decisaoId);
}
