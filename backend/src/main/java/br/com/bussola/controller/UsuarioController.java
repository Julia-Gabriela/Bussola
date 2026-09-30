package br.com.bussola.controller;

import br.com.bussola.dto.request.CadastroUsuarioRequest;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.model.entity.Usuario;
import br.com.bussola.service.UsuarioService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    @Operation(summary = "Cadastrar usuário", description = "Exige idade mínima de 16 anos e aceite dos termos.")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroUsuarioRequest request) {
        Usuario usuario = usuarioService.cadastrar(request.nomeCompleto(), request.dataNascimento(),
                request.email(), request.senha(), request.aceitouTermos());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuario));
    }
}
