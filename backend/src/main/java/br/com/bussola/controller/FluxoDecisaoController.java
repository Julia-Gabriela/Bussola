package br.com.bussola.controller;

import br.com.bussola.dto.request.IdeiaRequest;
import br.com.bussola.dto.request.NovaDecisaoRequest;
import br.com.bussola.dto.response.DecisaoResponse;
import br.com.bussola.dto.response.IdeiaResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.FluxoDecisaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/decisoes/{id}")
@SecurityRequirement(name = "bearerAuth")
public class FluxoDecisaoController {
    private final FluxoDecisaoService fluxo;

    public FluxoDecisaoController(FluxoDecisaoService fluxo) {
        this.fluxo = fluxo;
    }

    @PutMapping("/contexto")
    @Operation(summary = "Salvar título e contexto durante a etapa 1")
    public ResponseEntity<DecisaoResponse> salvarContexto(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @Valid @RequestBody NovaDecisaoRequest request) {
        return resposta(fluxo.salvarContexto(usuario.id(), id, request));
    }

    @PostMapping("/contexto/concluir")
    @Operation(summary = "Concluir contextualização e liberar brainstorming (etapa 2)")
    public ResponseEntity<DecisaoResponse> concluirContexto(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id) {
        return resposta(fluxo.concluirContexto(usuario.id(), id));
    }

    @GetMapping("/ideias")
    @Operation(summary = "Consultar ideias da decisão")
    public ResponseEntity<List<IdeiaResponse>> listar(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id) {
        return resposta(fluxo.listar(usuario.id(), id));
    }

    @PostMapping("/ideias")
    @Operation(summary = "Adicionar ideia ao brainstorming")
    public ResponseEntity<IdeiaResponse> adicionar(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @Valid @RequestBody IdeiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(fluxo.adicionar(usuario.id(), id, request));
    }

    @PutMapping("/ideias/{ideiaId}")
    @Operation(summary = "Editar conteúdo ou categoria de uma ideia")
    public ResponseEntity<IdeiaResponse> editar(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @PathVariable Long ideiaId, @Valid @RequestBody IdeiaRequest request) {
        return resposta(fluxo.editar(usuario.id(), id, ideiaId, request));
    }

    @DeleteMapping("/ideias/{ideiaId}")
    @Operation(summary = "Excluir ideia durante o brainstorming")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @PathVariable Long ideiaId) {
        fluxo.excluir(usuario.id(), id, ideiaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/brainstorming/concluir")
    @Operation(summary = "Concluir brainstorming e liberar definição das opções (etapa 3)",
            description = "Exige ao menos uma ideia, preserva as ideias e mantém a decisão em andamento.")
    public ResponseEntity<DecisaoResponse> concluirBrainstorming(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id) {
        return resposta(fluxo.concluirBrainstorming(usuario.id(), id));
    }

    private <T> ResponseEntity<T> resposta(T corpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(corpo);
    }
}
