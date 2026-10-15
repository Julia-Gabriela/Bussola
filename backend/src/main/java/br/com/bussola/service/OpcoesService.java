package br.com.bussola.service;

import br.com.bussola.dto.request.DefinirOpcoesRequest;
import br.com.bussola.dto.request.DefinirOpcoesRequest.DadosOpcao;
import br.com.bussola.dto.response.OpcoesResponse;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.exception.OpcoesInvalidasException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.AvaliacaoRepository;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.OpcaoRepository;
import br.com.bussola.repository.ResultadoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpcoesService {
    public static final int ETAPA_OPCOES = 3;
    public static final int ETAPA_CRITERIOS = 4;
    private final DecisaoRepository decisoes;
    private final OpcaoRepository opcoes;
    private final AvaliacaoRepository avaliacoes;
    private final ResultadoRepository resultados;

    public OpcoesService(DecisaoRepository decisoes, OpcaoRepository opcoes,
            AvaliacaoRepository avaliacoes, ResultadoRepository resultados) {
        this.decisoes = decisoes;
        this.opcoes = opcoes;
        this.avaliacoes = avaliacoes;
        this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public OpcoesResponse consultar(Long usuarioId, Long decisaoId) {
        var decisao = decisoes.findByIdAndUsuarioId(decisaoId, usuarioId)
                .orElseThrow(DecisaoNaoEncontradaException::new);
        if (decisao.getEtapaAtual() < ETAPA_OPCOES) {
            throw new EtapaInvalidaException("Conclua o brainstorming antes de definir as opções.");
        }
        return resposta(decisao, listar(decisaoId), false, false);
    }

    @Transactional
    public OpcoesResponse salvar(Long usuarioId, Long decisaoId, DefinirOpcoesRequest request) {
        return gravar(usuarioId, decisaoId, request, false);
    }

    @Transactional
    public OpcoesResponse concluir(Long usuarioId, Long decisaoId, DefinirOpcoesRequest request) {
        return gravar(usuarioId, decisaoId, request, true);
    }

    private OpcoesResponse gravar(Long usuarioId, Long decisaoId, DefinirOpcoesRequest request, boolean avancar) {
        // O bloqueio da decisão serializa criação, edição e avanço, inclusive entre sessões.
        var decisao = decisoes.buscarParaAtualizar(decisaoId, usuarioId)
                .orElseThrow(DecisaoNaoEncontradaException::new);
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO) {
            throw new EtapaInvalidaException("Esta decisão está concluída e disponível apenas para consulta.");
        }
        var existentes = listar(decisaoId);
        if (decisao.getEtapaAtual() < ETAPA_OPCOES) {
            throw new EtapaInvalidaException("Conclua o brainstorming antes de definir as opções.");
        }
        if (avancar && decisao.getEtapaAtual() == ETAPA_CRITERIOS && mesmosDados(decisao, existentes, request)) {
            return resposta(decisao, existentes, false, false);
        }
        boolean revisao = !avancar && decisao.getEtapaAtual() > ETAPA_OPCOES;
        if (decisao.getEtapaAtual() != ETAPA_OPCOES && !revisao) {
            throw new EtapaInvalidaException("As opções só podem ser alteradas na etapa de definição das opções.");
        }
        if (!decisao.getTitulo().equals(request.titulo().strip())) {
            throw new EtapaInvalidaException("O nome da decisão não pode ser alterado na definição das opções.");
        }
        if (request.opcaoA().nome().strip().equalsIgnoreCase(request.opcaoB().nome().strip())) {
            throw new OpcoesInvalidasException("Dê nomes diferentes às opções A e B para identificar cada alternativa.");
        }
        // A = menor ID, B = próximo ID. Nunca apagar/recriar opções: outras etapas referenciam esses IDs.
        var par = new ArrayList<>(existentes);
        while (par.size() < 2) {
            var opcao = new Opcao();
            opcao.setDecisao(decisao);
            par.add(opcao);
        }
        // Preserva os textos, mas uma alternativa diferente exige nova conclusão da reflexão.
        if (!mesmosDados(decisao, existentes, request)) decisao.setReflexao101010ConcluidaEm(null);
        var efeitoA = efeitoDaAlteracao(par.get(0), request.opcaoA());
        var efeitoB = efeitoDaAlteracao(par.get(1), request.opcaoB());
        preencher(par.get(0), request.opcaoA());
        preencher(par.get(1), request.opcaoB());
        if (efeitoA.pendencia() || efeitoB.pendencia()) {
            decisao.setVersaoAvaliacao(decisao.getVersaoAvaliacao() + 1);
        }
        opcoes.saveAll(par);
        decisao.setDataAtualizacao(LocalDateTime.now());
        if (avancar) decisao.setEtapaAtual(ETAPA_CRITERIOS);
        decisoes.save(decisao);
        return resposta(decisao, par, efeitoA.recomendacao(), efeitoB.recomendacao());
    }

    /**
     * Decide se a mudança de uma alternativa torna as notas pendentes.
     * <p>
     * A comparação do nome usa o mesmo {@code strip} já gravado na opção.
     * Espaços nas pontas não contam como alteração. Diferença de maiúsculas
     * conta, porque o nome persistido distingue caixa. A pendência só nasce
     * quando essa alternativa já tem ao menos uma nota. Mudar só a descrição
     * não cria pendência; devolve uma recomendação transitória. Nome e
     * descrição juntos seguem a regra do nome. Reenviar os mesmos valores
     * não liga nem apaga a pendência.
     *
     * @param opcao alternativa já associada à decisão; pode ainda não ter ID
     * @param dados nome e descrição recebidos, antes de gravar
     * @return pendência quando o nome mudou e já havia nota; recomendação
     *         quando só a descrição mudou e já havia nota
     */
    private Efeito efeitoDaAlteracao(Opcao opcao, DadosOpcao dados) {
        if (opcao.getId() == null || opcao.getNome() == null) return new Efeito(false, false);
        boolean nomeMudou = !opcao.getNome().equals(dados.nome().strip());
        boolean descricaoMudou = !Objects.equals(opcao.getDescricao(), descricao(dados.descricao()));
        if (!nomeMudou && !descricaoMudou) return new Efeito(false, false);
        if (!avaliacoes.existsByOpcaoId(opcao.getId())) return new Efeito(false, false);
        if (nomeMudou) {
            opcao.setAvaliacaoPendente(true);
            sinalizarResultado(opcao);
            return new Efeito(true, false);
        }
        return new Efeito(false, true);
    }

    private void sinalizarResultado(Opcao opcao) {
        resultados.findByOpcaoId(opcao.getId()).ifPresent(resultado -> {
            if (!resultado.isRequerAtualizacao()) {
                resultado.setRequerAtualizacao(true);
                resultados.save(resultado);
            }
        });
    }

    private record Efeito(boolean pendencia, boolean recomendacao) {}

    private List<Opcao> listar(Long decisaoId) {
        var lista = opcoes.findByDecisaoIdOrderByIdAsc(decisaoId);
        if (lista.size() > 2) {
            throw new EtapaInvalidaException("Esta decisão tem mais de duas opções registradas. Confira os dados antes de continuar.");
        }
        return lista;
    }

    private boolean mesmosDados(Decisao decisao, List<Opcao> lista, DefinirOpcoesRequest request) {
        return lista.size() == 2 && decisao.getTitulo().equals(request.titulo().strip())
                && igual(lista.get(0), request.opcaoA()) && igual(lista.get(1), request.opcaoB());
    }

    private boolean igual(Opcao opcao, DadosOpcao dados) {
        return opcao.getNome().equals(dados.nome().strip())
                && Objects.equals(opcao.getDescricao(), descricao(dados.descricao()));
    }

    private void preencher(Opcao opcao, DadosOpcao dados) {
        opcao.setNome(dados.nome().strip());
        opcao.setDescricao(descricao(dados.descricao()));
    }

    private String descricao(String texto) { return texto == null || texto.isBlank() ? null : texto.strip(); }

    private OpcoesResponse resposta(Decisao decisao, List<Opcao> lista, boolean recomendacaoA, boolean recomendacaoB) {
        return new OpcoesResponse(decisao.getId(), decisao.getTitulo(), decisao.getEtapaAtual(), decisao.getStatus(),
                lista.isEmpty() ? null : OpcoesResponse.DadosOpcao.de(lista.get(0)),
                lista.size() < 2 ? null : OpcoesResponse.DadosOpcao.de(lista.get(1)),
                !lista.isEmpty() && lista.get(0).isAvaliacaoPendente(),
                lista.size() > 1 && lista.get(1).isAvaliacaoPendente(),
                recomendacaoA, recomendacaoB, decisao.getVersaoAvaliacao());
    }
}
