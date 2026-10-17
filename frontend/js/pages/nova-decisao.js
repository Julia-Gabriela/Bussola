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

    window.BussolaAlteracoes.registrar({ sujo, ocupado: () => ocupado });
    titulo.addEventListener("input", () => window.BussolaAlteracoes.refletir(mensagem, sujo()));
    contexto.addEventListener("input", () => window.BussolaAlteracoes.refletir(mensagem, sujo()));

    function abrirBrainstorming() {
        bloqueado = true;
        ocupado = false;
        campos.disabled = true;
        window.location.replace(`brainstorming.html?decisaoId=${encodeURIComponent(id)}`);
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
                    const pagina = decisao.etapaAtual === 6 ? "analise.html"
                        : decisao.etapaAtual === 5 ? "pesos.html"
                        : decisao.etapaAtual >= 4 ? "criterios.html" : "opcoes.html";
                    bloqueado = true;
                    window.location.replace(pagina + "?decisaoId=" + encodeURIComponent(id));
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
        }
    }
    window.addEventListener("pageshow", evento => { if (evento.persisted) window.location.reload(); });
    carregar();
})();
