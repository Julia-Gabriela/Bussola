package br.com.bussola.controller;

import br.com.bussola.dto.request.SalvarInversaoRequest;
import br.com.bussola.dto.response.InversaoResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.InversaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/decisoes/{decisaoId}/inversao")
@SecurityRequirement(name = "bearerAuth")
public class InversaoController {
    private final InversaoService reflexoes;
    public InversaoController(InversaoService reflexoes) { this.reflexoes = reflexoes; }

    @GetMapping
    @Operation(summary = "Consultar reflexões de Inversão da própria decisão", description = "Etapa 6. Não cria dados. Textos ainda não preenchidos são null.")
    public ResponseEntity<InversaoResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId) {
        return resposta(reflexoes.consultar(usuario.id(), decisaoId));
    }

    @PutMapping
    @Operation(summary = "Salvar reflexões parciais", description = "Envie as duas opções e as duas perspectivas. Null limpa a resposta. Até 16.000 caracteres por texto. Alterações preservam IDs e exigem nova conclusão da técnica.")
    public ResponseEntity<InversaoResponse> salvar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId,
            @Valid @RequestBody SalvarInversaoRequest request) {
        return resposta(reflexoes.salvar(usuario.id(), decisaoId, request));
    }

    @PostMapping("/concluir")
    @Operation(summary = "Concluir o método de Inversão", description = "Exige as quatro reflexões. Salva ambas atomicamente e registra conclusão da técnica, sem encerrar a decisão nem alterar a etapa 6. Reenvios idênticos são idempotentes.")
    public ResponseEntity<InversaoResponse> concluir(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId,
            @Valid @RequestBody SalvarInversaoRequest request) {
        return resposta(reflexoes.concluir(usuario.id(), decisaoId, request));
    }

    private ResponseEntity<InversaoResponse> resposta(InversaoResponse dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
