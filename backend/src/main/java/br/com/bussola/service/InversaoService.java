package br.com.bussola.service;

import br.com.bussola.dto.request.SalvarInversaoRequest;
import br.com.bussola.dto.response.InversaoResponse;
import br.com.bussola.dto.response.InversaoResponse.Alternativa;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.exception.ReflexoesInvalidasException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.entity.Inversao;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.OpcaoRepository;
import br.com.bussola.repository.InversaoRepository;
import br.com.bussola.repository.ResultadoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InversaoService {
    // Cada perspectiva usa uma linha da tabela existente; textos livres antigos são preservados.
    private static final String FRACASSOS = "FRACASSOS";
    private static final String PREVENCOES = "PREVENCOES";
    private final DecisaoRepository decisoes;
    private final OpcaoRepository opcoes;
    private final InversaoRepository inversoes;
    private final ResultadoRepository resultados;

    public InversaoService(DecisaoRepository decisoes, OpcaoRepository opcoes,
            InversaoRepository inversoes, ResultadoRepository resultados) {
        this.decisoes = decisoes;
        this.opcoes = opcoes;
        this.inversoes = inversoes;
        this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public InversaoResponse consultar(Long usuarioId, Long id) {
        var decisao = decisoes.findByIdAndUsuarioId(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        exigirAnalise(decisao);
        return resposta(decisao, par(id), inversoes.findByOpcaoDecisaoIdOrderByIdAsc(id));
    }

    @Transactional
    public InversaoResponse salvar(Long usuarioId, Long id, SalvarInversaoRequest request) {
        return gravar(usuarioId, id, request, false);
    }

    @Transactional
    public InversaoResponse concluir(Long usuarioId, Long id, SalvarInversaoRequest request) {
        return gravar(usuarioId, id, request, true);
    }

    private InversaoResponse gravar(Long usuarioId, Long id, SalvarInversaoRequest request, boolean concluir) {
        // Compartilha o lock das etapas: não cria duplicatas em salvamentos simultâneos.
        var decisao = decisoes.buscarParaAtualizar(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        exigirAnalise(decisao);
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO)
            throw new EtapaInvalidaException("Esta decisão está concluída e disponível apenas para consulta.");
        var par = par(id);
        validar(request, par, concluir);
        var salvas = new ArrayList<>(inversoes.findByOpcaoDecisaoIdOrderByIdAsc(id));
        // Verifica dados legados inconsistentes antes de alterar qualquer entidade.
        for (var opcao : par) {
            buscar(salvas, opcao, FRACASSOS);
            buscar(salvas, opcao, PREVENCOES);
        }
        var recebidas = List.of(request.opcaoA(), request.opcaoB());
        var alteradas = new ArrayList<Inversao>();
        for (int i = 0; i < 2; i++) {
            var opcao = par.get(i);
            var dados = recebidas.get(i);
            boolean mudou = atualizar(salvas, alteradas, opcao, FRACASSOS, dados.fracassos());
            mudou |= atualizar(salvas, alteradas, opcao, PREVENCOES, dados.prevencoes());
            if (mudou) resultados.findByOpcaoId(opcao.getId()).ifPresent(resultado -> {
                resultado.setRequerAtualizacao(true);
                resultados.save(resultado);
            });
        }
        if (!alteradas.isEmpty()) inversoes.saveAll(alteradas);
        boolean mudouConclusao = concluir && decisao.getInversaoConcluidaEm() == null
                || !concluir && !alteradas.isEmpty() && decisao.getInversaoConcluidaEm() != null;
        if (!alteradas.isEmpty() || mudouConclusao) {
            var agora = LocalDateTime.now();
            decisao.setInversaoConcluidaEm(concluir ? agora : null);
            decisao.setDataAtualizacao(agora);
            decisoes.save(decisao);
        }
        return resposta(decisao, par, salvas);
    }

    private boolean atualizar(List<Inversao> salvas, List<Inversao> alteradas, Opcao opcao,
            String perspectiva, String valor) {
        var atual = buscar(salvas, opcao, perspectiva);
        var normalizado = texto(valor);
        if (Objects.equals(atual == null ? null : atual.getReflexao(), normalizado)) return false;
        if (atual == null) {
            atual = new Inversao();
            atual.setOpcao(opcao);
            atual.setPerspectiva(perspectiva);
            salvas.add(atual);
        }
        atual.setReflexao(normalizado);
        alteradas.add(atual);
        return true;
    }

    private void validar(SalvarInversaoRequest request, List<Opcao> par, boolean concluir) {
        if (request == null || request.opcaoA() == null || request.opcaoB() == null)
            throw new ReflexoesInvalidasException("Envie as reflexões das duas opções.");
        var recebidas = List.of(request.opcaoA(), request.opcaoB());
        for (int i = 0; i < 2; i++) {
            var item = recebidas.get(i);
            if (!Objects.equals(par.get(i).getId(), item.opcaoId()))
                throw new ReflexoesInvalidasException("As reflexões devem corresponder às opções A e B desta decisão.");
            for (var valor : new String[]{item.fracassos(), item.prevencoes()}) {
                if (valor != null && valor.length() > 16000)
                    throw new ReflexoesInvalidasException("Cada reflexão pode ter até 16.000 caracteres.");
                if (concluir && texto(valor) == null)
                    throw new ReflexoesInvalidasException("Preencha os possíveis fracassos e como evitá-los nas duas opções.");
            }
        }
    }

    private void exigirAnalise(Decisao decisao) {
        if (decisao.getEtapaAtual() != TecnicasAnaliseService.ETAPA_ANALISE)
            throw new EtapaInvalidaException("Conclua as etapas anteriores antes de acessar a Inversão.");
    }

    private List<Opcao> par(Long id) {
        var par = opcoes.findByDecisaoIdOrderByIdAsc(id);
        if (par.size() != 2) throw new EtapaInvalidaException("A decisão precisa ter suas duas alternativas definidas.");
        return par;
    }

    private Inversao buscar(List<Inversao> lista, Opcao opcao, String perspectiva) {
        var correspondentes = lista.stream().filter(r -> r.getOpcao().getId().equals(opcao.getId())
                && perspectiva.equals(r.getPerspectiva())).toList();
        if (correspondentes.size() > 1)
            throw new EtapaInvalidaException("Há reflexões duplicadas nesta perspectiva. Confira os registros antes de continuar.");
        return correspondentes.isEmpty() ? null : correspondentes.getFirst();
    }

    private String texto(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }

    private InversaoResponse resposta(Decisao decisao, List<Opcao> par, List<Inversao> salvas) {
        var data = decisao.getInversaoConcluidaEm();
        return new InversaoResponse(decisao.getId(), decisao.getTitulo(), decisao.getEtapaAtual(),
                decisao.getStatus(), data != null, data, alternativa(par.get(0), salvas), alternativa(par.get(1), salvas));
    }

    private Alternativa alternativa(Opcao opcao, List<Inversao> salvas) {
        var f = buscar(salvas, opcao, FRACASSOS);
        var p = buscar(salvas, opcao, PREVENCOES);
        return new Alternativa(opcao.getId(), opcao.getNome(), opcao.getDescricao(),
                f == null ? null : f.getId(), f == null ? null : f.getReflexao(),
                p == null ? null : p.getId(), p == null ? null : p.getReflexao());
    }
}
