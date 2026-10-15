package br.com.bussola.service;

import br.com.bussola.dto.request.ItemGutRequest;
import br.com.bussola.dto.request.ProContraRequest;
import br.com.bussola.dto.response.AnaliseResponse;
import br.com.bussola.dto.response.AnaliseResponse.Alternativa;
import br.com.bussola.dto.response.AnaliseResponse.Argumento;
import br.com.bussola.dto.response.AnaliseResponse.FatorGut;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.exception.RegistroAnaliseNaoEncontradoException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.ItemGut;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.entity.ProContra;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.ItemGutRepository;
import br.com.bussola.repository.OpcaoRepository;
import br.com.bussola.repository.ProContraRepository;
import br.com.bussola.repository.ResultadoRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TecnicasAnaliseService {
    public static final int ETAPA_ANALISE = 6;
    private final DecisaoRepository decisoes;
    private final OpcaoRepository opcoes;
    private final ProContraRepository argumentos;
    private final ItemGutRepository fatores;
    private final ResultadoRepository resultados;

    public TecnicasAnaliseService(DecisaoRepository decisoes, OpcaoRepository opcoes,
            ProContraRepository argumentos, ItemGutRepository fatores, ResultadoRepository resultados) {
        this.decisoes = decisoes;
        this.opcoes = opcoes;
        this.argumentos = argumentos;
        this.fatores = fatores;
        this.resultados = resultados;
    }

    @Transactional(readOnly = true)
    public AnaliseResponse consultar(Long usuarioId, Long id) {
        var decisao = decisoes.findByIdAndUsuarioId(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        exigirAnalise(decisao);
        var alternativas = par(id).stream().map(opcao -> new Alternativa(opcao.getId(), opcao.getNome(),
                argumentos.findByOpcaoIdOrderByIdAsc(opcao.getId()).stream().map(Argumento::de).toList())).toList();
        var matriz = fatores.findByDecisaoIdOrderByIdAsc(id).stream().map(FatorGut::de)
                .sorted(Comparator.comparingInt(FatorGut::prioridade).reversed().thenComparing(FatorGut::id)).toList();
        return new AnaliseResponse(id, decisao.getTitulo(), decisao.getEtapaAtual(), decisao.getStatus(), alternativas, matriz);
    }

    @Transactional
    public Argumento salvarArgumento(Long usuarioId, Long id, Long opcaoId, Long registroId, ProContraRequest request) {
        var decisao = bloquear(usuarioId, id);
        var par = par(id);
        var opcao = par.stream().filter(item -> item.getId().equals(opcaoId)).findFirst()
                .orElseThrow(RegistroAnaliseNaoEncontradoException::new);
        var registro = registroId == null ? new ProContra() : argumentos.findByIdAndOpcaoId(registroId, opcaoId)
                .orElseThrow(RegistroAnaliseNaoEncontradoException::new);
        registro.setOpcao(opcao);
        registro.setTipo(request.tipo());
        registro.setDescricao(request.descricao().strip());
        atualizar(decisao, List.of(opcao));
        return Argumento.de(argumentos.save(registro));
    }

    @Transactional
    public void excluirArgumento(Long usuarioId, Long id, Long opcaoId, Long registroId) {
        var decisao = bloquear(usuarioId, id);
        var opcao = par(id).stream().filter(item -> item.getId().equals(opcaoId)).findFirst()
                .orElseThrow(RegistroAnaliseNaoEncontradoException::new);
        var registro = argumentos.findByIdAndOpcaoId(registroId, opcaoId)
                .orElseThrow(RegistroAnaliseNaoEncontradoException::new);
        argumentos.delete(registro);
        atualizar(decisao, List.of(opcao));
    }

    @Transactional
    public FatorGut salvarFator(Long usuarioId, Long id, Long registroId, ItemGutRequest request) {
        var decisao = bloquear(usuarioId, id);
        var par = par(id);
        var registro = registroId == null ? new ItemGut() : fatores.findByIdAndDecisaoId(registroId, id)
                .orElseThrow(RegistroAnaliseNaoEncontradoException::new);
        registro.setDecisao(decisao);
        registro.setDescricao(request.descricao().strip());
        registro.setGravidade(request.gravidade().intValueExact());
        registro.setUrgencia(request.urgencia().intValueExact());
        registro.setTendencia(request.tendencia().intValueExact());
        atualizar(decisao, par);
        return FatorGut.de(fatores.save(registro));
    }

    @Transactional
    public void excluirFator(Long usuarioId, Long id, Long registroId) {
        var decisao = bloquear(usuarioId, id);
        var par = par(id);
        var registro = fatores.findByIdAndDecisaoId(registroId, id)
                .orElseThrow(RegistroAnaliseNaoEncontradoException::new);
        fatores.delete(registro);
        atualizar(decisao, par);
    }

    private Decisao bloquear(Long usuarioId, Long id) {
        var decisao = decisoes.buscarParaAtualizar(id, usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        exigirAnalise(decisao);
        if (decisao.getStatus() != StatusDecisao.EM_ANDAMENTO) {
            throw new EtapaInvalidaException("Esta decisão está concluída e disponível apenas para consulta.");
        }
        return decisao;
    }

    private void exigirAnalise(Decisao decisao) {
        if (decisao.getEtapaAtual() != ETAPA_ANALISE) {
            throw new EtapaInvalidaException("Conclua as etapas anteriores antes de acessar as técnicas de análise.");
        }
    }

    private List<Opcao> par(Long id) {
        var par = opcoes.findByDecisaoIdOrderByIdAsc(id);
        if (par.size() != 2) throw new EtapaInvalidaException("A decisão precisa ter suas duas alternativas definidas.");
        return par;
    }

    private void atualizar(Decisao decisao, List<Opcao> afetadas) {
        decisao.setDataAtualizacao(LocalDateTime.now());
        // Mantém o resultado anterior disponível até a próxima atualização.
        afetadas.forEach(opcao -> resultados.findByOpcaoId(opcao.getId()).ifPresent(resultado -> {
            resultado.setRequerAtualizacao(true);
            resultados.save(resultado);
        }));
    }
}
