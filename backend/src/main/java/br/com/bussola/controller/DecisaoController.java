package br.com.bussola.controller;

import br.com.bussola.dto.request.NovaDecisaoRequest;
import br.com.bussola.dto.response.DecisaoResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.DecisaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/decisoes")
@SecurityRequirement(name = "bearerAuth")
public class DecisaoController {
    private final DecisaoService decisoes;

    public DecisaoController(DecisaoService decisoes) {
        this.decisoes = decisoes;
    }

    @PostMapping
    @Operation(summary = "Iniciar nova decisão", description = "Título obrigatório e contexto opcional. Inicia na etapa 1, em andamento.")
    public ResponseEntity<DecisaoResponse> criar(@AuthenticationPrincipal UsuarioResponse usuario,
            @Valid @RequestBody NovaDecisaoRequest request) {
        var decisao = decisoes.criar(usuario.id(), request);
        return ResponseEntity.created(URI.create("/decisoes/" + decisao.id()))
                .cacheControl(CacheControl.noStore()).body(decisao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar uma decisão do usuário autenticado")
    public ResponseEntity<DecisaoResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(decisoes.consultar(usuario.id(), id));
    }
}
