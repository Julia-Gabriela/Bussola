(() => {
    "use strict";
    const origem = window.BUSSOLA_API_BASE || "http://localhost:8080";
    let encerrada = false;

    async function requisitar(caminho, metodo = "GET", dados) {
        let sessao;
        try { sessao = JSON.parse(sessionStorage.getItem("bussola.sessao")); } catch { sessao = null; }
        if (!sessao?.accessToken || sessao.tokenType !== "Bearer") return expirar();
        let resposta;
        try {
            resposta = await fetch(`${origem}${caminho}`, {
                method: metodo,
                headers: { Accept: "application/json", Authorization: `Bearer ${sessao.accessToken}`,
                    ...(dados === undefined ? {} : { "Content-Type": "application/json" }) },
                body: dados === undefined ? undefined : JSON.stringify(dados),
                cache: "no-store", signal: AbortSignal.timeout(15000)
            });
        } catch {
            throw Object.assign(new Error("Não foi possível confirmar a operação. Confira sua conexão e recarregue os dados antes de tentar novamente."), { incerto: metodo !== "GET" });
        }
        if (resposta.status === 401) return expirar();
        if (!resposta.ok) {
            const corpo = await resposta.json().catch(() => ({}));
            throw Object.assign(new Error(corpo.erro || "Não foi possível concluir a operação."),
                { status: resposta.status, incerto: metodo !== "GET" && resposta.status >= 500 });
        }
        if (resposta.status === 204) return null;
        try { return await resposta.json(); } catch {
            throw Object.assign(new Error("A resposta não pôde ser lida. Recarregue os dados antes de continuar."), { incerto: metodo !== "GET" });
        }
    }

    function expirar() {
        encerrada = true;
        sessionStorage.removeItem("bussola.sessao");
        window.location.replace("login.html?motivo=sessao-expirada");
        throw new Error("Sua sessão expirou. Entre novamente.");
    }

    function idDaUrl() {
        const id = new URLSearchParams(window.location.search).get("decisaoId");
        if (id !== null && !/^[1-9]\d*$/.test(id)) throw new Error("Link da decisão inválido. Acesse a decisão pela Home.");
        return id;
    }

    window.BussolaDecisaoApi = { requisitar, idDaUrl, get encerrada() { return encerrada; } };
})();
