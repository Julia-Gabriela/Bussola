/** Cabeçalho reutilizável pelas etapas que usam o cliente autenticado. */
export function configurarConta({ requisitar, aviso, podeSair, aoSair, ocupado, rotas }) {
    const el = id => document.getElementById(id);
    function fechar() { el("perfil-menu").hidden = true; el("perfil-botao").setAttribute("aria-expanded", "false"); }
    el("perfil-botao").addEventListener("click", () => {
        const aberto = el("perfil-menu").hidden;
        el("perfil-menu").hidden = !aberto; el("perfil-botao").setAttribute("aria-expanded", String(aberto));
    });
    document.addEventListener("click", evento => { if (!evento.target.closest(".home-account")) fechar(); });
    document.addEventListener("keydown", evento => { if (evento.key === "Escape") fechar(); });
    el("historico").addEventListener("click", () => {
        if (rotas.historico) window.location.assign(rotas.historico);
        else aviso("Histórico", "O histórico estará disponível em breve. Suas decisões recentes estão na Home.");
    });
    el("sair").addEventListener("click", async () => {
        if (!podeSair()) return;
        ocupado(true);
        try { await requisitar("/auth/logout", "POST"); aoSair(); sessionStorage.removeItem("bussola.sessao"); window.location.assign("login.html"); }
        catch { aviso("Não foi possível sair", "Confira a conexão e tente novamente."); }
        finally { ocupado(false); }
    });
    return usuario => {
        const nome = usuario.nomeCompleto.trim();
        el("usuario-menu").textContent = nome.split(/\s+/)[0];
        el("usuario-inicial").textContent = Array.from(nome)[0]?.toLocaleUpperCase("pt-BR") || "?";
        el("perfil-nome").textContent = nome; el("perfil-email").textContent = usuario.email;
        el("perfil-botao").disabled = false;
    };
}
