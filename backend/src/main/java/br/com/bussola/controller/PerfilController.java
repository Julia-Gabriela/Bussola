package br.com.bussola.controller;

import br.com.bussola.dto.request.AtualizarPerfilRequest;
import br.com.bussola.dto.request.AlterarSenhaRequest;
import br.com.bussola.dto.response.PerfilResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/perfil")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {
    private final PerfilService perfil;
    public PerfilController(PerfilService perfil) { this.perfil = perfil; }

    @GetMapping
    @Operation(summary = "Consultar o próprio perfil")
    public ResponseEntity<PerfilResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario) {
        return resposta(perfil.consultar(usuario.id()));
    }

    @PutMapping
    @Operation(summary = "Editar dados pessoais", description = "Mantém idade mínima de 16 anos. Alterar e-mail exige senha atual.")
    public ResponseEntity<PerfilResponse> atualizar(@AuthenticationPrincipal UsuarioResponse usuario,
            @Valid @RequestBody AtualizarPerfilRequest request) {
        return resposta(perfil.atualizar(usuario.id(), request));
    }

    @PutMapping("/senha")
    @Operation(summary = "Alterar senha", description = "Exige senha atual e confirmação da nova senha. Revoga todas as sessões; faça login novamente.")
    public ResponseEntity<Void> alterarSenha(@AuthenticationPrincipal UsuarioResponse usuario,
            @Valid @RequestBody AlterarSenhaRequest request) {
        perfil.alterarSenha(usuario.id(), request);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @DeleteMapping
    @Operation(summary = "Excluir a própria conta e seus dados", description = "Irreversível. Exige senha atual e confirmar=true; remove decisões, análises, sessões e tokens em cascata.")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioResponse usuario,
            @Valid @RequestBody br.com.bussola.dto.request.ExcluirContaRequest request) {
        perfil.excluir(usuario.id(), request);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private ResponseEntity<PerfilResponse> resposta(PerfilResponse dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
