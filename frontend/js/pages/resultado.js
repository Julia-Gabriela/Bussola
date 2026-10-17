import { criarApi } from "../shared/api.js";


const el = id => document.getElementById(id);
const decisaoId = new URLSearchParams(location.search).get("decisaoId");
const api = criarApi(() => location.replace("login.html?motivo=sessao-expirada"));

function aviso(titulo, texto) {
    el("aviso-titulo").textContent = titulo;
    el("aviso-texto").textContent = texto;
    if (!el("aviso").open) el("aviso").showModal();
}
function criar(tag, classe, texto) {
    const no = document.createElement(tag);
    if (classe) no.className = classe;
    if (texto !== undefined) no.textContent = texto;
    return no;
}
function renderizar(dados) {
    if (String(dados.decisaoId) !== decisaoId || !Array.isArray(dados.alternativas) || dados.alternativas.length !== 2)
        throw new Error("Não foi possível interpretar o resultado.");
    el("titulo-decisao").textContent = dados.titulo;
    const maior = Math.max(...dados.alternativas.map(opcao => opcao.pontuacao));
    const vencedora = dados.alternativas.find(opcao => opcao.pontuacao === maior);
    const chamada = criar("div", "resultado-recomendacao-conteudo");
    chamada.append(
        criar("small", "", "Opção mais alinhada com seus valores e prioridades"),
        criar("strong", "", vencedora.nome),
        criar("small", "", "Pontuação ponderada: " + vencedora.pontuacao + " pontos de " + dados.pontuacaoMaxima + " possíveis")
    );
    el("recomendacao").replaceChildren(chamada, criar("span", "resultado-selo", "✦ Recomendada"));

    el("alternativas").replaceChildren(...dados.alternativas.map((opcao, indice) => {
        const card = criar("article", "resultado-card" + (opcao.pontuacao === maior ? " vencedora" : ""));
        card.append(criar("h2", "", (indice === 0 ? "A — " : "B — ") + opcao.nome));
        for (const criterio of opcao.criterios) {
            const linha = criar("div", "resultado-linha");
            const rotulo = criar("div", "resultado-rotulo");
            const trilho = criar("div", "resultado-trilho");
            const barra = criar("div", "resultado-progresso");
            rotulo.append(
                criar("span", "", criterio.nome + " (×" + criterio.peso + ")"),
                criar("span", "", criterio.pontuacao + "/" + (criterio.peso * 5))
            );
            barra.style.width = (criterio.nota * 20) + "%";
            trilho.append(barra);
            linha.append(rotulo, trilho);
            card.append(linha);
        }
        const total = criar("div", "resultado-total");
        total.append(
            criar("span", "", "Pontuação total"),
            criar("strong", "", opcao.pontuacao + "/" + dados.pontuacaoMaxima)
        );
        card.append(total);
        return card;
    }));

    const [a, b] = dados.alternativas;
    const soma = a.pontuacao + b.pontuacao || 1;
    const percentualA = a.pontuacao / soma * 100;
    const segmentoA = criar("span", "resultado-segmento-a");
    const segmentoB = criar("span", "resultado-segmento-b");
    segmentoA.style.width = percentualA + "%";
    segmentoB.style.width = (100 - percentualA) + "%";
    el("comparativo").replaceChildren(segmentoA, segmentoB);
    el("legendas").replaceChildren(
        criar("span", "", "A — " + Math.round(percentualA) + "%"),
        criar("span", "", "B — " + Math.round(100 - percentualA) + "%")
    );
    el("revisar").href = "analise.html?decisaoId=" + encodeURIComponent(decisaoId);
    el("resultado").hidden = false;
}
async function carregar() {
    try {
        if (!decisaoId || !/^[1-9]\d*$/.test(decisaoId))
            throw new Error("Abra uma decisão pela Análise.");
        const usuario = await api.requisitar("/auth/me");
        el("usuario-menu").textContent = usuario.nomeCompleto.trim().split(/\s+/)[0]; el("usuario-inicial").textContent = Array.from(usuario.nomeCompleto.trim())[0]?.toLocaleUpperCase("pt-BR") || "?";
        const dados = await api.requisitar("/decisoes/" + decisaoId + "/resultado");
        renderizar(dados);
        el("mensagem").textContent = "";
    } catch (erro) {
        el("mensagem").textContent = erro.message;
        if (decisaoId) {
            el("voltar-analise").href = "analise.html?decisaoId=" + encodeURIComponent(decisaoId);
            el("voltar-analise").hidden = false;
        }
    }
}
el("sair").addEventListener("click", async () => {
    try {
        await api.requisitar("/auth/logout", "POST");
        sessionStorage.removeItem("bussola.sessao");
        location.replace("login.html");
    } catch (erro) {
        aviso("Não foi possível sair", erro.message);
    }
});
carregar();