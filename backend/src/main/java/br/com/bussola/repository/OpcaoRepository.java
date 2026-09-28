package br.com.bussola.repository;

import br.com.bussola.model.entity.Opcao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpcaoRepository extends JpaRepository<Opcao, Long> {
}
