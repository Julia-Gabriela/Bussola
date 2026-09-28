package br.com.bussola.repository;

import br.com.bussola.model.entity.Inversao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InversaoRepository extends JpaRepository<Inversao, Long> {
}
