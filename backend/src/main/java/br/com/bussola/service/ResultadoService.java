package br.com.bussola.service;

import br.com.bussola.dto.response.ResultadoComparativoResponse;
import br.com.bussola.dto.response.ResultadoComparativoResponse.Alternativa;
import br.com.bussola.dto.response.ResultadoComparativoResponse.Criterio;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.exception.EtapaInvalidaException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.entity.Resultado;
import br.com.bussola.model.enums.StatusDecisao;
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

    /**
     * Monta o resultado comparativo de uma decisão do usuário autenticado, sem gravar nada.
     * <p>
     * Valida todas as técnicas e calcula as pontuações a partir das notas e pesos
     * atuais, tanto para decisões em andamento quanto concluídas (depois de
     * concluída, notas e pesos não podem mais mudar, então o cálculo é estável).
     * Por ser somente leitura, consultas simultâneas, inclusive de abas
     * diferentes, não disputam a tabela {@code resultados} e não podem gerar
     * inserção duplicada. As pontuações só são persistidas em {@link #finalizar}.
     *
     * @param usuarioId dono exigido da decisão
     * @param decisaoId decisão consultada
     * @return resultado com as duas alternativas, os detalhes por critério, o status e a indicação de empate
     * @throws DecisaoNaoEncontradaException se a decisão não existir ou for de outra pessoa
     * @throws EtapaInvalidaException se ainda faltar algum requisito do resultado
     */
    @Transactional(readOnly = true)
    public ResultadoComparativoResponse consultar(Long usuarioId, Long decisaoId) {
        var decisao=decisoes.findByIdAndUsuarioId(decisaoId,usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        return montar(decisao);
    }

    /**
     * Finaliza uma decisão, tornando-a somente leitura de forma definitiva.
     * <p>
     * Bloqueia a decisão como as demais escritas, exige os mesmos requisitos do
     * resultado (etapas, técnicas concluídas e nenhuma avaliação pendente), grava
     * as pontuações finais e muda o status para {@code CONCLUIDA}. Se qualquer
     * requisito falhar, nada é gravado e a decisão continua em andamento. Repetir
     * a chamada em uma decisão já concluída devolve o resultado sem gravar nada
     * nem alterar o status. Um empate pode ser finalizado normalmente.
     * <p>
     * A gravação acontece com a decisão bloqueada ({@code buscarParaAtualizar}),
     * o mesmo bloqueio usado pelas demais escritas da decisão. Por isso duas
     * finalizações simultâneas são serializadas: a segunda já encontra a decisão
     * concluída e não tenta inserir o resultado de novo.
     *
     * @param usuarioId dono exigido da decisão
     * @param decisaoId decisão que será finalizada
     * @return resultado final, já com status {@code CONCLUIDA} e data de finalização
     * @throws DecisaoNaoEncontradaException se a decisão não existir ou for de outra pessoa
     * @throws EtapaInvalidaException se o resultado ainda não puder ser calculado
     */
    @Transactional
    public ResultadoComparativoResponse finalizar(Long usuarioId, Long decisaoId) {
        var decisao=decisoes.buscarParaAtualizar(decisaoId,usuarioId).orElseThrow(DecisaoNaoEncontradaException::new);
        if(decisao.getStatus()==StatusDecisao.CONCLUIDA) return montar(decisao);
        var resultado=montar(decisao);
        var opcoesDaDecisao=opcoes.findByDecisaoIdOrderByIdAsc(decisaoId);
        for(int i=0;i<opcoesDaDecisao.size();i++) gravar(opcoesDaDecisao.get(i),resultado.alternativas().get(i).pontuacao());
        decisao.setStatus(StatusDecisao.CONCLUIDA);
        decisao.setDataAtualizacao(LocalDateTime.now());
        decisoes.saveAndFlush(decisao);
        return resposta(decisao,resultado.pontuacaoMaxima(),resultado.alternativas());
    }

    /**
     * Valida os requisitos do resultado e calcula as pontuações, sem efeito de escrita.
     * <p>
     * Para cada critério, a nota informada é convertida uma única vez na
     * contribuição favorável ({@link CatalogoCriterios#notaFavoravel}: no Risco,
     * {@code 6 - nota}) e multiplicada pelo peso. A pontuação máxima é a soma de
     * {@code peso * 5}, que vale para todos os sentidos porque a nota ajustada
     * também vai de 1 a 5.
     *
     * @param decisao decisão já carregada e pertencente ao usuário
     * @return resultado comparativo calculado
     * @throws EtapaInvalidaException se faltar etapa, técnica, nota ou se houver avaliação pendente
     */
    private ResultadoComparativoResponse montar(Decisao decisao) {
        Long decisaoId=decisao.getId();
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
                String sentido=CatalogoCriterios.sentido(criterio.getNome());
                int ajustada=CatalogoCriterios.notaFavoravel(nota,sentido);
                int pontos=ajustada*criterio.getPeso();total+=pontos;
                detalhe.add(new Criterio(criterio.getNome(),criterio.getPeso(),nota,sentido,ajustada,pontos));}
            resposta.add(new Alternativa(opcao.getId(),opcao.getNome(),total,List.copyOf(detalhe)));}
        return resposta(decisao,maximo,List.copyOf(resposta));
    }

    /**
     * Monta a resposta final, incluindo a data de finalização e a indicação de empate.
     * <p>
     * Com pontuações iguais, {@code empate} é verdadeiro e {@code recomendadaId}
     * fica nulo: a ordem de cadastro nunca é usada como desempate. Com
     * pontuações diferentes, recomenda a alternativa de maior pontuação.
     *
     * @param decisao decisão consultada ou finalizada
     * @param maximo pontuação máxima possível
     * @param alternativas as duas alternativas já pontuadas
     * @return resposta pronta para a API
     */
    private ResultadoComparativoResponse resposta(Decisao decisao,int maximo,List<Alternativa> alternativas){
        var finalizadaEm=decisao.getStatus()==StatusDecisao.CONCLUIDA?decisao.getDataAtualizacao():null;
        var a=alternativas.get(0); var b=alternativas.get(1);
        boolean empate=a.pontuacao()==b.pontuacao();
        Long recomendada=empate?null:(a.pontuacao()>b.pontuacao()?a.id():b.id());
        return new ResultadoComparativoResponse(decisao.getId(),decisao.getTitulo(),decisao.getEtapaAtual(),decisao.getStatus(),
                finalizadaEm,maximo,empate,recomendada,alternativas);
    }

    /**
     * Grava (insere ou atualiza) a pontuação final de uma alternativa.
     * <p>
     * Só é chamado por {@link #finalizar}, com a decisão bloqueada, então não há
     * duas transações tentando inserir a mesma linha de {@code resultados}
     * (índice único em {@code opcao_id}).
     *
     * @param opcao alternativa pontuada
     * @param total pontuação total calculada para ela
     */
    private void gravar(Opcao opcao,int total){Resultado r=resultados.findByOpcaoId(opcao.getId()).orElseGet(()->{Resultado novo=new Resultado();novo.setOpcao(opcao);return novo;});
        r.setPontuacaoTotal(total);r.setDataCalculo(LocalDateTime.now());r.setRequerAtualizacao(false);resultados.save(r);}
}
