package br.com.bussola.controller;

import br.com.bussola.dto.request.ItemGutRequest;
import br.com.bussola.dto.request.ProContraRequest;
import br.com.bussola.dto.response.AnaliseResponse;
import br.com.bussola.dto.response.AnaliseResponse.Argumento;
import br.com.bussola.dto.response.AnaliseResponse.FatorGut;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.TecnicasAnaliseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
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
public class TecnicasAnaliseController {
    private final TecnicasAnaliseService tecnicas;
    public TecnicasAnaliseController(TecnicasAnaliseService tecnicas) { this.tecnicas = tecnicas; }

    @GetMapping("/analise")
    @Operation(summary = "Consultar prós e contras e matriz GUT", description = "Etapa 6. GUT ordenada por G × U × T decrescente, desempate pelo ID.")
    public ResponseEntity<AnaliseResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long id) {
        return resposta(200, tecnicas.consultar(usuario.id(), id));
    }

    @PostMapping("/opcoes/{opcaoId}/pros-contras")
    @Operation(summary = "Adicionar pró ou contra à alternativa")
    public ResponseEntity<Argumento> adicionarArgumento(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @PathVariable Long opcaoId, @Valid @RequestBody ProContraRequest request) {
        return resposta(201, tecnicas.salvarArgumento(usuario.id(), id, opcaoId, null, request));
    }

    @PutMapping("/opcoes/{opcaoId}/pros-contras/{registroId}")
    @Operation(summary = "Editar pró ou contra")
    public ResponseEntity<Argumento> editarArgumento(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @PathVariable Long opcaoId, @PathVariable Long registroId,
            @Valid @RequestBody ProContraRequest request) {
        return resposta(200, tecnicas.salvarArgumento(usuario.id(), id, opcaoId, registroId, request));
    }

    @DeleteMapping("/opcoes/{opcaoId}/pros-contras/{registroId}")
    @Operation(summary = "Excluir pró ou contra")
    public ResponseEntity<Void> excluirArgumento(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @PathVariable Long opcaoId, @PathVariable Long registroId) {
        tecnicas.excluirArgumento(usuario.id(), id, opcaoId, registroId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/gut")
    @Operation(summary = "Adicionar fator GUT", description = "Notas inteiras de 1 a 5. Prioridade calculada no servidor, de 1 a 125.")
    public ResponseEntity<FatorGut> adicionarFator(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @Valid @RequestBody ItemGutRequest request) {
        return resposta(201, tecnicas.salvarFator(usuario.id(), id, null, request));
    }

    @PutMapping("/gut/{registroId}")
    @Operation(summary = "Editar fator e recalcular prioridade GUT")
    public ResponseEntity<FatorGut> editarFator(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @PathVariable Long registroId, @Valid @RequestBody ItemGutRequest request) {
        return resposta(200, tecnicas.salvarFator(usuario.id(), id, registroId, request));
    }

    @DeleteMapping("/gut/{registroId}")
    @Operation(summary = "Excluir fator GUT")
    public ResponseEntity<Void> excluirFator(@AuthenticationPrincipal UsuarioResponse usuario,
            @PathVariable Long id, @PathVariable Long registroId) {
        tecnicas.excluirFator(usuario.id(), id, registroId);
        return ResponseEntity.noContent().build();
    }

    private <T> ResponseEntity<T> resposta(int status, T corpo) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(corpo);
    }
}
