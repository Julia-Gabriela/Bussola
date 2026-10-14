package br.com.bussola.service;

import br.com.bussola.dto.request.AvaliarCriteriosRequest;
import br.com.bussola.dto.request.AvaliarCriteriosRequest.NotaCriterio;
import br.com.bussola.dto.request.ConfirmarAvaliacaoRequest;
import br.com.bussola.dto.response.CriteriosResponse;
import br.com.bussola.dto.response.CriteriosResponse.Item;
import br.com.bussola.dto.response.OpcoesResponse;
import br.com.bussola.exception.*;
import br.com.bussola.model.entity.*;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CriteriosService {
    public static final int ETAPA_CRITERIOS = 4;
    public static final int ETAPA_PESOS = 5;
    private final DecisaoRepository decisoes;
    private final OpcaoRepository opcoes;
    private final CriterioRepository criterios;
    private final AvaliacaoRepository avaliacoes;
    private final ResultadoRepository resultados;

    public CriteriosService(DecisaoRepository decisoes, OpcaoRepository opcoes,
            CriterioRepository criterios, AvaliacaoRepository avaliacoes, ResultadoRepository resultados) {
        this.decisoes = decisoes; this.opcoes = opcoes;
        this.criterios = criterios; this.avaliacoes = avaliacoes;
        this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public CriteriosResponse consultar(Long usuarioId, Long decisaoId) {
        var decisao = decisoes.findByIdAndUsuarioId(decisaoId, usuarioId)
                .orElseThrow(DecisaoNaoEncontradaException::new);
        if (decisao.getEtapaAtual() < ETAPA_CRITERIOS)
            throw new EtapaInvalidaException("Conclua a definição das opções antes de avaliar os critérios.");
        return resposta(decisao, par(decisaoId), criterios.findByDecisaoIdOrderByIdAsc(decisaoId),
                avaliacoes.findByCriterioDecisaoId(decisaoId));
    }

    @Transactional
    public CriteriosResponse salvar(Long usuarioId, Long decisaoId, AvaliarCriteriosRequest request) {
        return gravar(usuarioId, decisaoId, request, false);
    }

    @Transactional
    public CriteriosResponse concluir(Long usuarioId, Long decisaoId, AvaliarCriteriosRequest request) {
        return gravar(usuarioId, decisaoId, request, true);
    }

    /**
     * Confirma as notas de uma alternativa sem alterá-las e sem avançar a etapa.
     * <p>
     * Só o dono da decisão em andamento, a partir da etapa de critérios, pode
     * confirmar. A versão enviada precisa ser a última vista pela tela: se as
     * notas ou o nome da alternativa mudaram depois, a confirmação é recusada.
     * Confirmar A não mexe na pendência de B. Salvar notas não passa por este
     * método. A confirmação não apaga um resultado já calculado.
     *
     * @param usuarioId dono exigido da decisão
     * @param decisaoId decisão cujas notas serão confirmadas
     * @param request alternativa {@code A} ou {@code B} e a versão carregada
     * @return estado atual das notas, com a pendência daquela alternativa desligada
     */
    @Transactional
    public CriteriosResponse confirmar(Long usuarioId, Long decisaoId, ConfirmarAvaliacaoRequest request) {
        var decisao = decisoes.buscarParaAtualizar(decisaoId, usuarioId)
                .orElseThrow(DecisaoNaoEncontradaException::new);
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO)
            throw new EtapaInvalidaException("Esta decisão está concluída e disponível apenas para consulta.");
        if (decisao.getEtapaAtual() < ETAPA_CRITERIOS)
            throw new EtapaInvalidaException("Conclua a definição das opções antes de confirmar as avaliações.");
        if (decisao.getVersaoAvaliacao() != request.versaoAvaliacao())
            throw new EtapaInvalidaException("As avaliações foram atualizadas. Recarregue a tela antes de confirmar.");
        var par = par(decisaoId);
        var opcao = "A".equals(request.alternativa()) ? par.get(0) : par.get(1);
        if (opcao.isAvaliacaoPendente()) {
            opcao.setAvaliacaoPendente(false);
            opcoes.save(opcao);
        }
        return resposta(decisao, par, criterios.findByDecisaoIdOrderByIdAsc(decisaoId),
                avaliacoes.findByCriterioDecisaoId(decisaoId));
    }

    private CriteriosResponse gravar(Long usuarioId, Long decisaoId, AvaliarCriteriosRequest request, boolean avancar) {
        // Todas as escritas deste fluxo bloqueiam a decisão; reenvios preservam critérios, notas e IDs.
        var decisao = decisoes.buscarParaAtualizar(decisaoId, usuarioId)
                .orElseThrow(DecisaoNaoEncontradaException::new);
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO)
            throw new EtapaInvalidaException("Esta decisão está concluída e disponível apenas para consulta.");
        var par = par(decisaoId);
        var existentes = criterios.findByDecisaoIdOrderByIdAsc(decisaoId);
        var notas = avaliacoes.findByCriterioDecisaoId(decisaoId);
        var solicitados = validar(request, avancar);
        if (decisao.getEtapaAtual() < ETAPA_CRITERIOS)
            throw new EtapaInvalidaException("Conclua a definição das opções antes de avaliar os critérios.");
        if (avancar && decisao.getEtapaAtual() == ETAPA_PESOS
                && mesmosDados(resposta(decisao, par, existentes, notas), solicitados))
            return resposta(decisao, par, existentes, notas);
        boolean revisao = !avancar && decisao.getEtapaAtual() > ETAPA_CRITERIOS;
        if (decisao.getEtapaAtual() != ETAPA_CRITERIOS && !revisao)
            throw new EtapaInvalidaException("Os critérios só podem ser alterados na etapa de avaliação dos critérios.");
        if (avancar && (par.get(0).isAvaliacaoPendente() || par.get(1).isAvaliacaoPendente()))
            throw new EtapaInvalidaException("Confirme as notas da alternativa alterada antes de continuar a análise.");
        var porNome = porNome(existentes);
        var salvos = new ArrayList<Criterio>();
        var mantidas = new ArrayList<Avaliacao>();
        boolean notasAlteradas = false;
        // Nomes identificam critérios dentro da decisão. Não há renomeação nesta etapa;
        // editar notas ou repetir um envio não recria critérios usados pela etapa de Pesos.
        for (var entrada : solicitados.entrySet()) {
            var dados = entrada.getValue();
            var padrao = CatalogoCriterios.buscar(dados.nome());
            var criterio = porNome.getOrDefault(entrada.getKey(), new Criterio());
            criterio.setDecisao(decisao);
            criterio.setNome(padrao == null ? dados.nome().strip() : padrao.nome());
            criterio.setDescricao(padrao == null ? descricao(dados.descricao()) : padrao.descricao());
            criterio.setPersonalizado(padrao == null);
            salvos.add(criterio);
        }
        criterios.saveAll(salvos);
        for (var criterio : salvos) {
            var dados = solicitados.get(CatalogoCriterios.chave(criterio.getNome()));
            notasAlteradas |= guardarNota(criterio, par.get(0), dados.notaA(), notas, mantidas);
            notasAlteradas |= guardarNota(criterio, par.get(1), dados.notaB(), notas, mantidas);
        }
        // Excluir apenas as notas retiradas/limpas, preservando IDs das que continuam.
        var removidas = notas.stream().filter(n -> !mantidas.contains(n)).toList();
        if (!removidas.isEmpty()) notasAlteradas = true;
        avaliacoes.deleteAll(removidas);
        avaliacoes.saveAll(mantidas);
        if (notasAlteradas) {
            decisao.setVersaoAvaliacao(decisao.getVersaoAvaliacao() + 1);
            mantidas.stream().map(Avaliacao::getOpcao).distinct().forEach(this::sinalizarResultado);
            removidas.stream().map(Avaliacao::getOpcao).distinct().forEach(this::sinalizarResultado);
        }
        criterios.deleteAll(existentes.stream().filter(c -> !solicitados.containsKey(CatalogoCriterios.chave(c.getNome()))).toList());
        decisao.setDataAtualizacao(LocalDateTime.now());
        if (avancar) decisao.setEtapaAtual(ETAPA_PESOS);
        decisoes.save(decisao);
        return resposta(decisao, par, salvos, mantidas);
    }

    private Map<String, NotaCriterio> validar(AvaliarCriteriosRequest request, boolean completar) {
        if (request == null || request.criterios() == null || request.criterios().size() < 5 || request.criterios().size() > 20)
            throw new CriteriosInvalidosException("Mantenha os cinco critérios fixos e até quinze personalizados.");
        var dados = new LinkedHashMap<String, NotaCriterio>();
        for (var item : request.criterios()) {
            if (item == null || item.nome() == null || item.nome().isBlank() || item.nome().length() > 200
                    || (item.descricao() != null && item.descricao().length() > 1000))
                throw new CriteriosInvalidosException("Informe um nome de até 200 caracteres e uma descrição de até 1.000 caracteres.");
            if (dados.putIfAbsent(CatalogoCriterios.chave(item.nome()), item) != null)
                throw new CriteriosInvalidosException("Use nomes diferentes para cada critério.");
            for (var nota : Arrays.asList(item.notaA(), item.notaB())) {
                if (nota != null && (nota < 1 || nota > 5))
                    throw new CriteriosInvalidosException("As notas devem ser números inteiros de 1 a 5.");
                if (completar && nota == null)
                    throw new CriteriosInvalidosException("Avalie todos os critérios das duas opções antes de continuar.");
            }
        }
        if (CatalogoCriterios.PADROES.stream().anyMatch(p -> !dados.containsKey(CatalogoCriterios.chave(p.nome()))))
            throw new CriteriosInvalidosException("Os cinco critérios fixos não podem ser removidos.");
        return dados;
    }

    private List<Opcao> par(Long decisaoId) {
        var par = opcoes.findByDecisaoIdOrderByIdAsc(decisaoId);
        if (par.size() != 2) throw new EtapaInvalidaException("A decisão precisa ter exatamente duas opções salvas.");
        return par;
    }

    private Map<String, Criterio> porNome(List<Criterio> lista) {
        var mapa = new LinkedHashMap<String, Criterio>();
        for (var criterio : lista)
            if (mapa.putIfAbsent(CatalogoCriterios.chave(criterio.getNome()), criterio) != null)
                throw new EtapaInvalidaException("Há critérios com nomes repetidos nesta decisão. Confira os dados antes de continuar.");
        return mapa;
    }

    private boolean guardarNota(Criterio criterio, Opcao opcao, Integer valor,
            List<Avaliacao> existentes, List<Avaliacao> mantidas) {
        if (valor == null) return false;
        var nota = existentes.stream().filter(n -> Objects.equals(n.getCriterio().getId(), criterio.getId())
                && Objects.equals(n.getOpcao().getId(), opcao.getId())).findFirst().orElseGet(Avaliacao::new);
        boolean mudou = nota.getId() == null || !Objects.equals(nota.getValor(), valor);
        nota.setCriterio(criterio); nota.setOpcao(opcao); nota.setValor(valor); mantidas.add(nota);
        return mudou;
    }

    private void sinalizarResultado(Opcao opcao) {
        if (opcao.getId() == null) return;
        resultados.findByOpcaoId(opcao.getId()).ifPresent(resultado -> {
            if (!resultado.isRequerAtualizacao()) {
                resultado.setRequerAtualizacao(true);
                resultados.save(resultado);
            }
        });
    }

    private CriteriosResponse resposta(Decisao decisao, List<Opcao> par, List<Criterio> lista, List<Avaliacao> notas) {
        var mapa = porNome(lista);
        var itens = new ArrayList<Item>();
        for (var padrao : CatalogoCriterios.PADROES) {
            var salvo = mapa.remove(CatalogoCriterios.chave(padrao.nome()));
            itens.add(item(salvo, padrao, par, notas));
        }
        for (var salvo : mapa.values()) itens.add(item(salvo, null, par, notas));
        return new CriteriosResponse(decisao.getId(), decisao.getTitulo(), decisao.getEtapaAtual(), decisao.getStatus(),
                OpcoesResponse.DadosOpcao.de(par.get(0)), OpcoesResponse.DadosOpcao.de(par.get(1)), List.copyOf(itens),
                par.get(0).isAvaliacaoPendente(), par.get(1).isAvaliacaoPendente(), decisao.getVersaoAvaliacao());
    }

    private Item item(Criterio salvo, CatalogoCriterios.Padrao padrao, List<Opcao> par, List<Avaliacao> notas) {
        return new Item(salvo == null ? null : salvo.getId(), padrao == null ? "personalizado" : padrao.codigo(),
                padrao == null ? salvo.getNome() : padrao.nome(), padrao == null ? salvo.getDescricao() : padrao.descricao(),
                padrao == null, padrao == null ? "MAIOR_MELHOR" : padrao.sentido(), salvo == null ? 1 : salvo.getPeso(),
                valor(salvo, par.get(0), notas), valor(salvo, par.get(1), notas));
    }

    private Integer valor(Criterio criterio, Opcao opcao, List<Avaliacao> notas) {
        if (criterio == null) return null;
        return notas.stream().filter(n -> Objects.equals(n.getCriterio().getId(), criterio.getId())
                && Objects.equals(n.getOpcao().getId(), opcao.getId())).map(Avaliacao::getValor).findFirst().orElse(null);
    }

    private boolean mesmosDados(CriteriosResponse resposta, Map<String, NotaCriterio> dados) {
        if (resposta.criterios().size() != dados.size()) return false;
        return resposta.criterios().stream().allMatch(item -> {
            var recebido = dados.get(CatalogoCriterios.chave(item.nome()));
            return recebido != null && Objects.equals(item.notaA(), recebido.notaA())
                    && Objects.equals(item.notaB(), recebido.notaB())
                    && (!item.personalizado() || Objects.equals(descricao(item.descricao()), descricao(recebido.descricao())));
        });
    }

    private String descricao(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }
}
