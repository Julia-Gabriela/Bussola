package br.com.bussola.repository;

import br.com.bussola.model.entity.Decisao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisaoRepository extends JpaRepository<Decisao, Long> {
}
