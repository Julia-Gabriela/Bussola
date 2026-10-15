package br.com.bussola.controller;

import br.com.bussola.dto.request.SalvarReflexoes101010Request;
import br.com.bussola.dto.response.Reflexoes101010Response;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.Reflexoes101010Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/decisoes/{decisaoId}/reflexoes-101010")
@SecurityRequirement(name = "bearerAuth")
public class Reflexoes101010Controller {
    private final Reflexoes101010Service reflexoes;
    public Reflexoes101010Controller(Reflexoes101010Service reflexoes) { this.reflexoes = reflexoes; }

    @GetMapping
    @Operation(summary = "Consultar reflexões 10–10–10 da própria decisão", description = "Etapa 6. Não cria dados. Textos ainda não preenchidos são null.")
    public ResponseEntity<Reflexoes101010Response> consultar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId) {
        return resposta(reflexoes.consultar(usuario.id(), decisaoId));
    }

    @PutMapping
    @Operation(summary = "Salvar reflexões parciais", description = "Envie as duas opções e os três prazos. Null limpa a resposta. Até 16.000 caracteres por texto. Alterações preservam IDs e exigem nova conclusão da técnica.")
    public ResponseEntity<Reflexoes101010Response> salvar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId,
            @Valid @RequestBody SalvarReflexoes101010Request request) {
        return resposta(reflexoes.salvar(usuario.id(), decisaoId, request));
    }

    @PostMapping("/concluir")
    @Operation(summary = "Concluir o método 10–10–10", description = "Exige as seis reflexões. Salva ambas atomicamente e registra conclusão da técnica, sem encerrar a decisão nem alterar a etapa 6. Reenvios idênticos são idempotentes.")
    public ResponseEntity<Reflexoes101010Response> concluir(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long decisaoId,
            @Valid @RequestBody SalvarReflexoes101010Request request) {
        return resposta(reflexoes.concluir(usuario.id(), decisaoId, request));
    }

    private ResponseEntity<Reflexoes101010Response> resposta(Reflexoes101010Response dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
