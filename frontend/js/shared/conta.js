/**
 * Cabeçalho reutilizável pelas etapas que usam o cliente autenticado.
 *
 * Liga o menu de perfil (abrir/fechar por clique, clique fora e Esc), o botão
 * Histórico, a opção "Meu perfil" e o botão Sair. Toda saída da página passa por
 * `confirmarSaida`; sem essa opção, usa `BussolaAlteracoes.permitir` (aviso de
 * alterações não salvas) quando o módulo está carregado, ou segue direto.
 *
 * @param {object} opcoes
 * @param {Function} opcoes.requisitar cliente autenticado da API (usado no logout)
 * @param {(titulo: string, texto: string) => void} opcoes.aviso mostra um aviso na página
 * @param {() => boolean} opcoes.podeSair diz se o logout pode começar agora
 * @param {() => void} opcoes.aoSair executado depois do logout bem-sucedido
 * @param {(valor: boolean) => void} opcoes.ocupado liga/desliga o estado ocupado durante o logout
 * @param {{historico?: string, perfil?: string}} opcoes.rotas páginas de destino
 * @param {(origem: HTMLElement, seguir: () => void) => void} [opcoes.confirmarSaida] confirmação
 *        própria da página antes de sair; deve chamar `seguir` só quando a saída for permitida
 * @returns {(usuario: {nomeCompleto: string, email: string}) => void} função que preenche
 *          o avatar, o nome e o painel com os dados do usuário e habilita o menu
 */
export function configurarConta({ requisitar, aviso, podeSair, aoSair, ocupado, rotas, confirmarSaida }) {
    rotas = { ...rotas, perfil: rotas?.perfil || "perfil.html" };
    const el = id => document.getElementById(id);
    const permitir = confirmarSaida || ((origem, seguir) => {
        if (window.BussolaAlteracoes) window.BussolaAlteracoes.permitir(origem, seguir);
        else seguir();
    });
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
        if (rotas.historico) permitir(el("historico"), seguir);
        else seguir();
    });
    el("perfil-menu").querySelector("[data-destino='perfil']").addEventListener("click", () => {
        const seguir = () => {
            fechar();
            if (rotas.perfil) window.location.assign(rotas.perfil);
            else aviso("Em breve", "A edição do perfil estará disponível em breve.");
        };
        if (rotas.perfil) { fechar(); permitir(el("perfil-botao"), seguir); }
        else seguir();
    });
    el("sair").addEventListener("click", () => {
        permitir(el("sair"), () => {
            if (!podeSair()) return;
            encerrar();
        });
    });
    async function encerrar() {
        ocupado(true);
        try {
            await requisitar("/auth/logout", "POST"); aoSair(); sessionStorage.removeItem("bussola.sessao");
            if (window.BussolaAlteracoes) window.BussolaAlteracoes.navegar("login.html");
            else window.location.assign("login.html");
        }
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
