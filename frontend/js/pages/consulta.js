import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";

/**
 * Consulta somente leitura de uma decisão CONCLUIDA.
 *
 * A página só faz GET nos endpoints que as telas de edição já usam e não tem
 * nenhum controle de escrita. O bloqueio de alterações continua sendo do
 * backend. Decisões em andamento não são exibidas aqui: elas seguem pelas
 * telas normais de edição e revisão.
 */

const ETAPAS = [
    { chave: "contexto", nome: "Contextualização", curto: "Contexto" },
    { chave: "brainstorming", nome: "Brainstorming", curto: "Brainstorm" },
    { chave: "opcoes", nome: "Opções", curto: "Opções" },
    { chave: "criterios", nome: "Critérios", curto: "Critérios" },
    { chave: "pesos", nome: "Pesos", curto: "Pesos" },
    { chave: "pros-contras", nome: "Prós e Contras", curto: "Prós e Contras" },
    { chave: "gut", nome: "Matriz GUT", curto: "Matriz GUT" },
    { chave: "101010", nome: "Método 10/10/10", curto: "10/10/10" },
    { chave: "inversao", nome: "Inversão", curto: "Inversão" },
    { chave: "resultado", nome: "Resultado comparativo", curto: "Resultado", pagina: "resultado.html" }
];
const CATEGORIAS = [
    { codigo: "FAVORAVEL", nome: "Favorável", classe: "favoravel" },
    { codigo: "DESFAVORAVEL", nome: "Desfavorável", classe: "desfavoravel" },
    { codigo: "DUVIDA", nome: "Dúvida", classe: "duvida" }
];
const NIVEIS_PESO = ["", "Baixo", "Moderado", "Médio", "Alto", "Essencial"];
const PRAZOS = [
    { campo: "curtoPrazo", nome: "Daqui a 10 minutos", icone: "⏱️" },
    { campo: "medioPrazo", nome: "Daqui a 10 meses", icone: "📅" },
    { campo: "longoPrazo", nome: "Daqui a 10 anos", icone: "🌏" }
];

const el = id => document.getElementById(id);
const id = new URLSearchParams(location.search).get("decisaoId");
let encerrada = false;
let dados = null;
let atual = null;
const api = criarApi(() => { encerrada = true; location.replace("login.html?motivo=sessao-expirada"); });

/**
 * Mostra o diálogo de aviso genérico do cabeçalho.
 *
 * @param {string} titulo título do diálogo
 * @param {string} texto mensagem exibida
 * @returns {void}
 */
function aviso(titulo, texto) {
    el("aviso-titulo").textContent = titulo;
    el("aviso-texto").textContent = texto;
    if (!el("aviso").open) el("aviso").showModal();
}
const preencherConta = configurarConta({
    requisitar: api.requisitar, aviso, podeSair: () => true, aoSair: () => {},
    ocupado: () => {}, rotas: { historico: "historico.html" }
});

/**
 * Cria um elemento com classe e texto opcionais, sempre via textContent.
 *
 * @param {string} tag nome da tag HTML
 * @param {string} [classe] classes CSS
 * @param {string|number} [texto] conteúdo de texto
 * @returns {HTMLElement} elemento criado
 */
function criar(tag, classe, texto) {
    const no = document.createElement(tag);
    if (classe) no.className = classe;
    if (texto !== undefined && texto !== null) no.textContent = String(texto);
    return no;
}

/**
 * Monta a URL desta consulta para uma etapa.
 *
 * @param {string} chave chave da etapa em ETAPAS
 * @returns {string} URL relativa com decisaoId e etapa, ou a página externa (Resultado)
 */
function urlEtapa(chave) {
    const etapa = ETAPAS.find(item => item.chave === chave);
    const url = new URL(etapa.pagina || "consulta.html", location.href);
    url.searchParams.set("decisaoId", id);
    if (!etapa.pagina) url.searchParams.set("etapa", chave);
    return url.pathname.split("/").pop() + url.search;
}

/**
 * Cria um parágrafo de texto livre do usuário, preservando quebras de linha,
 * ou um texto discreto quando nada foi registrado.
 *
 * @param {string|null} texto conteúdo salvo
 * @param {string} vazio texto mostrado quando não há conteúdo
 * @returns {HTMLParagraphElement} parágrafo pronto para leitura
 */
function textoLivre(texto, vazio) {
    const preenchido = typeof texto === "string" && texto.trim() !== "";
    return criar("p", preenchido ? "consulta-texto" : "consulta-texto consulta-vazio", preenchido ? texto : vazio);
}

/**
 * Título de alternativa no padrão das telas de análise ("A — nome").
 *
 * @param {"a"|"b"} letra alternativa
 * @param {string} nome nome da alternativa
 * @param {string} [tag] tag do título
 * @returns {HTMLElement} título com a letra destacada
 */
function tituloAlternativa(letra, nome, tag = "h2") {
    const titulo = criar(tag, "argumentos-titulo");
    titulo.append(criar("span", "argumentos-letra", letra.toUpperCase()));
    const texto = criar("span", "", "Alternativa " + letra.toUpperCase() + " — ");
    const nomeOpcao = criar("span", "", nome); nomeOpcao.dataset.nomeOpcao = "";
    texto.append(nomeOpcao);
    titulo.append(texto);
    titulo.querySelector(".argumentos-letra").setAttribute("aria-hidden", "true");
    return titulo;
}

/**
 * Bloco com a alternativa (letra e cor) usado em Opções, Prós e Contras e Inversão.
 *
 * @param {"a"|"b"} letra alternativa
 * @returns {HTMLElement} seção com data-letra para herdar as cores do projeto
 */
function blocoAlternativa(letra) {
    const bloco = criar("section", "argumentos-alternativa consulta-alternativa");
    bloco.dataset.letra = letra;
    return bloco;
}

/** Renderiza a Contextualização: título e descrição da decisão. */
function contexto() {
    const card = criar("article", "consulta-card");
    card.append(criar("h2", "consulta-rotulo", "Decisão"), criar("p", "consulta-destaque", dados.decisao.titulo));
    card.append(criar("h2", "consulta-rotulo", "Contexto"), textoLivre(dados.decisao.contexto, "Nenhum contexto foi registrado."));
    return [card];
}

/** Renderiza o Brainstorming: ideias agrupadas por categoria. */
function brainstorming() {
    const grade = criar("div", "consulta-ideias");
    for (const categoria of CATEGORIAS) {
        const ideias = dados.ideias.filter(ideia => ideia.categoria === categoria.codigo);
        const grupo = criar("section", "consulta-card consulta-ideia-grupo " + categoria.classe);
        grupo.append(criar("h2", "", categoria.nome + " (" + ideias.length + ")"));
        const lista = criar("ul", "consulta-lista");
        if (ideias.length === 0) lista.append(criar("li", "consulta-vazio", "Nenhuma ideia nesta categoria."));
        for (const ideia of ideias) lista.append(criar("li", "consulta-texto", ideia.conteudo));
        grupo.append(lista);
        grade.append(grupo);
    }
    return [criar("p", "consulta-ajuda", dados.ideias.length + (dados.ideias.length === 1 ? " ideia registrada." : " ideias registradas.")), grade];
}

/** Renderiza as Opções: alternativas A e B e suas descrições. */
function opcoes() {
    const grade = criar("div", "consulta-duas");
    for (const letra of ["a", "b"]) {
        const opcao = dados.criterios[letra === "a" ? "opcaoA" : "opcaoB"];
        const card = blocoAlternativa(letra);
        card.classList.add("consulta-card");
        card.append(tituloAlternativa(letra, opcao.nome), textoLivre(opcao.descricao, "Sem descrição."));
        grade.append(card);
    }
    return [grade];
}

/**
 * Célula com a nota de 1 a 5 no visual das notas da Matriz GUT.
 *
 * Em critérios com sentido `MENOR_MELHOR` (Risco), mostra a nota informada e,
 * logo abaixo, quanto ela conta no resultado (`6 - nota`), a mesma conversão
 * feita pelo backend.
 *
 * @param {number|null} nota nota registrada
 * @param {string} [sentido] sentido do critério; só `MENOR_MELHOR` muda a exibição
 * @returns {HTMLTableCellElement} célula centralizada
 */
function celulaNota(nota, sentido) {
    const celula = criar("td", "gut-nota");
    celula.append(criar("strong", "", nota == null ? "—" : nota + "/5"));
    if (nota != null && sentido === "MENOR_MELHOR")
        celula.append(criar("small", "consulta-nota-ajustada", "conta " + (6 - nota) + " no resultado"));
    return celula;
}

/**
 * Primeira coluna das tabelas de critérios: nome, selo de personalizado e descrição.
 *
 * @param {object} criterio critério devolvido pela API
 * @param {boolean} comSentido mostra a explicação da escala do Risco
 * @returns {HTMLTableCellElement} célula descritiva
 */
function celulaCriterio(criterio, comSentido) {
    const celula = document.createElement("th");
    celula.scope = "row";
    celula.append(criar("span", "consulta-criterio-nome", criterio.nome));
    if (criterio.personalizado) celula.append(criar("span", "gut-selo", "Personalizado"));
    if (criterio.descricao) celula.append(criar("small", "consulta-criterio-descricao", criterio.descricao));
    if (comSentido && criterio.sentido === "MENOR_MELHOR")
        celula.append(criar("small", "consulta-criterio-descricao", "1 = menor risco · 5 = maior risco"));
    return celula;
}

/**
 * Cria uma tabela com cabeçalho no visual da Matriz GUT.
 *
 * @param {string} legenda descrição da tabela para leitores de tela
 * @param {string[]} colunas rótulos das colunas
 * @param {HTMLTableRowElement[]} linhas linhas do corpo
 * @returns {HTMLDivElement} contêiner com rolagem horizontal quando necessário
 */
function tabela(legenda, colunas, linhas) {
    const contem = criar("div", "gut-tabela consulta-tabela");
    const tab = document.createElement("table");
    tab.append(criar("caption", "fluxo-sr-only", legenda));
    const cabecalho = document.createElement("tr");
    for (const coluna of colunas) { const th = criar("th", "", coluna); th.scope = "col"; cabecalho.append(th); }
    const thead = document.createElement("thead"); thead.append(cabecalho);
    const tbody = document.createElement("tbody"); tbody.append(...linhas);
    tab.append(thead, tbody);
    contem.append(tab);
    return contem;
}

/** Renderiza os Critérios: notas de cada alternativa, inclusive critérios personalizados. */
function criterios() {
    const { opcaoA, opcaoB } = dados.criterios;
    const linhas = dados.criterios.criterios.map(criterio => {
        const linha = document.createElement("tr");
        linha.append(celulaCriterio(criterio, true), celulaNota(criterio.notaA, criterio.sentido),
            celulaNota(criterio.notaB, criterio.sentido));
        return linha;
    });
    return [
        criar("p", "consulta-ajuda", "Notas de 1 a 5 atribuídas a cada alternativa em cada critério. "
            + "No Risco, a nota informada mede o risco; no resultado ela é invertida (risco 1 conta 5, risco 5 conta 1)."),
        tabela("Notas dos critérios para cada alternativa", ["Critério", "A — " + opcaoA.nome, "B — " + opcaoB.nome], linhas)
    ];
}

/** Renderiza os Pesos: importância atribuída a cada critério. */
function pesos() {
    const linhas = dados.criterios.criterios.map(criterio => {
        const linha = document.createElement("tr");
        const nivel = criar("td", "consulta-peso");
        const pontos = criar("span", "consulta-peso-pontos", "●".repeat(criterio.peso) + "○".repeat(5 - criterio.peso));
        pontos.setAttribute("aria-hidden", "true");
        nivel.append(pontos, criar("span", "", NIVEIS_PESO[criterio.peso] || ""));
        linha.append(celulaCriterio(criterio, false), celulaNota(criterio.peso), nivel);
        return linha;
    });
    return [
        criar("p", "consulta-ajuda", "Importância de 1 (baixa) a 5 (essencial) definida para cada critério."),
        tabela("Peso de cada critério", ["Critério", "Peso", "Importância"], linhas)
    ];
}

/** Renderiza Prós e Contras das duas alternativas, nos cards da Análise. */
function prosContras() {
    return dados.analise.opcoes.map((opcao, indice) => {
        const letra = indice === 0 ? "a" : "b";
        const bloco = blocoAlternativa(letra);
        const grade = criar("div", "argumentos-grade");
        for (const [tipo, classe, nome] of [["PRO", "lista-pros", "Prós"], ["CONTRA", "lista-contras", "Contras"]]) {
            const card = criar("section", "argumentos-card " + classe);
            card.append(criar("h3", "", nome));
            const lista = document.createElement("ul");
            const itens = opcao.prosContras.filter(item => item.tipo === tipo);
            if (itens.length === 0) lista.append(criar("li", "lista-vazia", "Nenhum registro."));
            for (const item of itens) { const li = document.createElement("li"); li.append(criar("p", "", item.descricao)); lista.append(li); }
            card.append(lista);
            grade.append(card);
        }
        bloco.append(tituloAlternativa(letra, opcao.nome), grade);
        return bloco;
    });
}

/** Renderiza a Matriz GUT na mesma tabela da Análise, sem a coluna de ações. */
function gut() {
    const fatores = dados.analise.fatoresGut;
    const maior = Math.max(...fatores.map(item => item.prioridade));
    const linhas = fatores.map(item => {
        const linha = document.createElement("tr");
        const fator = criar("td", "", item.descricao);
        if (fatores.length > 1 && item.prioridade === maior) {
            linha.classList.add("gut-topo");
            fator.append(criar("span", "gut-selo", "Maior prioridade"));
        }
        linha.append(fator);
        for (const campo of ["gravidade", "urgencia", "tendencia"]) linha.append(criar("td", "gut-nota", item[campo]));
        const prioridade = criar("td", "gut-nota"); prioridade.append(criar("strong", "", item.prioridade));
        linha.append(prioridade);
        return linha;
    });
    if (linhas.length === 0) {
        const vazia = document.createElement("tr"); const celula = criar("td", "lista-vazia", "Nenhum fator registrado.");
        celula.colSpan = 5; vazia.append(celula); linhas.push(vazia);
    }
    const legenda = criar("p", "gut-legenda");
    legenda.append(criar("strong", "", "Maior GUT = maior prioridade."), document.createTextNode(" Fatores ordenados do mais prioritário para o menos prioritário."),
        criar("span", "", "G: Gravidade · U: Urgência · T: Tendência · GUT: G × U × T (1 a 125)"));
    return [legenda, tabela("Fatores da matriz GUT, do mais prioritário para o menos prioritário", ["Fator", "G", "U", "T", "GUT"], linhas)];
}

/** Renderiza as reflexões do Método 10/10/10, por prazo e alternativa. */
function metodo101010() {
    const r = dados.reflexoes;
    const painel = criar("div", "consulta-prazos");
    for (const prazo of PRAZOS) {
        const card = criar("section", "consulta-card consulta-prazo");
        const titulo = criar("h2", "");
        const icone = criar("span", "", prazo.icone); icone.setAttribute("aria-hidden", "true");
        titulo.append(icone, document.createTextNode(" " + prazo.nome));
        card.append(titulo);
        for (const [letra, opcao] of [["a", r.opcaoA], ["b", r.opcaoB]]) {
            const bloco = criar("div", "consulta-reflexao opcao-" + letra);
            bloco.append(criar("h3", "", "Opção " + letra.toUpperCase() + " — " + opcao.nome), textoLivre(opcao[prazo.campo], "Sem resposta."));
            card.append(bloco);
        }
        painel.append(card);
    }
    return [criar("p", "consulta-ajuda", "Como você imaginou se sentir em relação a cada opção em três horizontes de tempo."), painel];
}

/** Renderiza a Inversão: problemas previstos e formas de prevenção de cada alternativa. */
function inversao() {
    const grade = criar("div", "consulta-duas");
    for (const [letra, opcao] of [["a", dados.inversao.opcaoA], ["b", dados.inversao.opcaoB]]) {
        const bloco = blocoAlternativa(letra);
        bloco.classList.add("consulta-card");
        bloco.append(tituloAlternativa(letra, opcao.nome),
            criar("h3", "consulta-rotulo", "O que poderia dar errado com esta escolha?"), textoLivre(opcao.fracassos, "Sem resposta."),
            criar("h3", "consulta-rotulo", "Como evitar esses fracassos?"), textoLivre(opcao.prevencoes, "Sem resposta."));
        grade.append(bloco);
    }
    return [grade];
}

const RENDERIZADORES = {
    contexto, brainstorming, opcoes, criterios, pesos,
    "pros-contras": prosContras, gut, "101010": metodo101010, inversao
};

/**
 * Atualiza um link de navegação (anterior/próxima) ou o esconde.
 *
 * @param {HTMLAnchorElement} link link a atualizar
 * @param {object|undefined} etapa etapa de destino
 * @param {string} prefixo "Anterior" ou "Próxima"
 * @returns {void}
 */
function ligarPasso(link, etapa, prefixo) {
    link.hidden = !etapa;
    if (!etapa) return;
    link.href = urlEtapa(etapa.chave);
    link.textContent = prefixo === "Anterior" ? "← Anterior: " + etapa.curto : "Próxima: " + etapa.nome + " →";
    link.dataset.etapa = etapa.pagina ? "" : etapa.chave;
}

/**
 * Exibe uma etapa da consulta, marca-a no indicador e ajusta Anterior/Próxima.
 *
 * Não faz nenhuma requisição: usa os dados já carregados.
 *
 * @param {string} chave etapa a exibir; chaves desconhecidas abrem a Contextualização
 * @param {{historico?: "push"|"replace"|null, focar?: boolean}} [opcoes] como registrar a URL e se o foco vai ao título
 * @returns {void}
 */
function mostrar(chave, { historico = null, focar = false } = {}) {
    const indice = Math.max(0, ETAPAS.findIndex(etapa => etapa.chave === chave && !etapa.pagina));
    const etapa = ETAPAS[indice];
    atual = etapa.chave;
    el("etapa-posicao").textContent = "Consulta · Etapa " + (indice + 1) + " de " + ETAPAS.length;
    el("etapa-titulo").textContent = etapa.nome;
    document.title = etapa.nome + " — Minha análise — Bússola";
    el("painel").replaceChildren(...RENDERIZADORES[etapa.chave]());
    el("etapas").querySelectorAll("a").forEach(link => {
        const ativo = link.dataset.etapa === etapa.chave;
        link.parentElement.classList.toggle("atual", ativo);
        if (ativo) link.setAttribute("aria-current", "step"); else link.removeAttribute("aria-current");
    });
    ligarPasso(el("anterior"), ETAPAS[indice - 1], "Anterior");
    ligarPasso(el("proxima"), ETAPAS[indice + 1], "Próxima");
    if (historico) history[historico === "push" ? "pushState" : "replaceState"](null, "", urlEtapa(etapa.chave));
    el("etapas").querySelector("li.atual")?.scrollIntoView({ block: "nearest", inline: "center" });
    if (focar) el("etapa-titulo").focus();
}

/**
 * Troca de etapa ao clicar num link interno, sem recarregar a página.
 * Links para o Resultado seguem como navegação normal.
 *
 * @param {MouseEvent} evento clique no link
 * @returns {void}
 */
function navegar(evento) {
    const link = evento.target.closest("a[data-etapa]");
    if (!link || !link.dataset.etapa || evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.button !== 0) return;
    evento.preventDefault();
    if (link.dataset.etapa !== atual) mostrar(link.dataset.etapa, { historico: "push", focar: true });
    else el("etapa-titulo").focus();
}

/** Monta o indicador com as 10 etapas, reaproveitando o círculo numerado das etapas. */
function montarIndicador() {
    el("etapas").replaceChildren(...ETAPAS.map((etapa, indice) => {
        const item = criar("li", etapa.pagina ? "externa" : "");
        const link = criar("a", "consulta-etapa");
        link.href = urlEtapa(etapa.chave);
        link.dataset.etapa = etapa.pagina ? "" : etapa.chave;
        link.append(criar("span", "etapa-circulo", indice + 1), criar("span", "", etapa.curto));
        link.querySelector(".etapa-circulo").setAttribute("aria-hidden", "true");
        if (etapa.curto !== etapa.nome) link.setAttribute("aria-label", etapa.nome);
        item.append(link);
        return item;
    }));
}

/**
 * Carrega a decisão e, se estiver concluída, todos os dados das etapas.
 *
 * Usa somente GET: /decisoes/{id}, /ideias, /criterios (opções, notas e
 * pesos), /analise (prós, contras e GUT), /reflexoes-101010 e /inversao.
 * Uma decisão em andamento não é mostrada aqui, para não confundir consulta
 * com revisão; a pessoa é orientada a continuar pelo Histórico.
 *
 * @returns {Promise<void>} Resolve depois de exibir a etapa pedida ou a mensagem de erro.
 */
async function carregar() {
    try {
        if (!id || !/^[1-9]\d*$/.test(id)) throw new Error("Abra a consulta pelo resultado de uma decisão finalizada.");
        preencherConta(await api.requisitar("/auth/me"));
        const decisao = await api.requisitar("/decisoes/" + id);
        if (decisao.status !== "CONCLUIDA") {
            const erro = new Error("Esta decisão ainda está em andamento. Continue a análise pelo Histórico; a consulta completa fica disponível depois de finalizar.");
            erro.emAndamento = true;
            throw erro;
        }
        const base = "/decisoes/" + id;
        const [ideias, criteriosDados, analise, reflexoes, inversaoDados] = await Promise.all(
            ["/ideias", "/criterios", "/analise", "/reflexoes-101010", "/inversao"].map(caminho => api.requisitar(base + caminho)));
        dados = { decisao, ideias, criterios: criteriosDados, analise, reflexoes, inversao: inversaoDados };
        el("decisao-titulo").textContent = decisao.titulo;
        el("atalho-resultado").href = urlEtapa("resultado");
        montarIndicador();
        el("mensagem").textContent = "";
        el("consulta").hidden = false;
        mostrar(new URLSearchParams(location.search).get("etapa") || "contexto", { historico: "replace" });
    } catch (erro) {
        if (encerrada) return;
        el("consulta").hidden = true;
        el("mensagem").textContent = erro.message;
        el("saidas-erro").hidden = false;
    } finally {
        el("conteudo").setAttribute("aria-busy", "false");
    }
}

el("etapas").addEventListener("click", navegar);
el("anterior").addEventListener("click", navegar);
el("proxima").addEventListener("click", navegar);
window.addEventListener("popstate", () => {
    if (dados) mostrar(new URLSearchParams(location.search).get("etapa") || "contexto", { focar: true });
});
carregar();
