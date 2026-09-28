package br.com.bussola.repository;

import br.com.bussola.model.entity.Resultado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoRepository extends JpaRepository<Resultado, Long> {
}
