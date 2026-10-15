/**
 * Indicador de progresso com revisão das etapas já concluídas.
 *
 * Etapas futuras não navegam. A etapa salva continua alcançável e, se a pessoa
 * estiver numa tela anterior, o clique volta para ela sem o popup. Uma etapa
 * anterior abre a confirmação e só então carrega a tela com `revisao=1`.
 * Na revisão, a página aberta recebe o destaque visual; a etapa salva no
 * backend fica marcada como alcançada, sem parecer a página aberta.
 * Abrir ou cancelar o popup não altera a decisão.
 */
(() => {
    "use strict";

    const PASSOS = [
        { etapa: 2, numero: "1", nome: "Brainstorm", pagina: "brainstorming.html" },
        { etapa: 3, numero: "2", nome: "Opções", pagina: "opcoes.html" },
        { etapa: 4, numero: "3", nome: "Critérios", pagina: "criterios.html" },
        { etapa: 5, numero: "4", nome: "Pesos", pagina: null },
        { etapa: 6, numero: "5", nome: "Análise", pagina: "analise.html" }
    ];

    let dialogo;
    let abridor = null;
    let aoConfirmar = null;

    /**
     * Diz se a URL pediu revisão e a decisão já passou desta tela.
     * Na etapa corrente o parâmetro é ignorado, para o aviso não aparecer
     * num acesso normal.
     *
     * @param {number} etapaAtual etapa persistida da decisão
     * @param {number} etapaPagina etapa desta tela
     * @returns {boolean} true somente na revisão de uma etapa anterior em andamento
     */
    function emRevisao(etapaAtual, etapaPagina) {
        const pedida = new URLSearchParams(window.location.search).get("revisao") === "1";
        return pedida && Number.isInteger(etapaAtual) && etapaAtual > etapaPagina;
    }

    /**
     * Cria o popup “Revisar esta etapa?” uma única vez.
     *
     * @returns {HTMLDialogElement} diálogo de confirmação
     */
    function criarDialogo() {
        const elemento = document.createElement("dialog");
        elemento.className = "revisao-etapa";
        elemento.setAttribute("aria-labelledby", "revisao-etapa-titulo");
        elemento.setAttribute("aria-describedby", "revisao-etapa-texto");
        elemento.innerHTML = `
            <h2 id="revisao-etapa-titulo">Revisar esta etapa?</h2>
            <p id="revisao-etapa-texto">Você poderá alterar suas respostas. Algumas informações e resultados das etapas seguintes poderão ser atualizados.</p>
            <form method="dialog" class="revisao-etapa-acoes">
                <button type="submit" value="cancelar">Cancelar</button>
                <button type="submit" value="confirmar" class="revisao-etapa-continuar">Continuar revisão</button>
            </form>
        `;
        document.addEventListener("keydown", (evento) => {
            if (evento.key !== "Escape" || !elemento.open) return;
            evento.preventDefault();
            elemento.close("cancelar");
        }, true);
        elemento.addEventListener("close", () => {
            const confirmar = elemento.returnValue === "confirmar";
            const destino = aoConfirmar;
            const origem = abridor;
            aoConfirmar = null;
            abridor = null;
            if (confirmar && destino) destino();
            else if (origem) origem.focus();
        });
        document.body.append(elemento);
        return elemento;
    }

    /**
     * Abre a confirmação e, só se a pessoa continuar, executa a navegação.
     *
     * @param {HTMLElement} origem botão que abriu o popup, para devolver o foco
     * @param {() => void} destino navegação executada apenas após a confirmação
     * @returns {void}
     */
    function confirmar(origem, destino) {
        if (!dialogo) dialogo = criarDialogo();
        if (dialogo.open) return;
        abridor = origem;
        aoConfirmar = destino;
        dialogo.returnValue = "";
        dialogo.showModal();
        dialogo.querySelector("[value='cancelar']").focus();
    }

    /**
     * Monta um passo do indicador, clicável apenas quando a regra permite.
     *
     * @param {{etapa: number, numero: string, nome: string, pagina: string|null}} passo definição do passo
     * @param {number} etapaAtual etapa persistida
     * @param {string} status status da decisão
     * @param {number} etapaPagina etapa da tela aberta
     * @param {string} decisaoId identificador da decisão na URL
     * @returns {HTMLLIElement} item do indicador
     */
    function criarItem(passo, etapaAtual, status, etapaPagina, decisaoId) {
        const item = document.createElement("li");
        const circulo = document.createElement("span");
        circulo.className = "etapa-circulo";
        const nome = document.createElement("span");
        nome.textContent = passo.nome;
        const concluida = passo.etapa < etapaAtual;
        const atual = passo.etapa === etapaAtual;
        const emAndamento = status === "EM_ANDAMENTO";
        const revisaoAtiva = emAndamento && emRevisao(etapaAtual, etapaPagina);
        const revisandoEstePasso = revisaoAtiva && passo.etapa === etapaPagina;
        if (revisandoEstePasso) {
            item.className = "visualizada";
            item.setAttribute("aria-current", "page");
            circulo.textContent = passo.numero;
            circulo.setAttribute("aria-label", "Página aberta");
        } else if (concluida) {
            item.className = "concluida";
            circulo.textContent = "✓";
            circulo.setAttribute("aria-label", "Concluído");
        } else if (atual && revisaoAtiva) {
            item.className = "progresso";
            circulo.textContent = passo.numero;
            circulo.setAttribute("aria-label", "Etapa alcançada");
        } else if (atual) {
            item.className = "atual";
            item.setAttribute("aria-current", "step");
            circulo.textContent = passo.numero;
        } else {
            item.className = "futura";
            circulo.textContent = passo.numero;
        }

        const podeRevisar = emAndamento && concluida && passo.pagina && !revisandoEstePasso;
        const podeVoltar = emAndamento && atual && passo.pagina && passo.etapa !== etapaPagina;
        if (!podeRevisar && !podeVoltar) {
            item.append(circulo, nome);
            if (!concluida && !atual) item.setAttribute("aria-disabled", "true");
            return item;
        }

        const botao = document.createElement("button");
        botao.type = "button";
        botao.className = "etapa-acesso";
        botao.append(circulo, nome);
        if (podeVoltar) {
            botao.setAttribute("aria-label", `Voltar para ${passo.nome}`);
            botao.addEventListener("click", () => {
                const url = new URL(passo.pagina, window.location.href);
                url.searchParams.set("decisaoId", decisaoId);
                const seguir = () => window.location.assign(url.href);
                if (window.BussolaAlteracoes) window.BussolaAlteracoes.permitir(botao, seguir);
                else seguir();
            });
        } else {
            botao.setAttribute("aria-label", `Revisar ${passo.nome}`);
            botao.addEventListener("click", () => {
                const url = new URL(passo.pagina, window.location.href);
                url.searchParams.set("decisaoId", decisaoId);
                url.searchParams.set("revisao", "1");
                const seguir = () => confirmar(botao, () => window.location.assign(url.href));
                if (window.BussolaAlteracoes) window.BussolaAlteracoes.permitir(botao, seguir);
                else seguir();
            });
        }
        item.append(botao);
        return item;
    }

    /**
     * Atualiza o indicador e o aviso da revisão com o estado real da decisão.
     *
     * @param {{etapaAtual: number, status: string, etapaPagina: number, decisaoId: string}} estado dados da decisão aberta
     * @returns {{revisao: boolean}} true quando esta tela foi reaberta para edição
     */
    function sincronizar(estado) {
        const lista = document.getElementById("indicador-etapas");
        const aviso = document.getElementById("aviso-revisao");
        const revisao = emRevisao(estado.etapaAtual, estado.etapaPagina) && estado.status === "EM_ANDAMENTO";
        if (lista && Number.isInteger(estado.etapaAtual)) {
            lista.replaceChildren(...PASSOS.map((passo) => criarItem(
                passo, estado.etapaAtual, estado.status, estado.etapaPagina, String(estado.decisaoId)
            )));
        }
        if (aviso) aviso.hidden = !revisao;
        return { revisao };
    }

    window.BussolaRevisao = { emRevisao, sincronizar };
})();
