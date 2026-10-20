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
@RequestMapping("/decisoes/{decisaoId}")
@SecurityRequirement(name = "bearerAuth")
public class ResultadoController {
    private final ResultadoService resultados;
    public ResultadoController(ResultadoService resultados) { this.resultados = resultados; }
    @GetMapping("/resultado")
    @Operation(summary = "Consultar o resultado comparativo", description = "Calcula as notas ponderadas após a conclusão das técnicas de análise. Em decisões concluídas, devolve as pontuações gravadas sem recalcular.")
    public ResponseEntity<ResultadoComparativoResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(resultados.consultar(usuario.id(), decisaoId));
    }
    @PostMapping("/finalizar")
    @Operation(summary = "Finalizar a decisão", description = "Exige um resultado válido, grava as pontuações finais e marca a decisão como CONCLUIDA. A partir daí ela fica somente para consulta; a exclusão continua permitida.")
    public ResponseEntity<ResultadoComparativoResponse> finalizar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(resultados.finalizar(usuario.id(), decisaoId));
    }
}
