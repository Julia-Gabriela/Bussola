package br.com.bussola.controller;

import br.com.bussola.dto.response.HistoricoResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.model.enums.StatusDecisao;
import br.com.bussola.service.HistoricoService;
import br.com.bussola.exception.HistoricoInvalidoException;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/decisoes")
@SecurityRequirement(name = "bearerAuth")
public class HistoricoController {
    private final HistoricoService historico;
    public HistoricoController(HistoricoService historico) { this.historico = historico; }

    @ExceptionHandler(HistoricoInvalidoException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(HistoricoInvalidoException erro) {
        return ResponseEntity.badRequest().body(Map.of("erro", erro.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido() {
        return ResponseEntity.badRequest().body(Map.of("erro", "Parâmetro de consulta inválido."));
    }

    @GetMapping
    @Operation(summary = "Pesquisar histórico pessoal", description = "Busca por título, status opcional e ordem recentes/antigas/titulo. Página começa em 0; tamanho de 1 a 50.")
    public ResponseEntity<HistoricoResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario,
            @RequestParam(defaultValue = "") String busca, @RequestParam(required = false) StatusDecisao status,
            @RequestParam(defaultValue = "recentes") String ordem, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(historico.consultar(usuario.id(), busca, status, ordem, pagina, tamanho));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir uma decisão e seus registros", description = "Irreversível. Exige confirmar=true e decisão pertencente ao usuário autenticado.")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioResponse usuario, @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean confirmar) {
        historico.excluir(usuario.id(), id, confirmar);
        return ResponseEntity.noContent().build();
    }
}
