package br.com.bussola.dto.response;

public record LoginResponse(String accessToken, String tokenType, long inatividadeMaximaSegundos,
        UsuarioResponse usuario) {
}
