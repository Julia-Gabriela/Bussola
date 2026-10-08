package br.com.bussola.service;

import br.com.bussola.dto.response.DecisaoResumoResponse;
import br.com.bussola.dto.response.HomeResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {
    private final DecisaoRepository decisoes;

    public HomeService(DecisaoRepository decisoes) {
        this.decisoes = decisoes;
    }

    /** Consulta apenas decisões do usuário autenticado, sem carregar as análises completas. */
    @Transactional(readOnly = true)
    public HomeResponse consultar(UsuarioResponse usuario) {
        Long usuarioId = usuario.id();
        var recentes = decisoes.findTop4ByUsuarioIdOrderByDataAtualizacaoDescIdDesc(usuarioId)
                .stream().map(DecisaoResumoResponse::de).toList();
        var emAndamento = decisoes
                .findFirstByUsuarioIdAndStatusOrderByDataAtualizacaoDescIdDesc(usuarioId, StatusDecisao.EM_ANDAMENTO)
                .map(DecisaoResumoResponse::de).orElse(null);
        return new HomeResponse(usuario,
                decisoes.countByUsuarioIdAndStatus(usuarioId, StatusDecisao.CONCLUIDA),
                decisoes.countByUsuarioIdAndStatus(usuarioId, StatusDecisao.EM_ANDAMENTO), recentes, emAndamento);
    }
}
