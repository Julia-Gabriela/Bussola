/**
 * Impede sair de uma etapa editável enquanto o que está na tela
 * for diferente do último salvamento bem-sucedido, ou enquanto um
 * salvamento ainda não terminou.
 *
 * O aviso não grava sozinho. Por padrão só informa; telas registradas
 * com `descartavel` também oferecem "Descartar alterações". Atualizar ou
 * fechar a aba continua com a proteção nativa do navegador.
 */
(() => {
    "use strict";

    const AVISO = "Alterações não salvas.";
    let consulta = null;
    let dialogo = null;
    let abridor = null;
    let pendenteSaida = null;
    let historicoInstalado = false;
    let navegacaoLiberada = false;
    let descartando = false;

    /**
     * Diz se o que está na tela difere do último salvamento.
     *
     * @returns {boolean} true quando há edição não salva
     */
    function sujo() {
        return !descartando && !!consulta && typeof consulta.sujo === "function" && consulta.sujo();
    }

    /**
     * Diz se ainda há um pedido ao servidor em andamento.
     *
     * @returns {boolean} true enquanto um salvamento ou carregamento não terminou
     */
    function ocupado() {
        return !descartando && !!consulta && typeof consulta.ocupado === "function" && consulta.ocupado();
    }

    /**
     * Diz se a saída deve ser impedida agora.
     *
     * @returns {boolean} true quando há edição não salva ou pedido em andamento
     */
    function impede() {
        return ocupado() || sujo();
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
            <p id="alteracoes-texto"></p>
            <form method="dialog" class="revisao-etapa-acoes">
                <button type="submit" value="descartar" hidden>Descartar alterações</button>
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
            const saida = pendenteSaida;
            pendenteSaida = null;
            if (elemento.returnValue === "descartar" && saida) {
                abridor = null;
                descartando = true;
                saida();
                return;
            }
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
     * Ajusta o texto e os botões do aviso às opções registradas pela tela atual.
     *
     * Roda a cada abertura (e não só na criação do diálogo), para que o aviso
     * siga sempre o último `registrar`. Telas sem `descartavel` mostram o texto
     * das etapas e apenas "Continuar editando"; telas com `descartavel` também
     * mostram "Descartar alterações". `mensagem`, quando informada, substitui o
     * texto padrão.
     *
     * @returns {void}
     */
    function configurarDialogo() {
        const descartavel = consulta?.descartavel === true;
        const mensagem = typeof consulta?.mensagem === "string" && consulta.mensagem.trim()
            ? consulta.mensagem
            : descartavel
                ? "Você tem alterações que ainda não foram salvas. Deseja continuar editando ou descartá-las?"
                : "Você fez alterações nesta etapa que ainda não foram salvas. Salve suas alterações antes de sair para não perder suas respostas.";
        dialogo.querySelector("#alteracoes-texto").textContent = mensagem;
        dialogo.querySelector("button[value='descartar']").hidden = !descartavel;
        dialogo.classList.toggle("revisao-etapa-descartavel", descartavel);
    }

    /**
     * Abre o aviso e devolve o foco ao controle que tentou sair.
     *
     * @param {HTMLElement|null} origem botão ou link que provocou a saída
     * @param {(() => void)|null} [saida=null] navegação executada se a pessoa escolher
     *        "Descartar alterações" (só existe em telas registradas com `descartavel`)
     * @returns {void}
     */
    function abrir(origem, saida = null) {
        if (!dialogo) dialogo = criarDialogo();
        if (dialogo.open) return;
        configurarDialogo();
        abridor = origem && origem.isConnected ? origem : null;
        pendenteSaida = saida;
        dialogo.returnValue = "";
        dialogo.showModal();
        dialogo.querySelector(".revisao-etapa-continuar").focus();
    }

    /**
     * Quando só falta terminar um salvamento (nada sujo), espera a tela
     * concluí-lo e tenta a saída de novo. Só vale para telas que registram
     * `aguardar`; nas demais, um pedido em andamento continua bloqueando.
     *
     * @param {HTMLElement|null} origem controle que pediu a saída
     * @param {() => void} destino navegação pedida
     * @returns {boolean} true quando a espera foi iniciada
     */
    function esperarSalvamento(origem, destino) {
        if (sujo() || !ocupado() || typeof consulta?.aguardar !== "function") return false;
        Promise.resolve(consulta.aguardar()).catch(() => {}).then(() => permitir(origem, destino));
        return true;
    }

    /**
     * Executa a navegação somente quando não há o que proteger.
     * Se ainda houver salvamento em andamento, aguarda a conclusão antes de sair
     * (nas telas que informam `aguardar`). Se o salvamento falhar e a tela ficar
     * com alterações não salvas, o aviso aparece e nada é perdido.
     *
     * @param {HTMLElement|null} origem controle que pediu a saída, para recuperar o foco
     * @param {() => void} destino navegação executada apenas se a tela estiver salva e ociosa
     * @returns {boolean} true quando o destino foi executado imediatamente
     */
    function permitir(origem, destino) {
        if (dialogo?.open) return false;
        if (!impede()) {
            destino();
            return true;
        }
        if (esperarSalvamento(origem, destino)) return false;
        abrir(origem, destino);
        return false;
    }

    /**
     * Navega para outra página logo depois de um salvamento confirmado pelo
     * servidor, sem disparar o aviso nativo "Sair do site?".
     *
     * A tela costuma ainda se considerar ocupada nesse instante, porque a
     * navegação acontece dentro do próprio fluxo de salvamento. Esse estado
     * é ignorado só para esta navegação; alterações realmente não salvas
     * continuam protegidas pelo navegador.
     *
     * @param {string} url endereço de destino (relativo ou absoluto)
     * @param {{substituir?: boolean}} [opcoes] `substituir: true` usa
     *        `location.replace`, sem deixar a página atual no histórico
     * @returns {void}
     */
    function navegar(url, { substituir = false } = {}) {
        navegacaoLiberada = true;
        if (substituir) window.location.replace(url);
        else window.location.assign(url);
    }

    /**
     * Recebe a consulta da tela aberta. Cada página informa como saber
     * se os campos editáveis mudaram e se ainda há um salvamento em curso.
     *
     * @param {{sujo: () => boolean, ocupado: () => boolean, aguardar?: () => Promise<unknown>, descartavel?: boolean, mensagem?: string}} opcoes
     *        funções da tela atual. `aguardar` devolve uma promessa resolvida quando
     *        os salvamentos em andamento terminam (com sucesso ou falha). `descartavel`
     *        acrescenta ao aviso a opção "Descartar alterações", que descarta só o que
     *        não foi salvo (nada é enviado ao servidor) e segue para o destino pedido.
     *        `mensagem` troca o texto do aviso por um específico da tela.
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
            abrir(document.activeElement instanceof HTMLElement ? document.activeElement : null, () => history.go(-2));
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
        const destino = () => window.location.assign(url.href);
        if (esperarSalvamento(link, destino)) return;
        abrir(link, destino);
    }, true);

    window.addEventListener("beforeunload", (evento) => {
        const liberada = navegacaoLiberada;
        navegacaoLiberada = false;
        if (liberada ? !sujo() : !impede()) return;
        evento.preventDefault();
        evento.returnValue = "";
    });

    window.addEventListener("pageshow", () => {
        navegacaoLiberada = false;
        descartando = false;
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

    window.BussolaAlteracoes = { registrar, permitir, navegar, refletir, AVISO };
})();
