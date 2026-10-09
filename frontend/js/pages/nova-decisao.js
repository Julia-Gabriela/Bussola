(() => {
    "use strict";
    const origem = window.BUSSOLA_API_BASE || "http://localhost:8080";
    const formulario = document.getElementById("nova-decisao-form");
    const campos = document.getElementById("campos");
    const titulo = document.getElementById("titulo");
    const mensagem = document.getElementById("mensagem");
    const salvar = document.getElementById("salvar");
    const decisaoId = new URLSearchParams(window.location.search).get("decisaoId");
    const retomando = decisaoId !== null;
    let sessao;
    let enviando = false;
    let criada = false;
    let sessaoEncerrada = false;
    let salvamentoIncerto = false;

    function encerrarSessao() {
        sessaoEncerrada = true;
        campos.disabled = true;
        sessionStorage.removeItem("bussola.sessao");
        window.location.replace("login.html?motivo=sessao-expirada");
    }

    try {
        sessao = JSON.parse(sessionStorage.getItem("bussola.sessao"));
    } catch { sessao = null; }
    if (!sessao?.accessToken || sessao.tokenType !== "Bearer") {
        encerrarSessao();
        return;
    }

    async function requisitar(caminho, opcoes = {}) {
        const resposta = await fetch(`${origem}${caminho}`, {
            ...opcoes,
            headers: { Accept: "application/json", Authorization: `Bearer ${sessao.accessToken}`, ...opcoes.headers },
            cache: "no-store",
            signal: AbortSignal.timeout(15000)
        });
        if (resposta.status === 401) {
            encerrarSessao();
            throw new Error("Sua sessão expirou. Entre novamente.");
        }
        return resposta;
    }

    document.querySelectorAll(".exemplos button").forEach((botao) => {
        botao.addEventListener("click", () => {
            if (campos.disabled) return;
            titulo.value = botao.textContent;
            titulo.setCustomValidity("");
            titulo.focus();
        });
    });
    titulo.addEventListener("input", () => titulo.setCustomValidity(""));

    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        if (enviando || criada || retomando || campos.disabled) return;
        titulo.setCustomValidity(titulo.value.trim() ? "" : "Descreva sua decisão.");
        if (!formulario.reportValidity()) return;
        const dados = { titulo: titulo.value.trim(), contexto: document.getElementById("contexto").value.trim() || null };
        enviando = true;
        campos.disabled = true;
        salvar.textContent = "Salvando…";
        mensagem.textContent = "";
        try {
            const resposta = await requisitar("/decisoes", {
                method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(dados)
            });
            if (resposta.status !== 201) {
                mensagem.textContent = resposta.status === 400
                    ? "Confira os campos: título de até 200 caracteres e contexto de até 16.000 caracteres."
                    : "Não foi possível salvar. Tente novamente mais tarde.";
                return;
            }
            criada = true;
            const decisao = await resposta.json();
            const url = new URL(window.location.href);
            url.searchParams.set("decisaoId", decisao.id);
            window.history.replaceState(null, "", url);
            mensagem.textContent = "Decisão criada! Ela já aparece na sua Home. As próximas etapas da análise estarão disponíveis em breve.";
            document.getElementById("ver-decisao").hidden = false;
            document.getElementById("exemplos").hidden = true;
            salvar.hidden = true;
        } catch {
            if (sessaoEncerrada) return;
            salvamentoIncerto = true;
            document.getElementById("ver-decisao").hidden = false;
            mensagem.textContent = "Não foi possível confirmar o salvamento. Confira sua Home antes de tentar novamente para evitar criar a mesma decisão duas vezes.";
        } finally {
            enviando = false;
            campos.disabled = criada || salvamentoIncerto || sessaoEncerrada;
            salvar.textContent = "Começar análise →";
        }
    });

    async function carregarDecisao() {
        document.title = "Sua decisão — Bússola";
        document.querySelector(".nova-decisao h1").textContent = "Sua decisão";
        document.querySelector(".introducao").textContent = "Confira a decisão e o contexto que você salvou.";
        document.getElementById("exemplos").hidden = true;
        salvar.hidden = true;
        titulo.value = "";
        document.getElementById("contexto").value = "";
        if (!/^[1-9]\d*$/.test(decisaoId)) {
            mensagem.textContent = "O link desta decisão é inválido. Volte ao início e selecione uma decisão.";
            return;
        }
        mensagem.textContent = "Carregando sua decisão…";
        try {
            const resposta = await requisitar(`/decisoes/${decisaoId}`);
            if (!resposta.ok) {
                mensagem.textContent = resposta.status === 404
                    ? "Decisão não encontrada para sua conta. Volte ao início e selecione outra decisão."
                    : "Não foi possível carregar a decisão. Recarregue a página para tentar novamente.";
                return;
            }
            const decisao = await resposta.json();
            titulo.value = decisao.titulo;
            document.getElementById("contexto").value = decisao.contexto || "";
            titulo.readOnly = true;
            document.getElementById("contexto").readOnly = true;
            campos.disabled = false;
            mensagem.textContent = `Etapa salva: ${decisao.etapaAtual}. Seu título e contexto estão preservados. A edição e as próximas etapas da análise ainda não estão disponíveis.`;
        } catch {
            if (sessaoEncerrada) return;
            mensagem.textContent = "Não foi possível carregar a decisão. Confira sua conexão e recarregue a página.";
        }
    }

    async function verificarSessao() {
        try {
            sessao = JSON.parse(sessionStorage.getItem("bussola.sessao"));
            if (!sessao?.accessToken || sessao.tokenType !== "Bearer") {
                encerrarSessao();
                return;
            }
            const resposta = await requisitar("/auth/me");
            if (!resposta.ok) throw new Error();
            const usuario = await resposta.json();
            document.getElementById("usuario-nome").textContent = usuario.nomeCompleto;
            if (retomando) {
                await carregarDecisao();
                return;
            }
            campos.disabled = criada || salvamentoIncerto;
            if (!criada && !salvamentoIncerto) mensagem.textContent = "";
        } catch {
            if (sessaoEncerrada) return;
            campos.disabled = true;
            mensagem.textContent = "Não foi possível verificar sua sessão. Confira se o backend está ligado e recarregue a página.";
        }
    }
    window.addEventListener("pageshow", (evento) => {
        if (evento.persisted) { campos.disabled = true; verificarSessao(); }
    });
    verificarSessao();
})();
