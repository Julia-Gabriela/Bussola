/*
 * Ajuda contextual (nível 2): botão "?" que mostra uma explicação curta logo
 * abaixo do termo. Segue o padrão de divulgação (disclosure): o botão tem
 * aria-expanded e aria-controls apontando para o painel, que fica no fluxo da
 * página, logo depois do botão no DOM, para ser lido na ordem natural.
 *
 * Marcação esperada:
 *   <button type="button" class="ajuda-botao" aria-expanded="false" aria-controls="ajuda-x">
 *       <span aria-hidden="true">?</span><span class="ajuda-sr">O que é X?</span>
 *   </button>
 *   <div class="ajuda-painel" id="ajuda-x" hidden>…</div>
 *
 * Funciona também com botões criados depois do carregamento, porque os
 * eventos são delegados ao documento. Não guarda nada no servidor.
 */
(() => {
    "use strict";

    /**
     * Encontra o painel controlado por um botão de ajuda.
     *
     * @param {HTMLElement} botao botão com a classe `ajuda-botao`
     * @returns {(HTMLElement|null)} o elemento indicado em `aria-controls`, ou
     *          null quando o atributo falta ou aponta para um id inexistente
     */
    function painelDe(botao) {
        const id = botao.getAttribute("aria-controls");
        return id ? document.getElementById(id) : null;
    }

    /**
     * Abre ou fecha um painel de ajuda e mantém o botão coerente com o estado.
     *
     * Ao abrir, fecha os outros painéis abertos para que só uma explicação
     * apareça por vez. O foco continua no botão, como manda o padrão de
     * divulgação; ao fechar pelo Esc, `fechar` devolve o foco ao botão.
     *
     * @param {HTMLElement} botao botão com a classe `ajuda-botao`
     * @param {boolean} abrir true para mostrar o painel, false para escondê-lo
     * @returns {void}
     */
    function alternar(botao, abrir) {
        const painel = painelDe(botao);
        if (!painel) return;
        if (abrir) document.querySelectorAll(".ajuda-botao[aria-expanded='true']").forEach(outro => {
            if (outro !== botao) alternar(outro, false);
        });
        botao.setAttribute("aria-expanded", String(abrir));
        painel.hidden = !abrir;
    }

    /**
     * Fecha todos os painéis abertos.
     *
     * @param {(HTMLElement|null)} [focar=null] botão que deve receber o foco
     *        depois de fechar (usado no Esc); null mantém o foco onde está
     * @returns {void}
     */
    function fechar(focar = null) {
        document.querySelectorAll(".ajuda-botao[aria-expanded='true']").forEach(botao => alternar(botao, false));
        if (focar) focar.focus();
    }

    document.addEventListener("click", evento => {
        const botao = evento.target.closest(".ajuda-botao");
        if (botao) {
            alternar(botao, botao.getAttribute("aria-expanded") !== "true");
            return;
        }
        const fecharBotao = evento.target.closest(".ajuda-fechar");
        if (!fecharBotao) return;
        const painel = fecharBotao.closest(".ajuda-painel");
        const dono = painel && document.querySelector(`.ajuda-botao[aria-controls="${painel.id}"]`);
        if (dono) { alternar(dono, false); dono.focus(); }
    });

    document.addEventListener("keydown", evento => {
        if (evento.key !== "Escape") return;
        const aberto = document.querySelector(".ajuda-botao[aria-expanded='true']");
        if (!aberto) return;
        const painel = painelDe(aberto);
        const ativo = document.activeElement;
        const focoNaAjuda = ativo === aberto || (painel && painel.contains(ativo));
        const semFoco = !ativo || ativo === document.body;
        if (!focoNaAjuda && !semFoco) return;
        evento.preventDefault();
        fechar(aberto);
    });

    window.BussolaAjuda = { alternar, fechar };
})();
