package br.com.bussola.repository;

import br.com.bussola.model.entity.ProContra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProContraRepository extends JpaRepository<ProContra, Long> {
    List<ProContra> findByOpcaoIdOrderByIdAsc(Long opcaoId);
    Optional<ProContra> findByIdAndOpcaoId(Long id, Long opcaoId);
}
