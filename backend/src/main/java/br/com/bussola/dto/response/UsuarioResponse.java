package br.com.bussola.dto.response;

import br.com.bussola.model.entity.Usuario;

public record UsuarioResponse(Long id, String nomeCompleto, String email) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNomeCompleto(), usuario.getEmail());
    }
}
