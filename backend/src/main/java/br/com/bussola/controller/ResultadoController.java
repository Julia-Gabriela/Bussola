package br.com.bussola.controller;

import br.com.bussola.dto.response.ResultadoComparativoResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.ResultadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/decisoes/{decisaoId}/resultado")
@SecurityRequirement(name = "bearerAuth")
public class ResultadoController {
    private final ResultadoService resultados;
    public ResultadoController(ResultadoService resultados) { this.resultados = resultados; }
    @GetMapping
    @Operation(summary = "Consultar o resultado comparativo", description = "Calcula as notas ponderadas após a conclusão das técnicas de análise.")
    public ResponseEntity<ResultadoComparativoResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(resultados.consultar(usuario.id(), decisaoId));
    }
}