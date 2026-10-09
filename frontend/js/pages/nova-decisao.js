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

    function abrirBrainstorming() {
        bloqueado = true;
        window.location.replace(`brainstorming.html?decisaoId=${encodeURIComponent(id)}`);
    }

    formulario.addEventListener("submit", async evento => {
        evento.preventDefault();
        if (ocupado || bloqueado) return;
        titulo.setCustomValidity(titulo.value.trim() ? "" : "Descreva sua decisão.");
        if (!formulario.reportValidity()) return;
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
            if (evento.submitter?.id === "salvar-depois") {
                mensagem.textContent = "Título e contexto salvos. Você pode continuar depois pela Home.";
            } else {
                await api.requisitar(`/decisoes/${id}/contexto/concluir`, "POST");
                abrirBrainstorming();
            }
        } catch (erro) {
            bloqueado = Boolean(erro.incerto || erro.status === 409 || api.encerrada);
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
        titulo.focus();
    }));

    async function carregar() {
        ocupado = true;
        campos.disabled = true;
        try {
            id = api.idDaUrl();
            const usuario = await api.requisitar("/auth/me");
            elemento("usuario-nome").textContent = usuario.nomeCompleto;
            if (id) {
                const decisao = await api.requisitar(`/decisoes/${id}`);
                if (decisao.etapaAtual >= 2) { abrirBrainstorming(); return; }
                titulo.value = decisao.titulo;
                contexto.value = decisao.contexto || "";
                bloqueado = decisao.status !== "EM_ANDAMENTO";
                elemento("exemplos").hidden = true;
            }
            mensagem.textContent = bloqueado ? "Esta decisão está disponível apenas para consulta." : "";
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
