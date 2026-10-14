package br.com.bussola.controller;

import br.com.bussola.dto.request.AvaliarCriteriosRequest;
import br.com.bussola.dto.request.ConfirmarAvaliacaoRequest;
import br.com.bussola.dto.response.CriteriosResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.CriteriosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/decisoes/{decisaoId}/criterios")
@SecurityRequirement(name = "bearerAuth")
public class CriteriosController {
    private final CriteriosService criterios;
    public CriteriosController(CriteriosService criterios) { this.criterios = criterios; }

    @GetMapping
    @Operation(summary = "Consultar critérios e notas da própria decisão", description = "A partir da etapa 4. Notas ausentes são null. Risco usa MENOR_MELHOR: 5 representa maior risco.")
    public ResponseEntity<CriteriosResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId) {
        return resposta(criterios.consultar(usuario.id(), decisaoId));
    }

    @PutMapping
    @Operation(summary = "Salvar avaliação parcial", description = "Substitui a lista completa na etapa 4. Inclua os cinco critérios fixos; omitir um personalizado o remove com suas notas. Notas null ficam sem avaliação. Até 20 critérios; nomes únicos identificam critérios e preservam IDs.")
    public ResponseEntity<CriteriosResponse> salvar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId,
            @Valid @RequestBody AvaliarCriteriosRequest request) {
        return resposta(criterios.salvar(usuario.id(), decisaoId, request));
    }

    @PostMapping("/confirmacao")
    @Operation(summary = "Confirmar as notas de uma alternativa", description = "Remove só a pendência de A ou de B. Exige a versão carregada na tela. Não altera notas, pesos nem etapaAtual. Salvar para depois não confirma.")
    public ResponseEntity<CriteriosResponse> confirmar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId,
            @Valid @RequestBody ConfirmarAvaliacaoRequest request) {
        return resposta(criterios.confirmar(usuario.id(), decisaoId, request));
    }

    @PostMapping("/concluir")
    @Operation(summary = "Salvar avaliação completa e avançar para Pesos", description = "Exige notas de 1 a 5 para ambas as opções em todos os critérios. Avança de 4 para 5 atomicamente, sem concluir a decisão. Repetir os mesmos dados na etapa 5 não avança novamente.")
    public ResponseEntity<CriteriosResponse> concluir(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId,
            @Valid @RequestBody AvaliarCriteriosRequest request) {
        return resposta(criterios.concluir(usuario.id(), decisaoId, request));
    }

    private ResponseEntity<CriteriosResponse> resposta(CriteriosResponse corpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(corpo);
    }
}
