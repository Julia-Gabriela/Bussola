package br.com.bussola.controller;

import br.com.bussola.dto.request.DefinirOpcoesRequest;
import br.com.bussola.dto.response.OpcoesResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.OpcoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/decisoes/{decisaoId}/opcoes")
@SecurityRequirement(name = "bearerAuth")
public class OpcoesController {
    private final OpcoesService opcoes;

    public OpcoesController(OpcoesService opcoes) { this.opcoes = opcoes; }

    @GetMapping
    @Operation(summary = "Consultar as opções da própria decisão", description = "Disponível a partir da etapa 3. Opções ainda não salvas retornam null.")
    public ResponseEntity<OpcoesResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long decisaoId) {
        return resposta(opcoes.consultar(usuario.id(), decisaoId));
    }

    @PutMapping
    @Operation(summary = "Salvar nome da decisão e opções A e B", description = "Salva o par de opções na etapa 3 sem avançar. Preserva os IDs nas edições.")
    public ResponseEntity<OpcoesResponse> salvar(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long decisaoId, @Valid @RequestBody DefinirOpcoesRequest request) {
        return resposta(opcoes.salvar(usuario.id(), decisaoId, request));
    }

    @PostMapping("/concluir")
    @Operation(summary = "Salvar opções e avançar para Critérios", description = "Salva título e duas opções e avança de 3 para 4 na mesma transação. Repetir o mesmo envio na etapa 4 não duplica opções nem avança novamente.")
    public ResponseEntity<OpcoesResponse> concluir(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long decisaoId, @Valid @RequestBody DefinirOpcoesRequest request) {
        return resposta(opcoes.concluir(usuario.id(), decisaoId, request));
    }

    private ResponseEntity<OpcoesResponse> resposta(OpcoesResponse corpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(corpo);
    }
}
