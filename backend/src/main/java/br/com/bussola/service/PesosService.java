package br.com.bussola.service;

import br.com.bussola.dto.request.DefinirPesosRequest;
import br.com.bussola.dto.response.PesosResponse;
import br.com.bussola.exception.CriteriosInvalidosException;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.model.entity.Criterio;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.CriterioRepository;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.OpcaoRepository;
import br.com.bussola.repository.ResultadoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PesosService {
    public static final int ETAPA_PESOS = 5;
    public static final int ETAPA_ANALISE = 6;
    private final DecisaoRepository decisoes;
    private final CriterioRepository criterios;
    private final OpcaoRepository opcoes;
    private final ResultadoRepository resultados;

    public PesosService(DecisaoRepository decisoes, CriterioRepository criterios,
            OpcaoRepository opcoes, ResultadoRepository resultados) {
        this.decisoes = decisoes;
        this.criterios = criterios;
        this.opcoes = opcoes;
        this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public PesosResponse consultar(Long usuarioId, Long decisaoId) {
        var decisao = decisoes.findByIdAndUsuarioId(decisaoId, usuarioId)
                .orElseThrow(DecisaoNaoEncontradaException::new);
        exigirEtapaPesosAlcancada(decisao);
        return resposta(decisao, carregarCriterios(decisaoId));
    }

    @Transactional
    public PesosResponse salvar(Long usuarioId, Long decisaoId, DefinirPesosRequest request) {
        var decisao = bloquear(usuarioId, decisaoId);
        exigirEmAndamento(decisao);
        exigirEtapaPesosAlcancada(decisao);
        var lista = carregarCriterios(decisaoId);
        if (aplicar(lista, request)) {
            criterios.saveAll(lista);
            invalidarResultados(decisaoId);
            decisao.setDataAtualizacao(LocalDateTime.now());
            decisoes.save(decisao);
        }
        return resposta(decisao, lista);
    }

    @Transactional
    public PesosResponse concluir(Long usuarioId, Long decisaoId, DefinirPesosRequest request) {
        var decisao = bloquear(usuarioId, decisaoId);
        exigirEmAndamento(decisao);
        exigirEtapaPesosAlcancada(decisao);
        var lista = carregarCriterios(decisaoId);
        if (decisao.getEtapaAtual() == ETAPA_ANALISE) {
            if (!mesmosPesos(lista, request))
                throw new EtapaInvalidaException("A etapa de Pesos já foi concluída. Reabra-a para salvar alterações.");
            return resposta(decisao, lista);
        }
        if (aplicar(lista, request)) {
            criterios.saveAll(lista);
            invalidarResultados(decisaoId);
        }
        decisao.setEtapaAtual(ETAPA_ANALISE);
        decisao.setDataAtualizacao(LocalDateTime.now());
        decisoes.save(decisao);
        return resposta(decisao, lista);
    }

    private List<Criterio> carregarCriterios(Long decisaoId) {
        var lista = criterios.findByDecisaoIdOrderByIdAsc(decisaoId);
        if (lista.size() < 5 || lista.size() > 20 || lista.stream().anyMatch(c -> c.getId() == null))
            throw new EtapaInvalidaException("Os critérios não estão prontos para receber pesos. Recarregue a decisão.");
        return lista;
    }

    private boolean aplicar(List<Criterio> existentes, DefinirPesosRequest request) {
        var recebidos = validar(existentes, request);
        boolean mudou = false;
        for (var criterio : existentes) {
            int peso = recebidos.get(criterio.getId());
            if (criterio.getPeso() != peso) {
                criterio.setPeso(peso);
                mudou = true;
            }
        }
        return mudou;
    }

    private Map<Long, Integer> validar(List<Criterio> existentes, DefinirPesosRequest request) {
        if (request == null || request.criterios() == null || request.criterios().size() != existentes.size()
                || existentes.size() < 5 || existentes.size() > 20)
            throw new CriteriosInvalidosException("Envie um peso para cada critério da decisão.");
        Set<Long> idsValidos = new HashSet<>();
        existentes.forEach(criterio -> idsValidos.add(criterio.getId()));
        var recebidos = new HashMap<Long, Integer>();
        for (var item : request.criterios()) {
            if (item == null || item.id() == null || item.peso() == null || item.peso() < 1 || item.peso() > 5)
                throw new CriteriosInvalidosException("Cada peso deve ser um número inteiro de 1 a 5.");
            if (!idsValidos.contains(item.id()) || recebidos.putIfAbsent(item.id(), item.peso()) != null)
                throw new CriteriosInvalidosException("Os critérios enviados não correspondem a esta decisão.");
        }
        if (recebidos.size() != existentes.size())
            throw new CriteriosInvalidosException("Envie um peso para cada critério da decisão.");
        return recebidos;
    }

    private boolean mesmosPesos(List<Criterio> existentes, DefinirPesosRequest request) {
        var recebidos = validar(existentes, request);
        return existentes.stream().allMatch(c -> c.getPeso().equals(recebidos.get(c.getId())));
    }

    private void invalidarResultados(Long decisaoId) {
        opcoes.findByDecisaoIdOrderByIdAsc(decisaoId).forEach(opcao ->
                resultados.findByOpcaoId(opcao.getId()).ifPresent(resultado -> {
                    resultado.setRequerAtualizacao(true);
                    resultados.save(resultado);
                }));
    }

    private Decisao bloquear(Long usuarioId, Long decisaoId) {
        return decisoes.buscarParaAtualizar(decisaoId, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
    }
    private void exigirEmAndamento(Decisao decisao) {
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO)
            throw new EtapaInvalidaException("Esta decisão está concluída e disponível apenas para consulta.");
    }
    private void exigirEtapaPesosAlcancada(Decisao decisao) {
        if (decisao.getEtapaAtual() < ETAPA_PESOS)
            throw new EtapaInvalidaException("Conclua a avaliação dos critérios antes de definir os pesos.");
    }
    private PesosResponse resposta(Decisao decisao, List<Criterio> lista) {
        var itens = new ArrayList<PesosResponse.Item>();
        for (var criterio : lista)
            itens.add(new PesosResponse.Item(criterio.getId(), criterio.getNome(), criterio.getDescricao(),
                    criterio.isPersonalizado(), criterio.getPeso()));
        return new PesosResponse(decisao.getId(), decisao.getTitulo(), decisao.getEtapaAtual(),
                decisao.getStatus(), List.copyOf(itens));
    }
}
