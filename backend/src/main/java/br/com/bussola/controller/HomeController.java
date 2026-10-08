package br.com.bussola.controller;

import br.com.bussola.dto.response.HomeResponse;
import br.com.bussola.dto.response.UsuarioResponse;
import br.com.bussola.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    private final HomeService home;

    public HomeController(HomeService home) {
        this.home = home;
    }

    @GetMapping("/home")
    @Operation(summary = "Consultar resumo da Home do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<HomeResponse> consultar(@AuthenticationPrincipal UsuarioResponse usuario) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(home.consultar(usuario));
    }
}
