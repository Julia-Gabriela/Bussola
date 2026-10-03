package br.com.bussola.repository;

import br.com.bussola.model.entity.SessaoAutenticacao;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoAutenticacaoRepository extends JpaRepository<SessaoAutenticacao, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoAutenticacao s join fetch s.usuario where s.id = :id")
    Optional<SessaoAutenticacao> buscarParaAutenticar(@Param("id") String id);
}
