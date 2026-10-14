package br.com.bussola.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.bussola.dto.request.IdeiaRequest;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.model.entity.IdeiaBrainstorming;
import br.com.bussola.model.entity.Opcao;
import br.com.bussola.model.enums.CategoriaIdeia;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.IdeiaBrainstormingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FluxoDecisaoServiceTest {
    private final DecisaoRepository decisoes = mock(DecisaoRepository.class);
    private final IdeiaBrainstormingRepository ideias = mock(IdeiaBrainstormingRepository.class);
    private final FluxoDecisaoService service = new FluxoDecisaoService(decisoes, ideias);
    private final Decisao decisao = new Decisao();
    private final Opcao alternativa = new Opcao();

    @BeforeEach
    void preparar() {
        decisao.setId(12L);
        decisao.setTitulo("Minha decisão");
        decisao.setStatus(StatusDecisao.EM_ANDAMENTO);
        decisao.setEtapaAtual(5);
        decisao.setVersaoAvaliacao(2);
        alternativa.setId(21L);
        alternativa.setNome("Trabalhar");
        alternativa.setAvaliacaoPendente(false);
        when(decisoes.buscarParaAtualizar(12L, 7L)).thenReturn(Optional.of(decisao));
        when(ideias.save(any())).thenAnswer(inv -> {
            IdeiaBrainstorming ideia = inv.getArgument(0);
            ideia.setId(40L);
            return ideia;
        });
        when(ideias.findByIdAndDecisaoId(40L, 12L)).thenAnswer(inv -> {
            var ideia = new IdeiaBrainstorming();
            ideia.setId(40L);
            ideia.setDecisao(decisao);
            ideia.setConteudo("Ideia original");
            ideia.setCategoria("FAVORAVEL");
            return Optional.of(ideia);
        });
    }

    @Test
    void brainstormingNaoCriaPendenciaNemMudaEtapa() {
        var pedido = new IdeiaRequest("Mais tempo livre", CategoriaIdeia.FAVORAVEL);
        service.adicionar(7L, 12L, pedido);
        service.editar(7L, 12L, 40L, new IdeiaRequest("Mais tempo livre revisado", CategoriaIdeia.DUVIDA));
        service.excluir(7L, 12L, 40L);
        assertEquals(5, decisao.getEtapaAtual());
        assertEquals(2L, decisao.getVersaoAvaliacao());
        assertFalse(alternativa.isAvaliacaoPendente());
    }
}
