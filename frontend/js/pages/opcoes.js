(() => {
    "use strict";
    const el = id => document.getElementById(id);
    const origem = window.BUSSOLA_API_BASE || "http://localhost:8080";
    const chave = "bussola.sessao";
    const id = new URLSearchParams(window.location.search).get("decisaoId");
    const formulario = el("opcoes-form");
    const campos = el("opcoes-campos");
    let sessao, decisao, ocupado = false, alterado = false, encerrada = false;
    const inputs = [...formulario.querySelectorAll("input, textarea")];

    function encerrar() {
        encerrada = true;
        alterado = false;
        sessionStorage.removeItem(chave);
        window.location.replace("login.html?motivo=sessao-expirada");
    }

    function lerSessao() {
        try { sessao = JSON.parse(sessionStorage.getItem(chave)); } catch { sessao = null; }
        if (!sessao || sessao.tokenType !== "Bearer" || typeof sessao.accessToken !== "string" || !sessao.accessToken.trim()) {
            encerrar(); return false;
        }
        return true;
    }

    async function requisitar(caminho, method = "GET", dados) {
        const controlador = new AbortController();
        const timeout = setTimeout(() => controlador.abort(), 15000);
        try {
            const resposta = await fetch(origem + caminho, {
                method, cache: "no-store", signal: controlador.signal,
                headers: { Accept: "application/json", Authorization: "Bearer " + sessao.accessToken,
                    ...(dados ? { "Content-Type": "application/json" } : {}) },
                ...(dados ? { body: JSON.stringify(dados) } : {})
            });
            if (resposta.status === 401) { encerrar(); throw new Error("Sua sessão expirou. Entre novamente."); }
            const texto = await resposta.text();
            let corpo;
            try { corpo = texto ? JSON.parse(texto) : {}; } catch { throw new Error("Resposta inválida do servidor."); }
            if (!resposta.ok) {
                const mensagem = resposta.status === 404 ? "Decisão não encontrada para sua conta. Acesse uma decisão pela Home."
                    : resposta.status === 409 && typeof corpo.erro === "string" ? corpo.erro
                    : resposta.status === 400 ? (corpo.erro || "Confira os campos antes de salvar.")
                    : "Não foi possível concluir a solicitação. Tente novamente em instantes.";
                throw Object.assign(new Error(mensagem), { status: resposta.status });
            }
            return corpo;
        } catch (erro) {
            if (erro.status || encerrada) throw erro;
            throw Object.assign(new Error(method === "GET"
                ? "Não foi possível carregar seus dados. Confira a conexão e tente novamente."
                : "Não foi possível confirmar o salvamento. Confira os dados salvos antes de continuar."), { incerto: method !== "GET" });
        } finally { clearTimeout(timeout); }
    }

    function aviso(titulo, texto) {
        el("aviso-titulo").textContent = titulo;
        el("aviso-texto").textContent = texto;
        el("aviso").showModal();
    }

    function estado(ocupada) {
        ocupado = ocupada;
        el("conteudo").setAttribute("aria-busy", String(ocupada));
        campos.disabled = ocupada || !decisao || encerrada;
        el("recarregar").disabled = ocupada;
        el("ir-criterios").disabled = ocupada;
        el("sair").disabled = ocupada;
    }

    function renderizar(dados) {
        if (String(dados.decisaoId) !== id || typeof dados.titulo !== "string" || !Number.isInteger(dados.etapaAtual)) {
            throw new Error("Não foi possível interpretar a decisão. Recarregue os dados.");
        }
        decisao = dados;
        el("titulo").textContent = dados.titulo;
        el("opcao-a-nome").value = dados.opcaoA?.nome || "";
        el("opcao-a-descricao").value = dados.opcaoA?.descricao || "";
        el("opcao-b-nome").value = dados.opcaoB?.nome || "";
        el("opcao-b-descricao").value = dados.opcaoB?.descricao || "";
        const editavel = dados.status === "EM_ANDAMENTO" && dados.etapaAtual === 3;
        inputs.forEach(input => { input.readOnly = !editavel; });
        el("acoes-edicao").hidden = !editavel;
        el("ir-criterios").hidden = dados.status !== "EM_ANDAMENTO" || dados.etapaAtual !== 4;
        el("mensagem").textContent = editavel ? "" : dados.status === "CONCLUIDA"
            ? "Esta decisão está concluída. Suas opções estão disponíveis para consulta."
            : "Opções salvas. Esta etapa está disponível para consulta.";
        alterado = false;
    }

    async function carregar() {
        if (ocupado || !lerSessao()) return;
        if (!id || !/^[1-9]\d*$/.test(id)) {
            el("mensagem").textContent = "Abra esta tela a partir de uma decisão da sua Home.";
            el("conteudo").setAttribute("aria-busy", "false");
            return;
        }
        estado(true);
        el("mensagem").textContent = "Carregando sua decisão…";
        el("recarregar").hidden = true;
        try {
            const usuario = await requisitar("/auth/me");
            el("usuario-menu").textContent = usuario.nomeCompleto.trim().split(/\s+/)[0];
            el("usuario-inicial").textContent = Array.from(usuario.nomeCompleto.trim())[0].toLocaleUpperCase("pt-BR");
            el("perfil-nome").textContent = usuario.nomeCompleto;
            el("perfil-email").textContent = usuario.email;
            el("perfil-botao").disabled = false;
            renderizar(await requisitar("/decisoes/" + id + "/opcoes"));
            limparErros();
        } catch (erro) {
            decisao = null;
            el("mensagem").textContent = erro.message;
            el("recarregar").hidden = encerrada || erro.status === 404 || erro.status === 409;
        } finally { estado(false); }
    }

    function limparErros() {
        inputs.forEach(input => input.removeAttribute("aria-invalid"));
        formulario.querySelectorAll(".fluxo-erro-campo").forEach(p => { p.textContent = ""; });
    }

    function validar() {
        limparErros();
        let primeiro;
        function erro(inputId, destino, mensagem) {
            const campo = el(inputId);
            campo.setAttribute("aria-invalid", "true");
            el(destino).textContent = mensagem;
            primeiro ||= campo;
        }
        [["opcao-a-nome", "opcao-a-erro", "Dê um nome à opção A."],
            ["opcao-b-nome", "opcao-b-erro", "Dê um nome à opção B."]].forEach(([campo, destino, mensagem]) => {
            if (!el(campo).value.trim()) erro(campo, destino, mensagem);
            else if (el(campo).value.length > 200) erro(campo, destino, "Use até 200 caracteres.");
        });
        if (el("opcao-a-nome").value.trim() && el("opcao-a-nome").value.trim().toLocaleLowerCase("pt-BR") === el("opcao-b-nome").value.trim().toLocaleLowerCase("pt-BR")) {
            erro("opcao-b-nome", "opcao-b-erro", "Use nomes diferentes para identificar as alternativas.");
        }
        ["a", "b"].forEach(letra => {
            if (el("opcao-" + letra + "-descricao").value.length > 16000) erro("opcao-" + letra + "-descricao", "descricao-" + letra + "-erro", "Use até 16.000 caracteres.");
        });
        primeiro?.focus();
        return !primeiro;
    }

    function payload() {
        return { titulo: decisao.titulo,
            opcaoA: { nome: el("opcao-a-nome").value.trim(), descricao: el("opcao-a-descricao").value.trim() || null },
            opcaoB: { nome: el("opcao-b-nome").value.trim(), descricao: el("opcao-b-descricao").value.trim() || null } };
    }

    function irParaCriterios() {
        const evento = new CustomEvent("bussola:opcoes-concluidas", { cancelable: true, detail: { decisaoId: id, etapa: 4 } });
        if (!window.dispatchEvent(evento)) return;
        const rota = window.BUSSOLA_OPCOES_ROTAS?.criterios;
        if (rota) {
            const url = new URL(rota, window.location.href);
            url.searchParams.set("decisaoId", id);
            window.location.assign(url.href);
        } else aviso("Opções salvas", "Suas alternativas estão guardadas. A etapa de critérios estará disponível em breve. Você pode sair e retomar pela Home.");
    }

    formulario.addEventListener("submit", async evento => {
        evento.preventDefault();
        if (ocupado || window.BussolaEtapas.aberta || !decisao || decisao.etapaAtual !== 3 || decisao.status !== "EM_ANDAMENTO" || !validar()) return;
        const avancar = evento.submitter?.id !== "salvar-opcoes";
        if (avancar && !await window.BussolaEtapas.confirmarAvanco()) return;
        const dados = payload();
        estado(true);
        el("mensagem").textContent = "Salvando suas opções…";
        el("recarregar").hidden = true;
        try {
            const resposta = await requisitar("/decisoes/" + id + "/opcoes" + (avancar ? "/concluir" : ""), avancar ? "POST" : "PUT", dados);
            renderizar(resposta);
            el("mensagem").textContent = avancar ? "Opções salvas. Você concluiu esta etapa." : "Opções salvas. Você pode continuar agora ou retomar pela Home.";
            if (avancar) irParaCriterios();
        } catch (erro) {
            el("mensagem").textContent = erro.message;
            if (erro.incerto || erro.status === 409 || erro.status === 404) {
                decisao = null;
                el("recarregar").hidden = false;
            }
        } finally { estado(false); }
    });
    formulario.addEventListener("input", evento => {
        alterado = true;
        evento.target.removeAttribute("aria-invalid");
    });
    el("recarregar").addEventListener("click", () => {
        if (alterado && !window.confirm("Recarregar substitui o que está no formulário pelos dados salvos. Deseja continuar?")) return;
        carregar();
    });
    el("ir-criterios").addEventListener("click", irParaCriterios);
    window.addEventListener("beforeunload", evento => { if (alterado) { evento.preventDefault(); evento.returnValue = ""; } });
    window.addEventListener("pageshow", evento => { if (evento.persisted) window.location.reload(); });
    el("perfil-botao").addEventListener("click", () => {
        el("perfil-menu").hidden = !el("perfil-menu").hidden;
        el("perfil-botao").setAttribute("aria-expanded", String(!el("perfil-menu").hidden));
    });
    function fecharPerfil() { el("perfil-menu").hidden = true; el("perfil-botao").setAttribute("aria-expanded", "false"); }
    document.addEventListener("click", evento => { if (!evento.target.closest(".home-account")) fecharPerfil(); });
    document.addEventListener("keydown", evento => { if (evento.key === "Escape") fecharPerfil(); });
    el("historico").addEventListener("click", () => {
        const rota = window.BUSSOLA_OPCOES_ROTAS?.historico;
        if (rota) window.location.assign(rota);
        else aviso("Histórico", "O histórico estará disponível em breve. Suas decisões recentes podem ser consultadas na Home.");
    });
    el("sair").addEventListener("click", async () => {
        if (ocupado || (alterado && !window.confirm("Há alterações que ainda não foram salvas. Deseja sair mesmo assim?"))) return;
        if (!lerSessao()) return;
        estado(true);
        try {
            await requisitar("/auth/logout", "POST");
            alterado = false;
            sessionStorage.removeItem(chave);
            window.location.assign("login.html");
        } catch (erro) {
            if (!encerrada) aviso("Não foi possível sair", "Confira sua conexão e tente sair novamente.");
        } finally { estado(false); }
    });
    carregar();
})();
