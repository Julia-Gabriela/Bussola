package br.com.bussola.service;

import br.com.bussola.dto.request.IdeiaRequest;
import br.com.bussola.dto.request.NovaDecisaoRequest;
import br.com.bussola.dto.response.DecisaoResponse;
import br.com.bussola.dto.response.IdeiaResponse;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.exception.IdeiaNaoEncontradaException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.IdeiaBrainstorming;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.IdeiaBrainstormingRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FluxoDecisaoService {
    private static final int CONTEXTUALIZACAO = 1;
    private static final int BRAINSTORMING = 2;
    private static final int OPCOES = 3;
    private final DecisaoRepository decisoes;
    private final IdeiaBrainstormingRepository ideias;

    public FluxoDecisaoService(DecisaoRepository decisoes, IdeiaBrainstormingRepository ideias) {
        this.decisoes = decisoes;
        this.ideias = ideias;
    }

    @Transactional
    public DecisaoResponse salvarContexto(Long usuarioId, Long id, NovaDecisaoRequest request) {
        var decisao = bloquear(usuarioId, id);
        exigirEtapa(decisao, CONTEXTUALIZACAO);
        decisao.setTitulo(request.titulo().strip());
        decisao.setContexto(request.contexto() == null || request.contexto().isBlank()
                ? null : request.contexto().strip());
        atualizarData(decisao);
        return DecisaoResponse.de(decisoes.saveAndFlush(decisao));
    }

    @Transactional
    public DecisaoResponse concluirContexto(Long usuarioId, Long id) {
        var decisao = bloquear(usuarioId, id);
        exigirEmAndamento(decisao);
        if (decisao.getEtapaAtual() == BRAINSTORMING) return DecisaoResponse.de(decisao);
        exigirEtapa(decisao, CONTEXTUALIZACAO);
        if (decisao.getTitulo() == null || decisao.getTitulo().isBlank()) {
            throw new EtapaInvalidaException("Descreva sua decisão antes de continuar.");
        }
        decisao.setEtapaAtual(BRAINSTORMING);
        atualizarData(decisao);
        return DecisaoResponse.de(decisoes.saveAndFlush(decisao));
    }

    @Transactional(readOnly = true)
    public List<IdeiaResponse> listar(Long usuarioId, Long id) {
        var decisao = decisoes.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(DecisaoNaoEncontradaException::new);
        if (decisao.getEtapaAtual() < BRAINSTORMING) {
            throw new EtapaInvalidaException("Conclua a contextualização antes de acessar o brainstorming.");
        }
        return ideias.findByDecisaoIdOrderByIdAsc(id).stream().map(IdeiaResponse::de).toList();
    }

    @Transactional
    public IdeiaResponse adicionar(Long usuarioId, Long id, IdeiaRequest request) {
        var decisao = bloquear(usuarioId, id);
        exigirEtapa(decisao, BRAINSTORMING);
        var ideia = new IdeiaBrainstorming();
        ideia.setDecisao(decisao);
        preencher(ideia, request);
        atualizarData(decisao);
        return IdeiaResponse.de(ideias.save(ideia));
    }

    @Transactional
    public IdeiaResponse editar(Long usuarioId, Long id, Long ideiaId, IdeiaRequest request) {
        var decisao = bloquear(usuarioId, id);
        exigirEtapa(decisao, BRAINSTORMING);
        var ideia = buscarIdeia(id, ideiaId);
        preencher(ideia, request);
        atualizarData(decisao);
        return IdeiaResponse.de(ideias.save(ideia));
    }

    @Transactional
    public void excluir(Long usuarioId, Long id, Long ideiaId) {
        var decisao = bloquear(usuarioId, id);
        exigirEtapa(decisao, BRAINSTORMING);
        ideias.delete(buscarIdeia(id, ideiaId));
        atualizarData(decisao);
    }

    @Transactional
    public DecisaoResponse concluirBrainstorming(Long usuarioId, Long id) {
        var decisao = bloquear(usuarioId, id);
        exigirEmAndamento(decisao);
        if (decisao.getEtapaAtual() == OPCOES) return DecisaoResponse.de(decisao);
        exigirEtapa(decisao, BRAINSTORMING);
        decisao.setEtapaAtual(OPCOES);
        atualizarData(decisao);
        return DecisaoResponse.de(decisoes.saveAndFlush(decisao));
    }

    private Decisao bloquear(Long usuarioId, Long id) {
        // Edição e avanço da mesma decisão compartilham o bloqueio, inclusive nas ideias.
        return decisoes.buscarParaAtualizar(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
    }

    private IdeiaBrainstorming buscarIdeia(Long id, Long ideiaId) {
        return ideias.findByIdAndDecisaoId(ideiaId, id).orElseThrow(IdeiaNaoEncontradaException::new);
    }

    private void preencher(IdeiaBrainstorming ideia, IdeiaRequest request) {
        ideia.setConteudo(request.conteudo().strip());
        ideia.setCategoria(request.categoria().name());
    }

    private void exigirEmAndamento(Decisao decisao) {
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO) {
            throw new EtapaInvalidaException("Uma decisão concluída não pode ser alterada.");
        }
    }

    private void exigirEtapa(Decisao decisao, int etapa) {
        exigirEmAndamento(decisao);
        if (decisao.getEtapaAtual() != etapa) {
            throw new EtapaInvalidaException("Esta operação não está disponível na etapa atual. Reabra a decisão pela Home.");
        }
    }

    private void atualizarData(Decisao decisao) {
        decisao.setDataAtualizacao(LocalDateTime.now());
    }
}
