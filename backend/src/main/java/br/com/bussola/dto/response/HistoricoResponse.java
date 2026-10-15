package br.com.bussola.dto.response;

import java.util.List;

public record HistoricoResponse(List<DecisaoResumoResponse> decisoes, int pagina, int totalPaginas,
        long totalRegistros, long concluidas, long emAndamento) {}
