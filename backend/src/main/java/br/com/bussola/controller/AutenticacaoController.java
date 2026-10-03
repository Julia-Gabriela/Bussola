package br.com.bussola.controller;

import br.com.bussola.dto.request.LoginRequest;
import br.com.bussola.dto.response.LoginResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {
    private final AutenticacaoService autenticacao;

    public AutenticacaoController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar com e-mail e senha")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(autenticacao.login(request.email(), request.senha()));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar o usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse usuarioAtual(@AuthenticationPrincipal UsuarioResponse usuario) {
        return usuario;
    }
}
