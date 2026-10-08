package br.com.bussola.dto.response;

import java.util.List;

public record HomeResponse(UsuarioResponse usuario, long decisoesConcluidas,
        long decisoesEmAndamento, List<DecisaoResumoResponse> decisoesRecentes,
        DecisaoResumoResponse decisaoEmAndamento) {
}
