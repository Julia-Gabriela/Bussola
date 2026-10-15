package br.com.bussola.controller;
import br.com.bussola.dto.request.DefinirPesosRequest;
import br.com.bussola.dto.response.PesosResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.PesosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/decisoes/{decisaoId}/pesos")
@SecurityRequirement(name = "bearerAuth")
public class PesosController {
    private final PesosService pesos;
    public PesosController(PesosService pesos) { this.pesos = pesos; }
    @GetMapping
    @Operation(summary = "Consultar os pesos dos critérios", description = "Disponível a partir da etapa 5.")
    public ResponseEntity<PesosResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId) {
        return resposta(pesos.consultar(usuario.id(), decisaoId));
    }
    @PutMapping
    @Operation(summary = "Salvar os pesos dos critérios", description = "Atualiza todos os pesos de 1 a 5 sem avançar. A decisão precisa já ter alcançado a etapa 5.")
    public ResponseEntity<PesosResponse> salvar(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long decisaoId, @Valid @RequestBody DefinirPesosRequest request) {
        return resposta(pesos.salvar(usuario.id(), decisaoId, request));
    }
    @PostMapping("/concluir")
    @Operation(summary = "Salvar pesos e avançar para Análise", description = "Conclui a etapa 5 e libera a etapa 6.")
    public ResponseEntity<PesosResponse> concluir(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long decisaoId, @Valid @RequestBody DefinirPesosRequest request) {
        return resposta(pesos.concluir(usuario.id(), decisaoId, request));
    }
    private ResponseEntity<PesosResponse> resposta(PesosResponse corpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(corpo);
    }
}
