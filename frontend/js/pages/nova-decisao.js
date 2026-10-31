(() => {
    "use strict";
    const api = window.BussolaDecisaoApi;
    const elemento = id => document.getElementById(id);
    const formulario = elemento("nova-decisao-form");
    const campos = elemento("campos");
    const titulo = elemento("titulo");
    const contexto = elemento("contexto");
    const mensagem = elemento("mensagem");
    let id;
    let ocupado = true;
    let bloqueado = false;
    let referencia = null;
    let redirecionando = false;
    const CHAVE_DEIXADA = "bussola.contextualizacao.redirecionada";

    /**
     * Fotografa título e contexto exatamente como estão nos campos.
     *
     * @returns {string} estado comparável da contextualização
     */
    function foto() {
        return JSON.stringify({ titulo: titulo.value, contexto: contexto.value });
    }

    /**
     * Indica se o texto atual difere do último carregamento ou salvamento.
     *
     * @returns {boolean} true quando ainda há texto não gravado
     */
    function sujo() {
        return referencia !== null && foto() !== referencia;
    }

    /**
     * Grava o estado visível como referência de “já salvo”.
     *
     * @returns {void}
     */
    function gravarReferencia() {
        referencia = foto();
        window.BussolaAlteracoes.refletir(mensagem, false);
    }

    titulo.addEventListener("input", () => window.BussolaAlteracoes.refletir(mensagem, sujo()));
    contexto.addEventListener("input", () => window.BussolaAlteracoes.refletir(mensagem, sujo()));

    function abrirBrainstorming() {
        abrirEtapa("brainstorming.html");
    }

    /**
     * Sai da Contextualização para a etapa salva, substituindo esta página no histórico.
     *
     * Usado quando a decisão já passou da etapa 1 (link antigo, favorito ou
     * conclusão da contextualização). Bloqueia os campos, informa que a etapa
     * está sendo aberta e marca na sessão que esta página foi deixada por
     * redirecionamento: se a pessoa voltar a ela pelo botão Voltar,
     * `pularSeVoltouAposRedirecionar` recua mais um passo em vez de mandá-la
     * de novo para frente (ou mostrar um formulário vazio).
     *
     * @param {string} pagina tela de destino relativa a frontend/pages/ (por exemplo "opcoes.html")
     * @returns {void}
     */
    function abrirEtapa(pagina) {
        bloqueado = true;
        ocupado = false;
        redirecionando = true;
        campos.disabled = true;
        mensagem.textContent = "Abrindo a etapa atual da decisão…";
        sessionStorage.setItem(CHAVE_DEIXADA, "1");
        window.location.replace(`${pagina}?decisaoId=${encodeURIComponent(id)}`);
    }

    /**
     * Trata a volta (botão Voltar/Avançar ou cache do navegador) a uma
     * Contextualização que já tinha redirecionado para outra etapa.
     *
     * Nesse caso recua mais um passo no histórico, mostrando "Voltando…".
     * Se não houver para onde recuar, abre a Home depois de um instante, para
     * a tela nunca ficar parada numa mensagem de carregamento. Num acesso
     * normal, apenas limpa a marca deixada por `abrirEtapa`.
     *
     * @param {boolean} voltou true quando a página foi aberta pelo histórico do navegador
     * @returns {boolean} true quando a página está recuando e o carregamento deve parar
     */
    function pularSeVoltouAposRedirecionar(voltou) {
        const deixada = sessionStorage.getItem(CHAVE_DEIXADA) === "1";
        sessionStorage.removeItem(CHAVE_DEIXADA);
        if (!voltou || !deixada) return false;
        redirecionando = true;
        bloqueado = true;
        campos.disabled = true;
        mensagem.textContent = "Voltando…";
        window.history.back();
        setTimeout(() => window.location.replace("home.html"), 1500);
        return true;
    }

    formulario.addEventListener("submit", async evento => {
        evento.preventDefault();
        if (ocupado || bloqueado || window.BussolaEtapas.aberta) return;
        titulo.setCustomValidity(titulo.value.trim() ? "" : "Descreva sua decisão.");
        if (!formulario.reportValidity()) return;
        const avancar = evento.submitter?.id !== "salvar-depois";
        if (avancar && !await window.BussolaEtapas.confirmarAvanco()) return;
        ocupado = true;
        campos.disabled = true;
        mensagem.textContent = "Salvando…";
        try {
            const dados = { titulo: titulo.value.trim(), contexto: contexto.value.trim() || null };
            const decisao = await api.requisitar(id ? `/decisoes/${id}/contexto` : "/decisoes", id ? "PUT" : "POST", dados);
            id = String(decisao.id);
            const url = new URL(window.location.href);
            url.searchParams.set("decisaoId", id);
            window.history.replaceState(null, "", url);
            elemento("exemplos").hidden = true;
            elemento("ver-decisao").hidden = false;
            gravarReferencia();
            if (!avancar) {
                mensagem.textContent = "Título e contexto salvos. Você pode continuar depois pela Home.";
            } else {
                await api.requisitar(`/decisoes/${id}/contexto/concluir`, "POST");
                abrirBrainstorming();
            }
        } catch (erro) {
            bloqueado = Boolean(api.encerrada);
            mensagem.textContent = erro.message;
            elemento("ver-decisao").hidden = false;
        } finally {
            ocupado = false;
            campos.disabled = bloqueado;
        }
    });
    titulo.addEventListener("input", () => titulo.setCustomValidity(""));
    document.querySelectorAll(".exemplos button").forEach(botao => botao.addEventListener("click", () => {
        if (ocupado || bloqueado) return;
        titulo.value = botao.textContent;
        titulo.setCustomValidity("");
        window.BussolaAlteracoes.refletir(mensagem, sujo());
        titulo.focus();
    }));

    async function carregar() {
        ocupado = true;
        campos.disabled = true;
        try {
            id = api.idDaUrl();
            const usuario = await api.requisitar("/auth/me");
            window.BussolaCabecalho.preencherUsuario(usuario);
            if (id) {
                const decisao = await api.requisitar(`/decisoes/${id}`);
                if (decisao.etapaAtual >= 3) {
                    abrirEtapa(decisao.etapaAtual === 6 ? "analise.html"
                        : decisao.etapaAtual === 5 ? "pesos.html"
                        : decisao.etapaAtual >= 4 ? "criterios.html" : "opcoes.html");
                    return;
                }
                if (decisao.etapaAtual === 2) { abrirBrainstorming(); return; }
                titulo.value = decisao.titulo;
                contexto.value = decisao.contexto || "";
                bloqueado = decisao.status !== "EM_ANDAMENTO";
                elemento("exemplos").hidden = true;
            }
            mensagem.textContent = bloqueado ? "Esta decisão está disponível apenas para consulta." : "";
            gravarReferencia();
        } catch (erro) {
            bloqueado = true;
            mensagem.textContent = erro.message;
        } finally {
            ocupado = false;
            campos.disabled = bloqueado;
            if (!redirecionando) window.BussolaAlteracoes.registrar({ sujo, ocupado: () => ocupado });
        }
    }
    window.addEventListener("pageshow", evento => {
        if (evento.persisted && !pularSeVoltouAposRedirecionar(true)) window.location.reload();
    });
    const navegacao = performance.getEntriesByType("navigation")[0];
    if (!pularSeVoltouAposRedirecionar(navegacao?.type === "back_forward")) carregar();
})();
