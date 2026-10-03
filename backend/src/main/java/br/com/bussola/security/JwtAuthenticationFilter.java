package br.com.bussola.security;

import br.com.bussola.service.AutenticacaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AutenticacaoService autenticacao;

    public JwtAuthenticationFilter(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return caminho.equals("/auth/login") || caminho.equals("/auth/cadastro")
                || caminho.startsWith("/swagger-ui") || caminho.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            try {
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() <= 7) {
                    throw new org.springframework.security.authentication.BadCredentialsException("Token inválido.");
                }
                var usuario = autenticacao.autenticar(header.substring(7));
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (AuthenticationException exception) {
                SecurityContextHolder.clearContext();
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"erro\":\"Sessão inválida ou expirada.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
