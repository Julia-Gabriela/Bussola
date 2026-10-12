/** Cliente autenticado reutilizável. Nunca repete automaticamente uma escrita. */
export function criarApi(aoExpirar) {
    const chave = "bussola.sessao";
    let sessao;
    try { sessao = JSON.parse(sessionStorage.getItem(chave)); } catch { sessao = null; }
    function expirar() { sessionStorage.removeItem(chave); aoExpirar(); }
    const autenticada = sessao?.tokenType === "Bearer" && typeof sessao.accessToken === "string" && !!sessao.accessToken.trim();
    if (!autenticada) expirar();
    async function requisitar(caminho, method = "GET", dados) {
        if (!autenticada) throw new Error("Entre novamente na sua conta.");
        const abort = new AbortController();
        const timeout = setTimeout(() => abort.abort(), 15000);
        try {
            const resposta = await fetch((window.BUSSOLA_API_BASE || "http://localhost:8080") + caminho, {
                method, cache: "no-store", signal: abort.signal,
                headers: { Accept: "application/json", Authorization: "Bearer " + sessao.accessToken,
                    ...(dados !== undefined ? { "Content-Type": "application/json" } : {}) },
                ...(dados !== undefined ? { body: JSON.stringify(dados) } : {})
            });
            if (resposta.status === 401) { expirar(); throw Object.assign(new Error("Sua sessão expirou."), { status: 401 }); }
            const texto = await resposta.text();
            const corpo = texto ? JSON.parse(texto) : {};
            if (!resposta.ok) throw Object.assign(new Error(resposta.status === 404
                ? "Decisão não encontrada para sua conta. Abra uma decisão pela Home."
                : [400, 409].includes(resposta.status) && typeof corpo.erro === "string" ? corpo.erro
                : "Não foi possível concluir a solicitação. Tente novamente."), { status: resposta.status });
            return corpo;
        } catch (erro) {
            if (erro.status) throw erro;
            throw Object.assign(new Error(method === "GET"
                ? "Não foi possível carregar seus dados. Confira a conexão e tente novamente."
                : "Não foi possível confirmar o salvamento. Recarregue os dados antes de continuar."), { incerto: method !== "GET" });
        } finally { clearTimeout(timeout); }
    }
    return { autenticada, requisitar };
}
