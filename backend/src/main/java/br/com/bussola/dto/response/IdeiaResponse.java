package br.com.bussola.dto.response;

import br.com.bussola.model.entity.IdeiaBrainstorming;

public record IdeiaResponse(Long id, String conteudo, String categoria) {
    public static IdeiaResponse de(IdeiaBrainstorming ideia) {
        return new IdeiaResponse(ideia.getId(), ideia.getConteudo(), ideia.getCategoria());
    }
}
