/*
 * Preferências de acessibilidade do Bússola: tamanho do texto, alto contraste
 * e redução de animações.
 *
 * Este arquivo é carregado de forma síncrona no <head> de todas as páginas.
 * Primeiro aplica as preferências salvas no elemento <html> (antes de o corpo
 * ser desenhado, para evitar mudança brusca de aparência). Depois que o DOM
 * fica pronto, coloca o botão "Abrir preferências de acessibilidade" no
 * cabeçalho (ou num canto da tela, nas páginas sem cabeçalho) e cria o painel,
 * um <dialog> modal nativo.
 *
 * As preferências ficam em localStorage, na chave `bussola.acessibilidade`.
 * Se o armazenamento estiver indisponível, elas valem até a página ser fechada
 * e o painel avisa isso.
 */
(() => {
    "use strict";

    const CHAVE = "bussola.acessibilidade";
    const PADRAO = Object.freeze({ texto: 100, contraste: false, movimento: false });
    const MINIMO = 100, MAXIMO = 150, PASSO = 10;
    const raiz = document.documentElement;
    let armazenamentoDisponivel = true;

    /**
     * Garante que um objeto lido do armazenamento tenha o formato esperado.
     *
     * Valores ausentes, de outro tipo ou fora do intervalo voltam ao padrão. O
     * tamanho do texto é arredondado para o múltiplo de 10 mais próximo entre
     * 100 e 150.
     *
     * @param {*} bruto valor lido do localStorage, já convertido de JSON
     * @returns {{texto: number, contraste: boolean, movimento: boolean}} preferências válidas
     */
    function normalizar(bruto) {
        const dados = bruto && typeof bruto === "object" ? bruto : {};
        const texto = Number.isFinite(dados.texto)
            ? Math.min(MAXIMO, Math.max(MINIMO, Math.round(dados.texto / PASSO) * PASSO)) : PADRAO.texto;
        return { texto, contraste: dados.contraste === true, movimento: dados.movimento === true };
    }

    /**
     * Lê as preferências salvas.
     *
     * Quando o navegador bloqueia o localStorage (modo privado restrito,
     * política da empresa etc.), marca o armazenamento como indisponível e
     * devolve o padrão, sem lançar erro.
     *
     * @returns {{texto: number, contraste: boolean, movimento: boolean}} preferências atuais
     */
    function ler() {
        try {
            const texto = window.localStorage.getItem(CHAVE);
            return normalizar(texto ? JSON.parse(texto) : null);
        } catch (_) {
            armazenamentoDisponivel = false;
            return { ...PADRAO };
        }
    }

    /**
     * Grava as preferências no localStorage.
     *
     * Preferências iguais ao padrão apagam a chave, para que "Restaurar padrão"
     * deixe o navegador como estava antes. Falhas de gravação apenas marcam o
     * armazenamento como indisponível; a página continua funcionando.
     *
     * @param {{texto: number, contraste: boolean, movimento: boolean}} preferencias valores a salvar
     * @returns {boolean} true quando a gravação funcionou
     */
    function salvar(preferencias) {
        try {
            const padrao = preferencias.texto === PADRAO.texto && !preferencias.contraste && !preferencias.movimento;
            if (padrao) window.localStorage.removeItem(CHAVE);
            else window.localStorage.setItem(CHAVE, JSON.stringify(preferencias));
            armazenamentoDisponivel = true;
            return true;
        } catch (_) {
            armazenamentoDisponivel = false;
            return false;
        }
    }

    /**
     * Aplica as preferências ao elemento <html>.
     *
     * O tamanho do texto vira `font-size` em porcentagem da raiz, então
     * respeita também o tamanho padrão escolhido no navegador (100% do
     * navegador × 120% do painel, por exemplo). Alto contraste e redução de
     * animações viram atributos `data-a11y-*`, usados por `acessibilidade.css`.
     * O tamanho também vai para `data-a11y-texto` (só acima de 100%), para o
     * CSS reorganizar o cabeçalho mais cedo quando o texto está ampliado.
     *
     * @param {{texto: number, contraste: boolean, movimento: boolean}} preferencias valores a aplicar
     * @returns {void}
     */
    function aplicar(preferencias) {
        raiz.style.fontSize = preferencias.texto === PADRAO.texto ? "" : preferencias.texto + "%";
        if (preferencias.texto > PADRAO.texto) raiz.dataset.a11yTexto = String(preferencias.texto); else delete raiz.dataset.a11yTexto;
        if (preferencias.contraste) raiz.dataset.a11yContraste = "alto"; else delete raiz.dataset.a11yContraste;
        if (preferencias.movimento) raiz.dataset.a11yMovimento = "reduzido"; else delete raiz.dataset.a11yMovimento;
    }

    let atuais = ler();
    aplicar(atuais);

    /** Ícone do botão: silhueta de pessoa (símbolo universal de acessibilidade), decorativo. */
    const ICONE = '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false">'
        + '<circle cx="12" cy="12" r="10.5" fill="none" stroke="currentColor" stroke-width="1.6"/>'
        + '<circle cx="12" cy="6.6" r="1.7" fill="currentColor"/>'
        + '<path d="M6.8 9.4 12 10.5l5.2-1.1M12 10.5v3.6m0 0-2.6 4.6m2.6-4.6 2.6 4.6" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>';

    /**
     * Cria um elemento com classe e texto opcionais.
     *
     * @param {string} tag nome da tag HTML
     * @param {string} [classe] classes CSS separadas por espaço
     * @param {string} [texto] conteúdo de texto
     * @returns {HTMLElement} elemento criado, ainda fora do documento
     */
    function criar(tag, classe, texto) {
        const no = document.createElement(tag);
        if (classe) no.className = classe;
        if (texto !== undefined) no.textContent = texto;
        return no;
    }

    /**
     * Monta o painel de preferências como um <dialog> modal.
     *
     * O diálogo nativo já prende o foco dentro dele enquanto está aberto,
     * fecha com Esc e deixa o restante da página inerte. Os controles são
     * nativos: botões para diminuir e aumentar o texto (desativados nos
     * limites), um <output> com o percentual atual e caixas de seleção com
     * papel de interruptor para alto contraste e redução de animações.
     *
     * @returns {{dialogo: HTMLDialogElement, atualizar: function(): void}} o diálogo e a função que sincroniza os controles com as preferências
     */
    function montarPainel() {
        const dialogo = criar("dialog", "a11y-painel");
        dialogo.id = "a11y-painel";
        dialogo.setAttribute("aria-labelledby", "a11y-titulo");
        dialogo.setAttribute("aria-describedby", "a11y-descricao");

        const cabecalho = criar("div", "a11y-painel-topo");
        const titulo = criar("h2", "", "Acessibilidade"); titulo.id = "a11y-titulo";
        const fechar = criar("button", "a11y-fechar"); fechar.type = "button";
        fechar.setAttribute("aria-label", "Fechar preferências de acessibilidade");
        fechar.innerHTML = '<span aria-hidden="true">×</span>';
        cabecalho.append(titulo, fechar);
        const descricao = criar("p", "a11y-descricao", "Ajuste a leitura da tela. As escolhas valem para todas as páginas do Bússola neste navegador.");
        descricao.id = "a11y-descricao";

        const grupoTexto = criar("div", "a11y-grupo");
        grupoTexto.setAttribute("role", "group");
        grupoTexto.setAttribute("aria-labelledby", "a11y-texto-rotulo");
        const rotuloTexto = criar("p", "a11y-rotulo", "Tamanho do texto"); rotuloTexto.id = "a11y-texto-rotulo";
        const linhaTexto = criar("div", "a11y-texto-linha");
        const menos = criar("button", "a11y-passo"); menos.type = "button";
        menos.innerHTML = '<span aria-hidden="true">A−</span>';
        menos.setAttribute("aria-label", "Diminuir texto");
        const valor = criar("output", "a11y-valor"); valor.id = "a11y-texto-valor";
        valor.setAttribute("aria-live", "polite");
        valor.setAttribute("aria-label", "Tamanho atual do texto");
        const mais = criar("button", "a11y-passo"); mais.type = "button";
        mais.innerHTML = '<span aria-hidden="true">A+</span>';
        mais.setAttribute("aria-label", "Aumentar texto");
        linhaTexto.append(menos, valor, mais);
        grupoTexto.append(rotuloTexto, linhaTexto, criar("p", "a11y-dica", "De 100% a 150%, de 10 em 10. O zoom do navegador também funciona."));

        /**
         * Cria uma opção liga/desliga com rótulo e explicação.
         *
         * @param {string} id id da caixa de seleção
         * @param {string} rotulo texto do rótulo visível
         * @param {string} dica explicação curta, associada por aria-describedby
         * @returns {{bloco: HTMLElement, caixa: HTMLInputElement}} bloco pronto e a caixa de seleção
         */
        function interruptor(id, rotulo, dica) {
            const bloco = criar("div", "a11y-grupo a11y-opcao");
            const caixa = criar("input"); caixa.type = "checkbox"; caixa.id = id;
            caixa.setAttribute("role", "switch");
            caixa.setAttribute("aria-describedby", id + "-dica");
            const etiqueta = criar("label", "a11y-interruptor"); etiqueta.htmlFor = id;
            etiqueta.append(caixa, criar("span", "a11y-trilho"), criar("span", "a11y-rotulo", rotulo));
            const estado = criar("span", "a11y-estado"); estado.setAttribute("aria-hidden", "true");
            etiqueta.append(estado);
            const explicacao = criar("p", "a11y-dica", dica); explicacao.id = id + "-dica";
            bloco.append(etiqueta, explicacao);
            return { bloco, caixa };
        }

        const contraste = interruptor("a11y-contraste", "Alto contraste", "Textos mais claros, bordas mais visíveis e foco reforçado, mantendo as cores do Bússola.");
        const movimento = interruptor("a11y-movimento", "Reduzir animações", "Remove transições e movimentos que não são essenciais. Se o seu sistema já pede menos movimento, isso é respeitado automaticamente.");

        const aviso = criar("p", "a11y-aviso", "Seu navegador não permite guardar as preferências. Elas valem só até você sair desta página.");
        aviso.setAttribute("role", "status");
        aviso.hidden = true;

        const acoes = criar("div", "a11y-acoes");
        const restaurar = criar("button", "a11y-restaurar", "Restaurar padrão"); restaurar.type = "button";
        const concluir = criar("button", "a11y-concluir", "Concluir"); concluir.type = "button";
        acoes.append(restaurar, concluir);
        const anuncio = criar("p", "a11y-sr"); anuncio.setAttribute("role", "status");

        dialogo.append(cabecalho, descricao, grupoTexto, contraste.bloco, movimento.bloco, aviso, acoes, anuncio);

        /**
         * Sincroniza os controles do painel com as preferências atuais.
         *
         * @returns {void}
         */
        function atualizar() {
            valor.textContent = atuais.texto + "%";
            menos.disabled = atuais.texto <= MINIMO;
            mais.disabled = atuais.texto >= MAXIMO;
            contraste.caixa.checked = atuais.contraste;
            movimento.caixa.checked = atuais.movimento;
            dialogo.querySelectorAll(".a11y-interruptor").forEach(etiqueta => {
                etiqueta.querySelector(".a11y-estado").textContent = etiqueta.querySelector("input").checked ? "Ligado" : "Desligado";
            });
            aviso.hidden = armazenamentoDisponivel;
        }

        /**
         * Troca as preferências, aplica na página, salva e atualiza o painel.
         *
         * Se o botão que estava focado ficar desativado (limite de 100% ou
         * 150%), o foco passa para o botão do sentido oposto, para não se
         * perder.
         *
         * @param {Partial<{texto: number, contraste: boolean, movimento: boolean}>} mudanca valores alterados
         * @returns {void}
         */
        function mudar(mudanca) {
            atuais = normalizar({ ...atuais, ...mudanca });
            aplicar(atuais);
            salvar(atuais);
            atualizar();
            if (document.activeElement && document.activeElement.disabled) (document.activeElement === mais ? menos : mais).focus();
        }

        menos.addEventListener("click", () => mudar({ texto: atuais.texto - PASSO }));
        mais.addEventListener("click", () => mudar({ texto: atuais.texto + PASSO }));
        contraste.caixa.addEventListener("change", () => mudar({ contraste: contraste.caixa.checked }));
        movimento.caixa.addEventListener("change", () => mudar({ movimento: movimento.caixa.checked }));
        restaurar.addEventListener("click", () => {
            mudar({ ...PADRAO });
            anuncio.textContent = "";
            requestAnimationFrame(() => { anuncio.textContent = "Preferências restauradas: texto em 100%, alto contraste e redução de animações desligados."; });
        });
        fechar.addEventListener("click", () => dialogo.close());
        concluir.addEventListener("click", () => dialogo.close());
        dialogo.addEventListener("click", evento => { if (evento.target === dialogo) dialogo.close(); });
        dialogo.addEventListener("keydown", evento => {
            if (evento.key !== "Tab") return;
            const focaveis = [...dialogo.querySelectorAll("button:not(:disabled), input:not(:disabled)")];
            const primeiro = focaveis[0], ultimo = focaveis[focaveis.length - 1];
            if (evento.shiftKey && document.activeElement === primeiro) { evento.preventDefault(); ultimo.focus(); }
            else if (!evento.shiftKey && document.activeElement === ultimo) { evento.preventDefault(); primeiro.focus(); }
        });

        atualizar();
        return { dialogo, atualizar };
    }

    /**
     * Coloca o botão de acessibilidade no cabeçalho da página e liga o painel.
     *
     * Ordem de procura do lugar do botão: área da conta do cabeçalho
     * autenticado (`.home-account`), cabeçalho das páginas públicas
     * (`.header-inner`, antes do botão de entrar) e, nas telas sem cabeçalho
     * (login e cadastro), um botão fixo no canto superior direito. Ao fechar o
     * painel, o foco volta para o botão que o abriu.
     *
     * @returns {void}
     */
    function iniciar() {
        if (document.getElementById("a11y-botao")) return;
        const botao = criar("button", "a11y-botao"); botao.type = "button"; botao.id = "a11y-botao";
        botao.setAttribute("aria-label", "Abrir preferências de acessibilidade");
        botao.setAttribute("aria-haspopup", "dialog");
        botao.setAttribute("aria-controls", "a11y-painel");
        botao.innerHTML = ICONE + '<span class="a11y-botao-texto" aria-hidden="true">Acessibilidade</span>';

        const conta = document.querySelector(".home-account");
        const publico = document.querySelector(".header-inner");
        if (conta) conta.prepend(botao);
        else if (publico) publico.insertBefore(botao, publico.querySelector(".auth-button"));
        else {
            botao.classList.add("is-flutuante");
            const pular = document.querySelector(".a11y-pular");
            if (pular) pular.after(botao); else document.body.prepend(botao);
        }

        const { dialogo, atualizar } = montarPainel();
        document.body.append(dialogo);
        botao.addEventListener("click", () => {
            atuais = ler();
            atualizar();
            dialogo.showModal();
            dialogo.querySelector(".a11y-passo:not(:disabled)")?.focus();
        });
        dialogo.addEventListener("close", () => botao.focus());
    }

    /**
     * Mostra o erro de validação nativa de um campo de forma persistente e acessível.
     *
     * Os formulários do Bússola que usam `reportValidity()` (Nova decisão,
     * Perfil, Prós e Contras etc.) só exibiam o balão temporário do navegador.
     * Esta função marca o campo com `aria-invalid="true"`, escreve a mensagem
     * do navegador (`validationMessage`) num parágrafo visível logo depois do
     * campo e liga esse parágrafo ao campo por `aria-describedby`, sem apagar
     * descrições que o campo já tinha. O valor digitado não é alterado e o
     * foco continua sendo levado ao primeiro campo inválido pelo próprio
     * navegador. Campos que a página já controla (com `aria-invalid` próprio)
     * são ignorados.
     *
     * @param {Event} evento evento `invalid` disparado pelo navegador (capturado no documento)
     * @returns {void}
     */
    function marcarInvalido(evento) {
        const campo = evento.target;
        if (!(campo instanceof HTMLElement) || !campo.id) return;
        if (campo.hasAttribute("aria-invalid") && !campo.dataset.a11yErro) return;
        const idErro = campo.id + "-a11y-erro";
        let erro = document.getElementById(idErro);
        if (!erro) {
            erro = criar("p", "a11y-erro-campo");
            erro.id = idErro;
            const ancora = campo.type === "checkbox" && campo.closest("label") ? campo.closest("label") : campo;
            ancora.after(erro);
            const descricoes = (campo.getAttribute("aria-describedby") || "").split(/\s+/).filter(Boolean);
            campo.setAttribute("aria-describedby", [...descricoes, idErro].join(" "));
        }
        erro.textContent = campo.validationMessage;
        erro.hidden = false;
        campo.dataset.a11yErro = "1";
        campo.setAttribute("aria-invalid", "true");
    }

    /**
     * Retira a marcação de erro criada por `marcarInvalido` assim que o campo fica válido.
     *
     * Executada a cada digitação ou mudança. Como algumas páginas só limpam o
     * `setCustomValidity` no próprio evento `input`, a verificação espera o
     * próximo ciclo de tarefas antes de consultar `validity.valid`.
     *
     * @param {Event} evento evento `input` ou `change` capturado no documento
     * @returns {void}
     */
    function limparInvalido(evento) {
        const campo = evento.target;
        if (!(campo instanceof HTMLElement) || campo.dataset.a11yErro !== "1") return;
        setTimeout(() => {
            if (!campo.validity || !campo.validity.valid) return;
            campo.removeAttribute("aria-invalid");
            delete campo.dataset.a11yErro;
            const erro = document.getElementById(campo.id + "-a11y-erro");
            if (erro) { erro.textContent = ""; erro.hidden = true; }
        }, 0);
    }

    document.addEventListener("invalid", marcarInvalido, true);
    document.addEventListener("input", limparInvalido, true);
    document.addEventListener("change", limparInvalido, true);
    document.addEventListener("reset", evento => {
        evento.target.querySelectorAll?.("[data-a11y-erro]").forEach(campo => {
            campo.removeAttribute("aria-invalid");
            delete campo.dataset.a11yErro;
            const erro = document.getElementById(campo.id + "-a11y-erro");
            if (erro) { erro.textContent = ""; erro.hidden = true; }
        });
    }, true);

    /**
     * Devolve o foco ao controle que o perdeu por ter sido desativado durante um salvamento.
     *
     * As telas desativam botões e campos enquanto conversam com a API
     * (`disabled` no próprio controle ou no `fieldset`). O navegador tira o
     * foco de um controle desativado e o deixa no `body`, sem aviso: quem usa
     * teclado ou leitor de tela volta ao início da página. Este observador
     * lembra o último controle focado; se ele for desativado com o foco, marca
     * a perda e, quando o controle volta a ficar ativo e visível, devolve o
     * foco a ele. Se a página ou a pessoa colocar o foco em outro lugar nesse
     * meio-tempo, nada é feito.
     *
     * @returns {void}
     */
    function preservarFocoAoDesativar() {
        let ultimo = null;
        let perdido = false;
        document.addEventListener("focusin", evento => { ultimo = evento.target; perdido = false; });
        new MutationObserver(() => {
            if (!ultimo || !ultimo.isConnected) return;
            const ativo = document.activeElement;
            const noBody = ativo === document.body || ativo === null;
            if (!noBody && ativo !== ultimo) { perdido = false; return; }
            if (ultimo.matches(":disabled")) { perdido = true; return; }
            if (noBody && perdido && ultimo.getClientRects().length > 0) {
                perdido = false;
                ultimo.focus({ preventScroll: true });
            }
        }).observe(document.documentElement, { subtree: true, attributes: true, attributeFilter: ["disabled"] });
    }
    preservarFocoAoDesativar();

    window.addEventListener("storage", evento => {
        if (evento.key !== CHAVE && evento.key !== null) return;
        atuais = ler();
        aplicar(atuais);
    });

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", iniciar);
    else iniciar();

    window.BussolaAcessibilidade = { ler, aplicar };
})();
