package br.com.bussola.service;

import br.com.bussola.dto.request.NovaDecisaoRequest;
import br.com.bussola.dto.response.DecisaoResponse;
import br.com.bussola.exception.DecisaoNaoEncontradaException;
import br.com.bussola.model.entity.Decisao;
import br.com.bussola.repository.DecisaoRepository;
import br.com.bussola.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisaoService {
    private final DecisaoRepository decisoes;
    private final UsuarioRepository usuarios;

    public DecisaoService(DecisaoRepository decisoes, UsuarioRepository usuarios) {
        this.decisoes = decisoes;
        this.usuarios = usuarios;
    }

    @Transactional
    public DecisaoResponse criar(Long usuarioId, NovaDecisaoRequest request) {
        var decisao = new Decisao();
        decisao.setUsuario(usuarios.getReferenceById(usuarioId));
        decisao.setTitulo(request.titulo().strip());
        String contexto = request.contexto();
        decisao.setContexto(contexto == null || contexto.isBlank() ? null : contexto.strip());
        return DecisaoResponse.de(decisoes.save(decisao));
    }

    @Transactional(readOnly = true)
    public DecisaoResponse consultar(Long usuarioId, Long decisaoId) {
        return decisoes.findByIdAndUsuarioId(decisaoId, usuarioId)
                .map(DecisaoResponse::de).orElseThrow(DecisaoNaoEncontradaException::new);
    }
}
