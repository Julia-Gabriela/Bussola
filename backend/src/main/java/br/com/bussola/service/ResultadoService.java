package br.com.bussola.service;

import br.com.bussola.dto.response.ResultadoComparativoResponse;
import br.com.bussola.dto.response.ResultadoComparativoResponse.Alternativa;
import br.com.bussola.dto.response.ResultadoComparativoResponse.Criterio;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.model.entity.Resultado;
import br.com.bussola.model.enums.TipoProContra;
import br.com.bussola.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultadoService {
    private final DecisaoRepository decisoes; private final CriterioRepository criterios; private final OpcaoRepository opcoes;
    private final AvaliacaoRepository avaliacoes; private final ProContraRepository argumentos; private final ItemGutRepository fatores;
    private final ResultadoRepository resultados; private final AvaliacoesPendentes pendencias;
    public ResultadoService(DecisaoRepository decisoes, CriterioRepository criterios, OpcaoRepository opcoes,
            AvaliacaoRepository avaliacoes, ProContraRepository argumentos, ItemGutRepository fatores,
            ResultadoRepository resultados, AvaliacoesPendentes pendencias) {
        this.decisoes=decisoes; this.criterios=criterios; this.opcoes=opcoes; this.avaliacoes=avaliacoes;
        this.argumentos=argumentos; this.fatores=fatores; this.resultados=resultados; this.pendencias=pendencias;
    }
    @Transactional
    public ResultadoComparativoResponse consultar(Long usuarioId, Long decisaoId) {
        var decisao=decisoes.findByIdAndUsuarioId(decisaoId,usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        if(decisao.getEtapaAtual()!=TecnicasAnaliseService.ETAPA_ANALISE) throw new EtapaInvalidaException("Conclua as etapas anteriores antes de consultar o resultado.");
        var opcoesDaDecisao=opcoes.findByDecisaoIdOrderByIdAsc(decisaoId);
        if(opcoesDaDecisao.size()!=2) throw new EtapaInvalidaException("Defina as duas alternativas antes do resultado.");
        for(var opcao:opcoesDaDecisao){var itens=argumentos.findByOpcaoIdOrderByIdAsc(opcao.getId());
            if(itens.stream().noneMatch(i->TipoProContra.PRO.equals(i.getTipo()))||itens.stream().noneMatch(i->TipoProContra.CONTRA.equals(i.getTipo())))
                throw new EtapaInvalidaException("Conclua Prós e Contras: registre ao menos um pró e um contra para cada alternativa.");}
        if(fatores.findByDecisaoIdOrderByIdAsc(decisaoId).isEmpty()) throw new EtapaInvalidaException("Conclua a Matriz GUT registrando ao menos um fator.");
        if(decisao.getReflexao101010ConcluidaEm()==null) throw new EtapaInvalidaException("Conclua a técnica 10/10/10 antes de ver o resultado.");
        if(decisao.getInversaoConcluidaEm()==null) throw new EtapaInvalidaException("Conclua a técnica de Inversão antes de ver o resultado.");
        if(pendencias.possuiPendencia(decisaoId)) throw new EtapaInvalidaException("Confirme as avaliações pendentes antes de calcular o resultado.");
        var listaCriterios=criterios.findByDecisaoIdOrderByIdAsc(decisaoId);
        if(listaCriterios.isEmpty()) throw new EtapaInvalidaException("A decisão ainda não tem critérios avaliados.");
        Map<String,Integer> notas=new HashMap<>();
        avaliacoes.findByCriterioDecisaoId(decisaoId).forEach(a->notas.put(a.getCriterio().getId()+":"+a.getOpcao().getId(),a.getValor()));
        int maximo=listaCriterios.stream().mapToInt(c->c.getPeso()*5).sum(); List<Alternativa> resposta=new ArrayList<>();
        for(var opcao:opcoesDaDecisao){int total=0;List<Criterio> detalhe=new ArrayList<>();
            for(var criterio:listaCriterios){Integer nota=notas.get(criterio.getId()+":"+opcao.getId());
                if(nota==null) throw new EtapaInvalidaException("Complete as avaliações das duas alternativas antes do resultado.");
                int pontos=nota*criterio.getPeso();total+=pontos;detalhe.add(new Criterio(criterio.getNome(),criterio.getPeso(),nota,pontos));}
            gravar(opcao,total);resposta.add(new Alternativa(opcao.getId(),opcao.getNome(),total,List.copyOf(detalhe)));}
        return new ResultadoComparativoResponse(decisao.getId(),decisao.getTitulo(),decisao.getEtapaAtual(),decisao.getStatus(),maximo,List.copyOf(resposta));
    }
    private void gravar(br.com.bussola.model.entity.Opcao opcao,int total){Resultado r=resultados.findByOpcaoId(opcao.getId()).orElseGet(()->{Resultado novo=new Resultado();novo.setOpcao(opcao);return novo;});
        r.setPontuacaoTotal(total);r.setDataCalculo(LocalDateTime.now());r.setRequerAtualizacao(false);resultados.save(r);}
}