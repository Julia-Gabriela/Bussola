package br.com.bussola.service;

import br.com.bussola.dto.request.SalvarReflexoes101010Request;
import br.com.bussola.dto.request.SalvarReflexoes101010Request.Reflexao;
import br.com.bussola.dto.response.Reflexoes101010Response;
import br.com.bussola.dto.response.Reflexoes101010Response.Alternativa;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.exception.ReflexoesInvalidasException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.entity.Reflexao101010;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.OpcaoRepository;
import br.com.bussola.repository.Reflexao101010Repository;
import br.com.bussola.repository.ResultadoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Reflexoes101010Service {
    private final DecisaoRepository decisoes;
    private final OpcaoRepository opcoes;
    private final Reflexao101010Repository reflexoes;
    private final ResultadoRepository resultados;

    public Reflexoes101010Service(DecisaoRepository decisoes, OpcaoRepository opcoes,
            Reflexao101010Repository reflexoes, ResultadoRepository resultados) {
        this.decisoes = decisoes; this.opcoes = opcoes;
        this.reflexoes = reflexoes; this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public Reflexoes101010Response consultar(Long usuarioId, Long id) {
        var decisao = decisoes.findByIdAndUsuarioId(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        exigirAnalise(decisao);
        return resposta(decisao, par(id), reflexoes.findByOpcaoDecisaoId(id));
    }

    @Transactional
    public Reflexoes101010Response salvar(Long usuarioId, Long id, SalvarReflexoes101010Request request) {
        return gravar(usuarioId, id, request, false);
    }

    @Transactional
    public Reflexoes101010Response concluir(Long usuarioId, Long id, SalvarReflexoes101010Request request) {
        return gravar(usuarioId, id, request, true);
    }

    private Reflexoes101010Response gravar(Long usuarioId, Long id, SalvarReflexoes101010Request request, boolean concluir) {
        // O mesmo lock das outras etapas serializa salvamento, revisão e conclusão.
        var decisao = decisoes.buscarParaAtualizar(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        exigirAnalise(decisao);
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO)
            throw new EtapaInvalidaException("Esta decisão está concluída e disponível apenas para consulta.");
        var par = par(id);
        validar(request, par, concluir);
        var recebidas = List.of(request.opcaoA(), request.opcaoB());
        var salvas = new ArrayList<>(reflexoes.findByOpcaoDecisaoId(id));
        var alteradas = new ArrayList<Reflexao101010>();
        for (int i = 0; i < par.size(); i++) {
            var opcao = par.get(i);
            var dados = recebidas.get(i);
            var atual = buscar(salvas, opcao);
            if (iguais(atual, dados)) continue;
            if (atual == null) {
                atual = new Reflexao101010(); atual.setOpcao(opcao); salvas.add(atual);
            }
            atual.setCurtoPrazo(texto(dados.curtoPrazo()));
            atual.setMedioPrazo(texto(dados.medioPrazo()));
            atual.setLongoPrazo(texto(dados.longoPrazo()));
            alteradas.add(atual);
            resultados.findByOpcaoId(opcao.getId()).ifPresent(resultado -> {
                resultado.setRequerAtualizacao(true); resultados.save(resultado);
            });
        }
        if (!alteradas.isEmpty()) reflexoes.saveAll(alteradas);
        boolean mudouConclusao = concluir && decisao.getReflexao101010ConcluidaEm() == null
                || !concluir && !alteradas.isEmpty() && decisao.getReflexao101010ConcluidaEm() != null;
        if (!alteradas.isEmpty() || mudouConclusao) {
            var agora = LocalDateTime.now();
            decisao.setReflexao101010ConcluidaEm(concluir ? agora : null);
            decisao.setDataAtualizacao(agora);
            decisoes.save(decisao);
        }
        // Reenvios idênticos preservam IDs e datas; a decisão permanece na etapa 6.
        return resposta(decisao, par, salvas);
    }

    private void validar(SalvarReflexoes101010Request request, List<Opcao> par, boolean concluir) {
        if (request == null || request.opcaoA() == null || request.opcaoB() == null)
            throw new ReflexoesInvalidasException("Envie as reflexões das duas opções.");
        var recebidas = List.of(request.opcaoA(), request.opcaoB());
        for (int i = 0; i < 2; i++) {
            var item = recebidas.get(i);
            if (!Objects.equals(par.get(i).getId(), item.opcaoId()))
                throw new ReflexoesInvalidasException("As reflexões devem corresponder às opções A e B desta decisão.");
            for (var valor : new String[]{item.curtoPrazo(), item.medioPrazo(), item.longoPrazo()}) {
                if (valor != null && valor.length() > 16000)
                    throw new ReflexoesInvalidasException("Cada reflexão pode ter até 16.000 caracteres.");
                if (concluir && texto(valor) == null)
                    throw new ReflexoesInvalidasException("Preencha os três prazos das duas opções antes de concluir a técnica.");
            }
        }
    }

    private void exigirAnalise(Decisao decisao) {
        if (decisao.getEtapaAtual() != TecnicasAnaliseService.ETAPA_ANALISE)
            throw new EtapaInvalidaException("Conclua as etapas anteriores antes de acessar o método 10–10–10.");
    }

    private List<Opcao> par(Long id) {
        var par = opcoes.findByDecisaoIdOrderByIdAsc(id);
        if (par.size() != 2) throw new EtapaInvalidaException("A decisão precisa ter suas duas alternativas definidas.");
        return par;
    }

    private Reflexao101010 buscar(List<Reflexao101010> lista, Opcao opcao) {
        return lista.stream().filter(r -> r.getOpcao().getId().equals(opcao.getId())).findFirst().orElse(null);
    }

    private boolean iguais(Reflexao101010 atual, Reflexao dados) {
        return Objects.equals(atual == null ? null : atual.getCurtoPrazo(), texto(dados.curtoPrazo()))
                && Objects.equals(atual == null ? null : atual.getMedioPrazo(), texto(dados.medioPrazo()))
                && Objects.equals(atual == null ? null : atual.getLongoPrazo(), texto(dados.longoPrazo()));
    }

    private String texto(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }

    private Reflexoes101010Response resposta(Decisao decisao, List<Opcao> par, List<Reflexao101010> salvas) {
        var data = decisao.getReflexao101010ConcluidaEm();
        return new Reflexoes101010Response(decisao.getId(), decisao.getTitulo(), decisao.getEtapaAtual(),
                decisao.getStatus(), data != null, data, alternativa(par.get(0), salvas), alternativa(par.get(1), salvas));
    }

    private Alternativa alternativa(Opcao opcao, List<Reflexao101010> salvas) {
        var r = buscar(salvas, opcao);
        return new Alternativa(opcao.getId(), opcao.getNome(), opcao.getDescricao(), r == null ? null : r.getId(),
                r == null ? null : r.getCurtoPrazo(), r == null ? null : r.getMedioPrazo(), r == null ? null : r.getLongoPrazo());
    }
}
