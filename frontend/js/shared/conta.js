/** Cabeçalho reutilizável pelas etapas que usam o cliente autenticado. */
export function configurarConta({ requisitar, aviso, podeSair, aoSair, ocupado, rotas }) {
    const el = id => document.getElementById(id);
    function fechar() { el("perfil-menu").hidden = true; el("perfil-botao").setAttribute("aria-expanded", "false"); }
    el("perfil-botao").addEventListener("click", () => {
        const aberto = el("perfil-menu").hidden;
        el("perfil-menu").hidden = !aberto; el("perfil-botao").setAttribute("aria-expanded", String(aberto));
    });
    document.addEventListener("click", evento => { if (!evento.target.closest(".home-account")) fechar(); });
    document.addEventListener("keydown", evento => {
        if (evento.key === "Escape" && !el("perfil-menu").hidden) { fechar(); el("perfil-botao").focus(); }
    });
    el("historico").addEventListener("click", () => {
        const seguir = () => {
            if (rotas.historico) window.location.assign(rotas.historico);
            else aviso("Em breve", "O Histórico estará disponível em breve. Suas decisões recentes continuam disponíveis na Home.");
        };
        if (rotas.historico && window.BussolaAlteracoes) window.BussolaAlteracoes.permitir(el("historico"), seguir);
        else seguir();
    });
    el("perfil-menu").querySelector("[data-destino='perfil']").addEventListener("click", () => {
        const seguir = () => {
            fechar();
            if (rotas.perfil) window.location.assign(rotas.perfil);
            else aviso("Em breve", "A edição do perfil estará disponível em breve.");
        };
        if (rotas.perfil && window.BussolaAlteracoes) window.BussolaAlteracoes.permitir(el("perfil-botao"), seguir);
        else seguir();
    });
    el("sair").addEventListener("click", async () => {
        const seguir = () => {
            if (!podeSair()) return;
            encerrar();
        };
        if (window.BussolaAlteracoes) {
            window.BussolaAlteracoes.permitir(el("sair"), seguir);
            return;
        }
        seguir();
    });
    async function encerrar() {
        ocupado(true);
        try { await requisitar("/auth/logout", "POST"); aoSair(); sessionStorage.removeItem("bussola.sessao"); window.location.assign("login.html"); }
        catch { aviso("Não foi possível sair", "Confira a conexão e tente novamente."); }
        finally { ocupado(false); }
    }
    return usuario => {
        const nome = usuario.nomeCompleto.trim();
        el("usuario-menu").textContent = nome.split(/\s+/)[0];
        el("usuario-inicial").textContent = Array.from(nome)[0]?.toLocaleUpperCase("pt-BR") || "?";
        el("perfil-nome").textContent = nome; el("perfil-email").textContent = usuario.email;
        el("perfil-botao").disabled = false;
    };
}
