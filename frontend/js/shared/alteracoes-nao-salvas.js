/**
 * Impede sair de uma etapa editável enquanto o que está na tela
 * for diferente do último salvamento bem-sucedido, ou enquanto um
 * salvamento ainda não terminou.
 *
 * O aviso só informa. Não descarta e não grava sozinho. Atualizar ou
 * fechar a aba continua com a proteção nativa do navegador.
 */
(() => {
    "use strict";

    const AVISO = "Alterações não salvas.";
    let consulta = null;
    let dialogo = null;
    let abridor = null;
    let historicoInstalado = false;

    /**
     * Diz se a saída deve ser impedida agora.
     *
     * @returns {boolean} true quando há edição não salva ou pedido em andamento
     */
    function impede() {
        if (!consulta) return false;
        if (typeof consulta.ocupado === "function" && consulta.ocupado()) return true;
        return typeof consulta.sujo === "function" && consulta.sujo();
    }

    /**
     * Cria o aviso “Alterações não salvas” uma única vez, no mesmo
     * visual do popup de revisão.
     *
     * @returns {HTMLDialogElement} diálogo já inserido na página
     */
    function criarDialogo() {
        const elemento = document.createElement("dialog");
        elemento.className = "revisao-etapa";
        elemento.setAttribute("aria-labelledby", "alteracoes-titulo");
        elemento.setAttribute("aria-describedby", "alteracoes-texto");
        elemento.innerHTML = `
            <h2 id="alteracoes-titulo">Alterações não salvas</h2>
            <p id="alteracoes-texto">Você fez alterações nesta etapa que ainda não foram salvas. Salve suas alterações antes de sair para não perder suas respostas.</p>
            <form method="dialog" class="revisao-etapa-acoes">
                <button type="submit" value="cancelar" class="revisao-etapa-continuar">Continuar editando</button>
            </form>
        `;
        document.addEventListener("keydown", (evento) => {
            if (evento.key !== "Escape" || !elemento.open) return;
            evento.preventDefault();
            evento.stopPropagation();
            elemento.close("cancelar");
        }, true);
        elemento.addEventListener("close", () => {
            const origem = abridor;
            abridor = null;
            if (!origem || typeof origem.focus !== "function") return;
            const devolver = () => { if (origem.isConnected) origem.focus(); };
            devolver();
            setTimeout(devolver, 0);
        });
        document.body.append(elemento);
        return elemento;
    }

    /**
     * Abre o aviso e devolve o foco ao controle que tentou sair.
     *
     * @param {HTMLElement|null} origem botão ou link que provocou a saída
     * @returns {void}
     */
    function abrir(origem) {
        if (!dialogo) dialogo = criarDialogo();
        if (dialogo.open) return;
        abridor = origem && origem.isConnected ? origem : null;
        dialogo.returnValue = "";
        dialogo.showModal();
        dialogo.querySelector("button").focus();
    }

    /**
     * Executa a navegação somente quando não há o que proteger.
     *
     * @param {HTMLElement|null} origem controle que pediu a saída, para recuperar o foco
     * @param {() => void} destino navegação executada apenas se a tela estiver salva e ociosa
     * @returns {boolean} true quando o destino foi executado
     */
    function permitir(origem, destino) {
        if (dialogo?.open) return false;
        if (!impede()) {
            destino();
            return true;
        }
        abrir(origem);
        return false;
    }

    /**
     * Recebe a consulta da tela aberta. Cada página informa como saber
     * se os campos editáveis mudaram e se ainda há um salvamento em curso.
     *
     * @param {{sujo: () => boolean, ocupado: () => boolean}} opcoes funções da tela atual
     * @returns {void}
     */
    function registrar(opcoes) {
        consulta = opcoes;
        if (historicoInstalado) return;
        historicoInstalado = true;
        history.pushState({ bussolaAlteracoes: 1 }, "");
        window.addEventListener("popstate", () => {
            if (!impede()) {
                historicoInstalado = false;
                history.back();
                return;
            }
            history.pushState({ bussolaAlteracoes: 1 }, "");
            abrir(document.activeElement instanceof HTMLElement ? document.activeElement : null);
        });
    }

    document.addEventListener("click", (evento) => {
        if (evento.defaultPrevented || evento.button !== 0) return;
        if (evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) return;
        const link = evento.target instanceof Element ? evento.target.closest("a[href]") : null;
        if (!link || link.target === "_blank" || link.hasAttribute("download")) return;
        let url;
        try { url = new URL(link.href, window.location.href); } catch { return; }
        if (url.origin !== window.location.origin) return;
        if (url.pathname === window.location.pathname && url.search === window.location.search) return;
        if (!impede()) return;
        evento.preventDefault();
        evento.stopPropagation();
        abrir(link);
    }, true);

    window.addEventListener("beforeunload", (evento) => {
        if (!impede()) return;
        evento.preventDefault();
        evento.returnValue = "";
    });

    /**
     * Atualiza o texto de alterações não salvas sem apagar outros avisos.
     *
     * @param {HTMLElement|null} destino parágrafo de mensagem da tela
     * @param {boolean} mudou true quando a comparação com o último salvamento difere
     * @returns {void}
     */
    function refletir(destino, mudou) {
        if (!destino) return;
        if (mudou) destino.textContent = "Alterações ainda não salvas.";
        else if (destino.textContent === "Alterações ainda não salvas.") destino.textContent = "";
    }

    window.BussolaAlteracoes = { registrar, permitir, refletir, AVISO };
})();
