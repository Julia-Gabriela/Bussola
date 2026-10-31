import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js?v=3";
import { pendenciasAvaliacao, urlRevisarAvaliacoes } from "../shared/requisitos-resultado.js";


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
    el("analise-concluida").hidden = false;
    const recomendada = alternativaRecomendada(dados);
    renderizarRecomendacao(dados, recomendada);

    el("alternativas").replaceChildren(...dados.alternativas.map((opcao, indice) => {
        const card = criar("article", "resultado-card" + (opcao === recomendada ? " vencedora" : ""));
        card.append(criar("h2", "", (indice === 0 ? "A — " : "B — ") + opcao.nome));
        for (const criterio of opcao.criterios) {
            const linha = criar("div", "resultado-linha");
            const rotulo = criar("div", "resultado-rotulo");
            const trilho = criar("div", "resultado-trilho");
            const barra = criar("div", "resultado-progresso");
            const maximo = criterio.peso * 5;
            rotulo.append(
                criar("span", "", criterio.nome + " · peso " + criterio.peso),
                criar("span", "", criterio.pontuacao + "/" + maximo)
            );
            barra.style.width = (maximo > 0 ? criterio.pontuacao / maximo * 100 : 0) + "%";
            trilho.append(barra);
            linha.append(rotulo, trilho);
            linha.append(criar("small", "resultado-ajuste", criterio.sentido === "MENOR_MELHOR"
                ? "Nota informada " + criterio.nota + " · nota ajustada " + criterio.notaAjustada + " × peso " + criterio.peso
                : "Nota " + criterio.notaAjustada + " × peso " + criterio.peso));
            card.append(linha);
        }
        const total = criar("div", "resultado-total");
        total.append(
            criar("span", "", "Pontuação ponderada"),
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
        criar("span", "", "A — " + a.nome + ": " + Math.round(percentualA) + "%"),
        criar("span", "", "B — " + b.nome + ": " + Math.round(100 - percentualA) + "%")
    );
    renderizarInfluencias(dados);
    el("revisar").href = "analise.html?decisaoId=" + encodeURIComponent(decisaoId);
    aplicarStatus(dados);
    el("resultado").hidden = false;
}
/**
 * Descobre qual alternativa o backend recomendou.
 *
 * O backend é a fonte da regra: em empate, `empate` é true e `recomendadaId`
 * é null, e nenhuma alternativa é escolhida (a ordem de cadastro não desempata).
 * Se a resposta não trouxer esses campos, compara as pontuações com a mesma
 * regra: só há recomendação quando uma pontuação é estritamente maior.
 *
 * @param {{empate?: boolean, recomendadaId?: (number|null), alternativas: Array<{id: number, pontuacao: number}>}} dados resultado da API
 * @returns {(object|null)} a alternativa recomendada, ou null em caso de empate
 */
function alternativaRecomendada(dados) {
    const [a, b] = dados.alternativas;
    const empate = typeof dados.empate === "boolean" ? dados.empate : a.pontuacao === b.pontuacao;
    if (empate) return null;
    return dados.alternativas.find(opcao => opcao.id === dados.recomendadaId)
        ?? (a.pontuacao > b.pontuacao ? a : b);
}
/**
 * Escreve "N ponto" ou "N pontos", conforme o número.
 *
 * @param {number} valor quantidade de pontos (já em valor absoluto)
 * @returns {string} texto como "1 ponto" ou "8 pontos"
 */
function pontos(valor) {
    return valor + (valor === 1 ? " ponto" : " pontos");
}
/**
 * Calcula, só para exibição, quanto cada critério favoreceu uma alternativa.
 *
 * Usa as pontuações ponderadas que a API já devolveu (nota ajustada × peso)
 * e subtrai a de B da de A. Não recalcula notas nem pesos e não muda a
 * fórmula do resultado. Os critérios são pareados pelo nome; se o nome não
 * for encontrado em B, usa a mesma posição da lista.
 *
 * @param {{alternativas: Array<{criterios: Array<{nome: string, pontuacao: number}>}>}} dados resultado da API
 * @returns {Array<{nome: string, diferenca: number}>} uma entrada por critério de A;
 *          diferença positiva favorece A, negativa favorece B e zero não favorece ninguém.
 *          A lista vem ordenada da maior para a menor diferença absoluta.
 */
function diferencasPorCriterio(dados) {
    const [a, b] = dados.alternativas;
    return a.criterios.map((criterio, indice) => {
        const par = b.criterios.find(outro => outro.nome === criterio.nome) ?? b.criterios[indice];
        return { nome: criterio.nome, diferenca: criterio.pontuacao - (par ? par.pontuacao : 0) };
    }).sort((x, y) => Math.abs(y.diferenca) - Math.abs(x.diferenca));
}
/**
 * Preenche "O que mais influenciou" com os critérios que mais separaram as alternativas.
 *
 * Mostra até três critérios com diferença diferente de zero, no formato
 * "Segurança favoreceu a alternativa A (nome) em 8 pontos.". Critérios com
 * diferença zero não aparecem na lista. O resumo abaixo trata os casos
 * especiais: nenhuma diferença em critério algum, empate no total com
 * diferenças que se compensam, e critérios restantes omitidos da lista.
 *
 * @param {{alternativas: Array<{nome: string, pontuacao: number, criterios: Array<{nome: string, pontuacao: number}>}>}} dados resultado da API
 * @returns {void}
 */
function renderizarInfluencias(dados) {
    const [a, b] = dados.alternativas;
    const diferentes = diferencasPorCriterio(dados).filter(item => item.diferenca !== 0);
    const mostrados = diferentes.slice(0, 3);
    el("influencias").replaceChildren(...mostrados.map(item => {
        const favorecida = item.diferenca > 0 ? "A (" + a.nome + ")" : "B (" + b.nome + ")";
        const linha = criar("li", item.diferenca > 0 ? "favorece-a" : "favorece-b");
        linha.append(criar("strong", "", item.nome), document.createTextNode(
            " favoreceu a alternativa " + favorecida + " em " + pontos(Math.abs(item.diferenca)) + "."));
        return linha;
    }));
    el("influencias").hidden = mostrados.length === 0;
    let resumo = "";
    if (diferentes.length === 0)
        resumo = "Nenhum critério separou as alternativas: as duas tiveram a mesma pontuação em todos os critérios.";
    else if (a.pontuacao === b.pontuacao)
        resumo = "As diferenças entre os critérios se compensaram, e o total ficou empatado.";
    else if (diferentes.length > mostrados.length)
        resumo = "Os demais critérios tiveram diferenças menores.";
    el("influencia-resumo").textContent = resumo;
    el("influencia-resumo").hidden = !resumo;
}
/**
 * Preenche o quadro de destaque do topo do resultado.
 *
 * Com uma alternativa de maior pontuação, mostra o nome, a frase "Com seus
 * critérios e pesos, X teve a maior pontuação.", os pontos e o selo
 * "Maior pontuação". Em empate, mostra uma mensagem neutra com a pontuação
 * comum e um selo neutro, sem destacar A nem B.
 *
 * @param {{pontuacaoMaxima: number, alternativas: Array<{pontuacao: number}>}} dados resultado da API
 * @param {(object|null)} recomendada alternativa recomendada, ou null em empate
 * @returns {void}
 */
function renderizarRecomendacao(dados, recomendada) {
    const quadro = el("recomendacao");
    const chamada = criar("div", "resultado-recomendacao-conteudo");
    quadro.classList.toggle("empate", !recomendada);
    if (!recomendada) {
        const comum = dados.alternativas[0].pontuacao;
        chamada.append(
            criar("small", "", "Resultado comparativo"),
            criar("strong", "", "Empate entre as alternativas"),
            criar("p", "", "Com seus critérios e pesos, as duas alternativas tiveram a mesma pontuação: "
                + comum + " de " + dados.pontuacaoMaxima + " pontos possíveis.")
        );
        quadro.replaceChildren(chamada, criar("span", "resultado-selo resultado-selo-empate", "= Empate"));
        return;
    }
    chamada.append(
        criar("small", "", "Maior pontuação"),
        criar("strong", "", recomendada.nome),
        criar("p", "", "Com seus critérios e pesos, " + recomendada.nome + " teve a maior pontuação: "
            + recomendada.pontuacao + " de " + dados.pontuacaoMaxima + " pontos possíveis.")
    );
    quadro.replaceChildren(chamada, criar("span", "resultado-selo", "✦ Maior pontuação"));
}
/**
 * Ajusta a tela ao status da decisão vindo do backend.
 *
 * Em andamento: mostra "Revisar análise" e "Finalizar decisão".
 * Concluída: mostra o selo "Decisão finalizada" (com a data, se houver),
 * esconde as ações de edição e de finalização e oferece "Visualizar minha
 * análise" (consulta somente leitura) e "Voltar ao Histórico". Nada é gravado aqui; só a resposta do backend decide o status.
 *
 * @param {{status: string, finalizadaEm: (string|null)}} dados resultado devolvido pela API
 * @returns {void}
 */
function aplicarStatus(dados) {
    const concluida = dados.status === "CONCLUIDA";
    pendenteFinalizacao = dados.status === "EM_ANDAMENTO";
    el("revisar").hidden = concluida;
    el("finalizar").hidden = concluida;
    el("visualizar-analise").hidden = !concluida;
    el("visualizar-analise").href = "consulta.html?decisaoId=" + encodeURIComponent(decisaoId) + "&etapa=contexto";
    el("historico-rotulo").textContent = concluida ? "Voltar ao Histórico" : "Ver no Histórico";
    el("autonomia").hidden = concluida;
    el("finalizada").hidden = !concluida;
    if (!concluida) return;
    const data = new Date(dados.finalizadaEm);
    el("finalizada-texto").textContent = Number.isNaN(data.getTime())
        ? "Decisão finalizada · somente consulta"
        : "Decisão finalizada em " + new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(data) + " · somente consulta";
}
async function carregar() {
    try {
        if (!decisaoId || !/^[1-9]\d*$/.test(decisaoId))
            throw new Error("Abra uma decisão pela Análise.");
        const usuario = await api.requisitar("/auth/me");
        preencherConta(usuario);
        const dados = await api.requisitar("/decisoes/" + decisaoId + "/resultado");
        renderizar(dados);
        el("mensagem").textContent = "";
    } catch (erro) {
        el("resultado").hidden = true;
        el("analise-concluida").hidden = true;
        pendenteFinalizacao = false;
        el("mensagem").textContent = erro.status === 409 ? "O resultado ainda não pode ser exibido. " + erro.message : erro.message;
        if (decisaoId && /^[1-9]\d*$/.test(decisaoId)) {
            el("voltar-analise").href = "analise.html?decisaoId=" + encodeURIComponent(decisaoId);
            el("voltar-analise").hidden = false;
            if (erro.status === 409) {
                el("voltar-inversao").href = "inversao.html?decisaoId=" + encodeURIComponent(decisaoId);
                el("voltar-inversao").hidden = false;
                await indicarPendencias();
            }
        }
    }
}
/**
 * Quando o resultado foi recusado, verifica se há notas de critérios aguardando
 * confirmação e, nesse caso, diz qual alternativa revisar e mostra o link
 * "Revisar avaliações" para Critérios em modo de revisão.
 *
 * Consulta GET /decisoes/{id}/criterios (avaliacaoPendenteA/B), a mesma
 * informação que o backend usa para bloquear o resultado. Se a consulta
 * falhar, mantém apenas a mensagem original do backend.
 *
 * @returns {Promise<void>} Resolve depois de atualizar a mensagem e o link.
 */
async function indicarPendencias() {
    try {
        const situacao = pendenciasAvaliacao(await api.requisitar("/decisoes/" + decisaoId + "/criterios"));
        if (situacao.pronto) return;
        el("mensagem").textContent = "O resultado ainda não pode ser exibido. Confirme as notas "
            + (situacao.alternativas.length > 1 ? "das alternativas " : "da alternativa ") + situacao.alternativas.join(" e ") + " em Critérios.";
        el("revisar-avaliacoes").href = urlRevisarAvaliacoes(decisaoId, "resultado");
        el("revisar-avaliacoes").hidden = false;
        el("revisar-avaliacoes").focus();
    } catch { /* A mensagem do backend continua visível. */ }
}
let finalizando = false;
let recarregarAoFechar = false;
/**
 * Liga ou desliga o estado "finalizando" do popup.
 *
 * Enquanto a requisição está em curso, os dois botões ficam desabilitados
 * (impede clique duplo e cancelamento no meio) e o botão principal mostra
 * "Finalizando…".
 *
 * @param {boolean} valor true durante a chamada ao backend
 * @returns {void}
 */
function estadoFinalizacao(valor) {
    finalizando = valor;
    el("finalizar-dialogo").setAttribute("aria-busy", String(valor));
    el("finalizar-confirmar").disabled = valor;
    el("finalizar-cancelar").disabled = valor;
    el("finalizar-confirmar").textContent = valor ? "Finalizando…" : "Finalizar";
}
/**
 * Abre o popup "Finalizar esta decisão?" com o foco na opção segura
 * ("Continuar analisando"). Abrir o popup não altera a decisão.
 *
 * @returns {void}
 */
function abrirFinalizacao() {
    const dialogo = el("finalizar-dialogo");
    if (finalizando || dialogo.open) return;
    el("finalizar-erro").hidden = true;
    el("finalizar-erro").textContent = "";
    dialogo.showModal();
    el("finalizar-cancelar").focus();
}
/**
 * Pede ao backend para finalizar a decisão e, só depois do sucesso, mostra a
 * tela como concluída.
 *
 * Chama POST /decisoes/{id}/finalizar. Em caso de erro, o popup continua
 * aberto com a mensagem do backend e a decisão permanece em andamento,
 * pois o backend só grava o status depois de validar o resultado.
 *
 * @returns {Promise<void>} Resolve depois de atualizar a tela ou mostrar o erro.
 */
async function finalizar() {
    if (finalizando) return;
    estadoFinalizacao(true);
    try {
        const dados = await api.requisitar("/decisoes/" + decisaoId + "/finalizar", "POST");
        renderizar(dados);
        if (dados.status !== "CONCLUIDA") throw new Error("A decisão não foi finalizada. Tente novamente.");
        estadoFinalizacao(false);
        el("finalizar-dialogo").close();
        el("finalizada").focus();
    } catch (erro) {
        estadoFinalizacao(false);
        el("finalizar-erro").textContent = "Não foi possível finalizar. "
            + (erro.message || "Tente novamente.") + " A decisão continua em andamento.";
        el("finalizar-erro").hidden = false;
        recarregarAoFechar = erro.status === 409;
        el("finalizar-confirmar").focus();
    }
}
el("finalizar").addEventListener("click", abrirFinalizacao);
el("finalizar-confirmar").addEventListener("click", finalizar);
el("finalizar-cancelar").addEventListener("click", () => el("finalizar-dialogo").close());
el("finalizar-dialogo").addEventListener("cancel", evento => { if (finalizando) evento.preventDefault(); });
document.addEventListener("keydown", evento => {
    if (evento.key !== "Escape" || !el("finalizar-dialogo").open) return;
    evento.preventDefault();
    if (!finalizando) el("finalizar-dialogo").close();
}, true);
el("finalizar-dialogo").addEventListener("close", () => {
    if (recarregarAoFechar) {
        recarregarAoFechar = false;
        carregar().then(() => {
            if (document.activeElement !== document.body) return;
            const destino = el("finalizar").hidden || el("resultado").hidden
                ? document.querySelector(".resultado-bloqueio-acoes a:not([hidden])") : el("finalizar");
            destino?.focus();
        });
        return;
    }
    if (!el("finalizar").hidden) el("finalizar").focus();
});
let pendenteFinalizacao = false;
let saidaPendente = null;
let abridorSaida = null;
const LINKS_DO_FLUXO = new Set(["revisar", "revisar-avaliacoes", "voltar-inversao", "voltar-analise", "visualizar-analise"]);
/**
 * Avisa que a decisão ainda não foi finalizada antes de sair do Resultado.
 *
 * Só abre o popup "Sua decisão ainda não foi finalizada" quando o resultado está
 * na tela e a decisão continua EM_ANDAMENTO; em decisões concluídas, ou se o
 * resultado não pôde ser exibido, executa a saída direto. Abrir o popup não grava
 * nada: "Continuar analisando" fecha e mantém a pessoa aqui, e "Sair por enquanto"
 * executa `seguir`, com a decisão ainda em andamento e todos os dados preservados.
 * Não abre um segundo aviso se este ou o de finalização já estiver aberto.
 *
 * @param {() => void} seguir ação da saída escolhida (navegar ou encerrar a sessão)
 * @param {HTMLElement} abridor elemento que pediu a saída; recebe o foco de volta se a pessoa continuar
 * @returns {void}
 */
function confirmarSaida(seguir, abridor) {
    if (!pendenteFinalizacao || el("resultado").hidden) { seguir(); return; }
    const dialogo = el("saida-dialogo");
    if (dialogo.open || el("finalizar-dialogo").open || finalizando) return;
    saidaPendente = seguir;
    abridorSaida = abridor;
    dialogo.showModal();
    el("saida-continuar").focus();
}
document.addEventListener("click", evento => {
    const link = evento.target.closest?.("a[href]");
    if (!link || evento.defaultPrevented || evento.button !== 0
        || evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.altKey) return;
    if (LINKS_DO_FLUXO.has(link.id) || (link.target && link.target !== "_self") || link.hasAttribute("download")) return;
    const destino = new URL(link.href, location.href);
    if (destino.origin !== location.origin
        || (destino.pathname === location.pathname && destino.search === location.search)) return;
    if (!pendenteFinalizacao || el("resultado").hidden) return;
    evento.preventDefault();
    confirmarSaida(() => location.assign(destino.href), link);
});
el("saida-continuar").addEventListener("click", () => el("saida-dialogo").close());
document.addEventListener("keydown", evento => {
    if (evento.key !== "Escape" || !el("saida-dialogo").open) return;
    evento.preventDefault();
    el("saida-dialogo").close();
}, true);
el("saida-sair").addEventListener("click", () => {
    const seguir = saidaPendente;
    saidaPendente = null;
    abridorSaida = null;
    el("saida-dialogo").close();
    seguir?.();
});
el("saida-dialogo").addEventListener("close", () => {
    saidaPendente = null;
    if (abridorSaida?.isConnected) abridorSaida.focus();
    abridorSaida = null;
});
const preencherConta = configurarConta({
    requisitar: api.requisitar, aviso, podeSair: () => !finalizando, aoSair: () => {},
    ocupado: valor => { el("sair").disabled = valor; }, rotas: { historico: "historico.html" },
    confirmarSaida: (origem, seguir) => confirmarSaida(seguir, origem)
});
carregar();