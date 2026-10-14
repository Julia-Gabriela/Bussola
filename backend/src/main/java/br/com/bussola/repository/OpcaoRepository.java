package br.com.bussola.repository;

import br.com.bussola.model.entity.Opcao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpcaoRepository extends JpaRepository<Opcao, Long> {
    List<Opcao> findByDecisaoIdOrderByIdAsc(Long decisaoId);

    boolean existsByDecisaoIdAndAvaliacaoPendenteTrue(Long decisaoId);
}
