package br.com.bussola.dto.response;

import br.com.bussola.model.entity.Usuario;
import java.time.LocalDate;

public record PerfilResponse(Long id, String nomeCompleto, LocalDate dataNascimento, String email) {
    public static PerfilResponse de(Usuario usuario) {
        return new PerfilResponse(usuario.getId(), usuario.getNomeCompleto(), usuario.getDataNascimento(), usuario.getEmail());
    }
}
