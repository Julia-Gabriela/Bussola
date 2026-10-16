/**
 * Liga o cabeçalho autenticado já usado na Home nas telas de etapa
 * que carregam `decisao-api.js`.
 *
 * O HTML precisa repetir os controles da Home: marca, Início, Histórico,
 * botão de perfil, painel com nome, e-mail e “Meu perfil”, e o botão Sair.
 * O aviso de área indisponível usa o diálogo `#aviso-integracao`.
 *
 * Histórico abre a listagem, respeitando os rascunhos da etapa.
 * Meu perfil ainda apresenta o aviso de indisponibilidade.
 * Sair chama `POST /auth/logout` pelo cliente já existente,
 * apaga `bussola.sessao` e abre `login.html`.
 */
(() => {
    "use strict";

    const api = window.BussolaDecisaoApi;
    let saindo = false;

    /**
     * Localiza um elemento do cabeçalho pelo id.
     *
     * @param {string} id identificador do elemento no documento
     * @returns {HTMLElement} elemento encontrado
     */
    function elemento(id) {
        return document.getElementById(id);
    }

    /**
     * Fecha o painel de perfil e devolve o foco ao botão que o abriu,
     * quando o fechamento veio do teclado.
     *
     * @param {boolean} devolverFoco true para focar de novo o botão de perfil
     * @returns {void}
     */
    function fecharPerfil(devolverFoco) {
        const menu = elemento("perfil-menu");
        const botao = elemento("perfil-botao");
        if (!menu || !botao || menu.hidden) return;
        menu.hidden = true;
        botao.setAttribute("aria-expanded", "false");
        if (devolverFoco) botao.focus();
    }

    /**
     * Abre o diálogo compartilhado do cabeçalho.
     *
     * @param {string} titulo título curto do diálogo
     * @param {string} mensagem texto apresentado à pessoa
     * @returns {void}
     */
    function avisar(titulo, mensagem) {
        const dialogo = elemento("aviso-integracao");
        const texto = elemento("aviso-texto");
        if (!dialogo || !texto) return;
        elemento("aviso-titulo").textContent = titulo;
        texto.textContent = mensagem;
        fecharPerfil(false);
        if (!dialogo.open) dialogo.showModal();
    }

    /**
     * Preenche avatar, primeiro nome, nome completo e e-mail com o usuário
     * devolvido por `GET /auth/me` e libera o botão do menu.
     *
     * @param {{nomeCompleto?: string, email?: string}} usuario dados públicos da conta autenticada
     * @returns {void} nada; apenas atualiza o cabeçalho
     */
    function preencherUsuario(usuario) {
        const nomeCompleto = typeof usuario?.nomeCompleto === "string" ? usuario.nomeCompleto.trim() : "";
        const primeiroNome = nomeCompleto.split(/\s+/).filter(Boolean)[0] || "Minha conta";
        elemento("usuario-menu").textContent = primeiroNome;
        elemento("usuario-inicial").textContent = Array.from(primeiroNome)[0]?.toLocaleUpperCase("pt-BR") || "?";
        elemento("perfil-nome").textContent = nomeCompleto;
        elemento("perfil-email").textContent = typeof usuario?.email === "string" ? usuario.email : "";
        elemento("perfil-botao").disabled = false;
    }

    /**
     * Pede a saída da conta. Se a etapa tiver alteração não salva, o aviso
     * impede o logout e mantém a pessoa na tela.
     *
     * @returns {void}
     */
    function sair() {
        if (saindo) return;
        const seguir = () => { encerrarSessao(); };
        if (window.BussolaAlteracoes) {
            window.BussolaAlteracoes.permitir(elemento("sair"), seguir);
            return;
        }
        seguir();
    }

    /**
     * Envia o logout depois que a proteção de alterações não salvas autorizou a saída.
     *
     * @returns {Promise<void>} resolve depois da tentativa de encerrar a sessão
     */
    async function encerrarSessao() {
        if (saindo) return;
        saindo = true;
        const botao = elemento("sair");
        botao.disabled = true;
        fecharPerfil(false);
        try {
            await api.requisitar("/auth/logout", "POST");
            sessionStorage.removeItem("bussola.sessao");
            window.location.assign("login.html");
        } catch (_) {
            if (!api.encerrada) {
                avisar("Não foi possível sair", "Não foi possível encerrar sua sessão. Confira sua conexão e clique em Sair novamente.");
            }
        } finally {
            saindo = false;
            botao.disabled = false;
        }
    }

    elemento("perfil-botao").addEventListener("click", () => {
        const menu = elemento("perfil-menu");
        const aberto = menu.hidden;
        menu.hidden = !aberto;
        elemento("perfil-botao").setAttribute("aria-expanded", String(aberto));
    });
    document.addEventListener("click", (evento) => {
        if (!evento.target.closest(".home-account")) fecharPerfil(false);
    });
    document.addEventListener("keydown", (evento) => {
        if (evento.key === "Escape") fecharPerfil(true);
    });
    elemento("historico").addEventListener("click", () => {
        window.BussolaAlteracoes.permitir(elemento("historico"), () => window.location.assign("historico.html"));
    });
    elemento("perfil-menu").querySelector("[data-destino='perfil']").addEventListener("click", () => {
        window.BussolaAlteracoes.permitir(elemento("perfil-botao"), () => window.location.assign("perfil.html"));
    });
    elemento("sair").addEventListener("click", sair);

    window.BussolaCabecalho = { preencherUsuario };
})();
