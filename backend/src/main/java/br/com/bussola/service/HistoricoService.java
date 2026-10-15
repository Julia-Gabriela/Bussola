package br.com.bussola.service;

import br.com.bussola.dto.response.DecisaoResumoResponse;
import br.com.bussola.dto.response.HistoricoResponse;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.HistoricoInvalidoException;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricoService {
    private final DecisaoRepository decisoes;

    public HistoricoService(DecisaoRepository decisoes) { this.decisoes = decisoes; }

    @Transactional(readOnly = true)
    public HistoricoResponse consultar(Long usuarioId, String busca, StatusDecisao status,
            String ordem, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 50 || busca.length() > 200) {
            throw new HistoricoInvalidoException("Filtros ou paginação inválidos.");
        }
        Sort sort = switch (ordem) {
            case "recentes" -> Sort.by(Sort.Direction.DESC, "dataAtualizacao", "id");
            case "antigas" -> Sort.by(Sort.Direction.ASC, "dataAtualizacao", "id");
            case "titulo" -> Sort.by("titulo", "id");
            default -> throw new HistoricoInvalidoException("Ordenação inválida.");
        };
        String termo = "%" + busca.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var resultado = decisoes.pesquisarHistorico(usuarioId, status, termo, PageRequest.of(pagina, tamanho, sort));
        return new HistoricoResponse(resultado.getContent().stream().map(DecisaoResumoResponse::de).toList(),
                pagina, resultado.getTotalPages(), resultado.getTotalElements(),
                decisoes.countByUsuarioIdAndStatus(usuarioId, StatusDecisao.CONCLUIDA),
                decisoes.countByUsuarioIdAndStatus(usuarioId, StatusDecisao.EM_ANDAMENTO));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id, boolean confirmar) {
        var decisao = decisoes.buscarParaAtualizar(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        if (!confirmar) throw new HistoricoInvalidoException("Confirme a exclusão da decisão.");
        // As chaves estrangeiras removem os registros dependentes com ON DELETE CASCADE.
        decisoes.delete(decisao);
        decisoes.flush();
    }
}
