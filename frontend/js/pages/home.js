(() => {
    "use strict";
    const origemApi = window.BUSSOLA_API_BASE || "http://localhost:8080";
    const chaveSessao = "bussola.sessao";
    const elemento = (id) => document.getElementById(id);
    let sessao;
    let carregando = false;
    let saindo = false;
    let decisaoAtual = null;

    function voltarAoLogin(expirada = false) {
        try { sessionStorage.removeItem(chaveSessao); } catch (_) { /* armazenamento indisponível */ }
        window.location.replace(expirada ? "login.html?motivo=sessao-expirada" : "login.html");
    }

    function lerSessao() {
        try {
            const valor = JSON.parse(sessionStorage.getItem(chaveSessao));
            return valor && valor.tokenType === "Bearer" && typeof valor.accessToken === "string"
                && valor.accessToken.trim() ? valor : null;
        } catch (_) { return null; }
    }

    async function requisitar(caminho, method = "GET") {
        const resposta = await fetch(`${origemApi}${caminho}`, {
            method,
            headers: { Accept: "application/json", Authorization: `Bearer ${sessao.accessToken}` },
            cache: "no-store",
            signal: AbortSignal.timeout(15000)
        });
        if (resposta.status === 401) { voltarAoLogin(true); return null; }
        if (!resposta.ok) throw new Error("Falha na requisição");
        return resposta;
    }

    function atualizarData() {
        const agora = new Date();
        const texto = new Intl.DateTimeFormat("pt-BR", {
            weekday: "long", day: "numeric", month: "short", year: "numeric"
        }).format(agora);
        elemento("data-hoje").textContent = texto.charAt(0).toUpperCase() + texto.slice(1);
        elemento("data-hoje").dateTime = [agora.getFullYear(), String(agora.getMonth() + 1).padStart(2, "0"),
            String(agora.getDate()).padStart(2, "0")].join("-");
    }

    function formatarData(valor) {
        const data = new Date(valor);
        if (Number.isNaN(data.getTime())) return "Data indisponível";
        if (data.toDateString() === new Date().toDateString()) return "Hoje";
        return new Intl.DateTimeFormat("pt-BR", { day: "numeric", month: "short", year: "numeric" }).format(data);
    }

    function imagem(nome, tamanho) {
        const img = document.createElement("img");
        img.src = `../assets/images/home/${nome}.svg`;
        img.alt = "";
        img.width = tamanho;
        img.height = tamanho;
        return img;
    }

    function renderizarDecisao(decisao) {
        const concluida = decisao.status === "CONCLUIDA";
        const item = document.createElement("li");
        const botao = document.createElement("button");
        botao.type = "button";
        botao.className = `home-decision${concluida ? " is-complete" : ""}`;
        const ponto = document.createElement("span");
        ponto.className = "home-decision-dot";
        ponto.setAttribute("aria-hidden", "true");
        const textos = document.createElement("span");
        textos.className = "home-decision-copy";
        const titulo = document.createElement("span");
        titulo.className = "home-decision-title";
        titulo.textContent = decisao.titulo;
        const data = document.createElement("time");
        data.dateTime = decisao.dataAtualizacao;
        data.textContent = formatarData(decisao.dataAtualizacao);
        textos.append(titulo, data);
        const status = document.createElement("span");
        status.className = "home-badge";
        status.textContent = concluida ? "concluída" : "em andamento";
        botao.append(ponto, textos, status, imagem("chevron", 16));
        botao.addEventListener("click", () => navegar(concluida ? "resultadoDecisao" : "continuarDecisao", decisao));
        item.append(botao);
        return item;
    }

    function renderizarHome(dados) {
        if (!dados.usuario || typeof dados.usuario.nomeCompleto !== "string"
                || !Array.isArray(dados.decisoesRecentes)
                || !Number.isFinite(dados.decisoesConcluidas) || !Number.isFinite(dados.decisoesEmAndamento)) {
            throw new Error("Resposta inválida");
        }
        const nome = dados.usuario.nomeCompleto.trim().split(/\s+/)[0] || "Você";
        elemento("usuario-nome").textContent = nome;
        elemento("usuario-menu").textContent = nome;
        elemento("usuario-inicial").textContent = Array.from(nome)[0].toLocaleUpperCase("pt-BR");
        elemento("perfil-nome").textContent = dados.usuario.nomeCompleto;
        elemento("perfil-email").textContent = dados.usuario.email;
        elemento("perfil-botao").disabled = false;
        elemento("total-concluidas").textContent = dados.decisoesConcluidas;
        elemento("total-andamento").textContent = dados.decisoesEmAndamento;
        const resumo = elemento("resumo");
        resumo.replaceChildren();
        if (dados.decisoesEmAndamento > 0) {
            const destaque = document.createElement("strong");
            destaque.textContent = `${dados.decisoesEmAndamento} ${dados.decisoesEmAndamento === 1 ? "decisão" : "decisões"} em andamento`;
            resumo.append("Você tem ", destaque, ". Continue de onde parou.");
        } else {
            resumo.textContent = "Uma nova escolha começa com clareza. Inicie sua próxima decisão.";
        }
        const lista = elemento("decisoes-recentes");
        lista.replaceChildren(...dados.decisoesRecentes.map(renderizarDecisao));
        lista.hidden = dados.decisoesRecentes.length === 0;
        elemento("decisoes-status").hidden = !lista.hidden;
        elemento("decisoes-status").textContent = "Suas decisões aparecerão aqui. Comece quando estiver pronto para uma nova escolha.";
        decisaoAtual = dados.decisaoEmAndamento;
        elemento("andamento-conteudo").hidden = !decisaoAtual;
        elemento("andamento-vazio").hidden = Boolean(decisaoAtual);
        elemento("andamento-vazio").textContent = "Nenhuma decisão em andamento. Seu próximo passo começa em Nova decisão.";
        if (decisaoAtual) {
            elemento("andamento-nome").textContent = decisaoAtual.titulo;
            elemento("andamento-etapa").textContent = `Etapa ${decisaoAtual.etapaAtual} de ${decisaoAtual.totalEtapas}`;
            elemento("andamento-percentual").textContent = `${decisaoAtual.progressoPercentual}%`;
            elemento("andamento-progresso").value = decisaoAtual.progressoPercentual;
        }
    }

    function mostrarErro(mensagem) {
        elemento("home-erro-texto").textContent = mensagem;
        elemento("home-erro").hidden = false;
    }

    async function carregarHome() {
        if (carregando || saindo) return;
        carregando = true;
        elemento("conteudo").setAttribute("aria-busy", "true");
        elemento("tentar-novamente").disabled = true;
        elemento("home-erro").hidden = true;
        try {
            const resposta = await requisitar("/home");
            if (resposta) renderizarHome(await resposta.json());
        } catch (_) {
            mostrarErro("Não foi possível carregar sua Home. Confira sua conexão e tente novamente.");
            if (elemento("usuario-nome").textContent === "…") {
                elemento("resumo").textContent = "Seus dados estarão disponíveis quando a conexão for restabelecida.";
            }
            if (!elemento("decisoes-status").hidden) elemento("decisoes-status").textContent = "Suas decisões não puderam ser carregadas.";
            if (!decisaoAtual) elemento("andamento-vazio").textContent = "Seu progresso estará disponível quando a conexão for restabelecida.";
        } finally {
            carregando = false;
            elemento("tentar-novamente").disabled = false;
            elemento("conteudo").setAttribute("aria-busy", "false");
        }
    }

    function navegar(destino, decisao = null) {
        const detalhe = { destino, decisaoId: decisao?.id ?? null, etapa: decisao?.etapaAtual ?? null,
            status: decisao?.status ?? null };
        if (!window.dispatchEvent(new CustomEvent("bussola:navegar", { detail: detalhe, cancelable: true }))) return;
        const rota = window.BUSSOLA_HOME_ROTAS?.[destino];
        if (typeof rota === "string" && rota.trim()) {
            try {
                const url = new URL(rota, window.location.href);
                if (url.origin !== window.location.origin || !["http:", "https:"].includes(url.protocol)) throw new Error("Rota inválida");
                if (decisao) url.searchParams.set("decisaoId", decisao.id);
                if (destino === "continuarDecisao" && decisao) url.searchParams.set("etapa", decisao.etapaAtual);
                window.location.assign(url.href);
                return;
            } catch (_) { /* Uma configuração incorreta não deve abrir URLs externas. */ }
        }
        const mensagens = {
            historico: "O Histórico estará disponível em breve. Suas decisões recentes continuam disponíveis aqui.",
            novaDecisao: "O fluxo para iniciar uma nova decisão estará disponível em breve.",
            continuarDecisao: "A continuação da análise estará disponível em breve. Seu progresso permanece salvo.",
            resultadoDecisao: "A visualização da análise estará disponível em breve. Sua decisão permanece salva.",
            perfil: "A edição do perfil estará disponível em breve."
        };
        elemento("aviso-texto").textContent = mensagens[destino] || "Esta área estará disponível em breve.";
        fecharPerfil();
        elemento("aviso-integracao").showModal();
    }

    function fecharPerfil() {
        elemento("perfil-menu").hidden = true;
        elemento("perfil-botao").setAttribute("aria-expanded", "false");
    }

    async function sair() {
        if (saindo) return;
        saindo = true;
        elemento("sair").disabled = true;
        fecharPerfil();
        try {
            const resposta = await requisitar("/auth/logout", "POST");
            if (!resposta) return;
            sessionStorage.removeItem(chaveSessao);
            window.location.replace("index.html");
        } catch (_) {
            mostrarErro("Não foi possível encerrar sua sessão. Confira sua conexão e clique em Sair novamente.");
        } finally {
            saindo = false;
            elemento("sair").disabled = false;
        }
    }

    sessao = lerSessao();
    if (!sessao) { voltarAoLogin(); return; }
    atualizarData();
    document.querySelectorAll("[data-destino]").forEach((botao) => botao.addEventListener("click", () => navegar(botao.dataset.destino)));
    elemento("continuar").addEventListener("click", () => { if (decisaoAtual) navegar("continuarDecisao", decisaoAtual); });
    elemento("tentar-novamente").addEventListener("click", carregarHome);
    elemento("sair").addEventListener("click", sair);
    elemento("perfil-botao").addEventListener("click", () => {
        const aberto = elemento("perfil-menu").hidden;
        elemento("perfil-menu").hidden = !aberto;
        elemento("perfil-botao").setAttribute("aria-expanded", String(aberto));
    });
    document.addEventListener("click", (evento) => { if (!evento.target.closest(".home-account")) fecharPerfil(); });
    document.addEventListener("keydown", (evento) => {
        if (evento.key === "Escape" && !elemento("perfil-menu").hidden) { fecharPerfil(); elemento("perfil-botao").focus(); }
    });
    window.addEventListener("pageshow", (evento) => {
        if (evento.persisted) {
            sessao = lerSessao();
            if (!sessao) voltarAoLogin(); else carregarHome();
        }
    });
    carregarHome();
})();
