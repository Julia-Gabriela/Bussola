import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";
import { requisitosAnalise, marcarRequisito, pendenciasAvaliacao, urlRevisarAvaliacoes } from "../shared/requisitos-resultado.js";
import { marcarNavegacao } from "../shared/tecnicas-estado.js?v=1";

const el = id => document.getElementById(id);
const id = new URLSearchParams(location.search).get("decisaoId");
const prazos = ["fracassos", "prevencoes"];
const campos = [...document.querySelectorAll("#reflexoes-form textarea")];
const rotas = window.BUSSOLA_INVERSAO_ROTAS || {};
let dados, ocupado = false, bloqueado = false, encerrada = false, confirmando = false, referencia = "";
let avaliacoes = null;
const api = criarApi(() => { encerrada = true; ocupado = false; confirmando = false; referencia = ""; location.replace("login.html?motivo=sessao-expirada"); });
const foto = () => JSON.stringify(campos.map(c => c.value));
const sujo = () => referencia !== "" && foto() !== referencia;
const editavel = () => dados?.status === "EM_ANDAMENTO" && !bloqueado && !encerrada;
window.BussolaAlteracoes.registrar({ sujo, ocupado: () => ocupado || confirmando });
function aviso(titulo, texto) {
    if (encerrada) return;
    el("aviso-titulo").textContent = titulo; el("aviso-texto").textContent = texto;
    if (!el("aviso").open) el("aviso").showModal();
}
function estado(valor) {
    ocupado = valor;
    el("conteudo").setAttribute("aria-busy", String(valor));
    el("reflexoes-campos").disabled = valor || bloqueado || !dados || encerrada;
    el("recarregar").disabled = valor || confirmando;
    el("resultado-consulta").disabled = valor;
    el("revisar-avaliacoes").disabled = valor || confirmando;
}

/**
 * Atualiza o item "Inversão" da lista de requisitos do resultado.
 *
 * Só aparece como concluída quando o backend confirmou a conclusão e não há
 * alterações na tela depois disso, porque salvar mudanças desfaz a conclusão.
 *
 * @returns {void}
 */
function marcarInversao() {
    const pronta = !!dados?.concluida && !sujo();
    marcarRequisito(el("requisitos-lista"), "inversao", pronta, pronta
        ? "concluída."
        : "preencha as quatro reflexões e escolha “Ver resultado comparativo” para concluir.");
    marcarNavegacao(document.querySelector(".tecnicas-nav"), { inversao: pronta });
}

/**
 * Consulta Prós e Contras, Matriz GUT, o método 10/10/10 e as avaliações dos
 * critérios para mostrar o que ainda falta antes do resultado comparativo.
 *
 * Usa apenas leituras já existentes (GET /analise, GET /reflexoes-101010 e
 * GET /criterios). As pendências de avaliação vêm de `avaliacaoPendenteA/B`,
 * a mesma informação que o backend usa para recusar o resultado. Quando há
 * pendência, mostra o botão "Revisar avaliações". Se alguma leitura falhar,
 * os itens dizem que não foi possível verificar. A liberação real é conferida
 * uma única vez, pela página de Resultado, com GET /decisoes/{id}/resultado.
 *
 * @returns {Promise<void>} Resolve depois de atualizar a lista.
 */
async function carregarRequisitos() {
    const lista = el("requisitos-lista");
    try {
        const [analise, reflexoes] = await Promise.all([
            api.requisitar("/decisoes/" + id + "/analise"), api.requisitar("/decisoes/" + id + "/reflexoes-101010")]);
        const situacao = requisitosAnalise(analise);
        for (const requisito of ["pros", "gut"]) marcarRequisito(lista, requisito, situacao[requisito].pronto, situacao[requisito].texto);
        marcarRequisito(lista, "101010", reflexoes.concluida === true, reflexoes.concluida === true
            ? "concluída."
            : "falta concluir: preencha as seis reflexões e use “Próxima: Inversão”.");
        marcarNavegacao(document.querySelector(".tecnicas-nav"),
            { pros: situacao.pros.pronto, gut: situacao.gut.pronto, "101010": reflexoes.concluida === true });
    } catch {
        for (const requisito of ["pros", "gut", "101010"]) marcarRequisito(lista, requisito, false, "não foi possível verificar agora.");
    }
    try {
        avaliacoes = pendenciasAvaliacao(await api.requisitar("/decisoes/" + id + "/criterios"));
        marcarRequisito(lista, "avaliacoes", avaliacoes.pronto, avaliacoes.texto);
    } catch {
        avaliacoes = null;
        marcarRequisito(lista, "avaliacoes", false, "não foi possível verificar agora.");
    }
    el("revisar-avaliacoes").hidden = !avaliacoes || avaliacoes.pronto;
}
const preencherConta = configurarConta({ requisitar: api.requisitar, aviso,
    podeSair: () => !ocupado && !confirmando, aoSair: () => { referencia = ""; }, ocupado: estado, rotas });
function progresso() {
    for (const letra of ["a", "b"]) el("progresso-" + letra).textContent = prazos.filter(p => el(p + "-" + letra).value.trim()).length + " de 2 preenchidos";
    el("progresso").textContent = campos.filter(c => c.value.trim()).length + " de 4 reflexões preenchidas";
    el("situacao").textContent = dados?.concluida && !sujo() ? "Técnica concluída" : "Preencha os quatro campos para concluir.";
    marcarInversao();
}
function renderizar(resposta) {
    if (String(resposta.decisaoId) !== id || resposta.etapaAtual !== 6 || !resposta.opcaoA?.id || !resposta.opcaoB?.id
            || typeof resposta.concluida !== "boolean" || typeof resposta.titulo !== "string"
            || [resposta.opcaoA, resposta.opcaoB].some(o => typeof o.nome !== "string"
                || prazos.some(p => o[p] !== null && typeof o[p] !== "string")))
        throw new Error("Não foi possível interpretar suas reflexões. Recarregue os dados.");
    dados = resposta;
    el("decisao-titulo").textContent = dados.titulo;
    for (const letra of ["a", "b"]) {
        const opcao = dados["opcao" + letra.toUpperCase()];
        document.querySelectorAll('[data-nome="' + letra + '"]').forEach(no => { no.textContent = opcao.nome; });
        for (const prazo of prazos) {
            const campo = el(prazo + "-" + letra); campo.value = opcao[prazo] || "";
            campo.readOnly = dados.status !== "EM_ANDAMENTO";
            campo.removeAttribute("aria-invalid"); el(campo.id + "-erro").textContent = "";
        }
    }
    referencia = foto(); progresso();
    el("acoes-edicao").hidden = dados.status !== "EM_ANDAMENTO";
    el("acoes-consulta").hidden = dados.status === "EM_ANDAMENTO";
    el("requisitos").hidden = dados.status !== "EM_ANDAMENTO";
    el("metodo").hidden = false;
    window.BussolaRevisao.sincronizar({ etapaAtual: dados.etapaAtual, status: dados.status, etapaPagina: 6, decisaoId: id });
    for (const tecnica of ["pros", "gut"]) el("tecnica-" + tecnica).href = "analise.html?decisaoId=" + encodeURIComponent(id) + "&tecnica=" + tecnica;
    el("tecnica-101010").href = "metodo-101010.html?decisaoId=" + encodeURIComponent(id);
}
async function carregar() {
    if (ocupado || encerrada || !api.autenticada) return;
    estado(true);
    try {
        if (!id || !/^[1-9]\d*$/.test(id)) throw new Error("Abra uma decisão pela Home para acessar esta técnica.");
        const [usuario, resposta] = await Promise.all([api.requisitar("/auth/me"), api.requisitar("/decisoes/" + id + "/inversao")]);
        preencherConta(usuario); renderizar(resposta); bloqueado = false; el("recarregar").hidden = true;
        el("mensagem").textContent = dados.status === "CONCLUIDA" ? "Decisão concluída: suas reflexões estão disponíveis apenas para consulta." : "";
    } catch (erro) { bloqueado = true; el("mensagem").textContent = erro.message; el("recarregar").hidden = encerrada || [404,409].includes(erro.status); }
    finally { estado(false); }
    if (dados?.status === "EM_ANDAMENTO" && !bloqueado) await carregarRequisitos();
}
const NOMES_CAMPOS = { fracassos: "o que poderia dar errado", prevencoes: "como evitar" };

/**
 * Confere os quatro campos antes de salvar ou concluir.
 *
 * Marca cada campo inválido com uma mensagem própria e foca o primeiro deles.
 * Ao concluir, o quadro acima dos botões lista quais respostas faltam, por
 * exemplo "Faltam responder: como evitar (A) e o que poderia dar errado (B).".
 *
 * @param {boolean} completa true para exigir os quatro campos preenchidos (conclusão);
 *        false para checar apenas o limite de 16.000 caracteres (salvamento parcial).
 * @returns {boolean} true quando todos os campos estão válidos.
 */
function validar(completa) {
    let primeiro;
    const faltas = [];
    for (const campo of campos) {
        const vazio = completa && !campo.value.trim();
        const erro = campo.value.length > 16000 ? "Use até 16.000 caracteres." : vazio ? "Escreva sua reflexão para esta perspectiva." : "";
        el(campo.id + "-erro").textContent = erro; campo.setAttribute("aria-invalid", String(!!erro));
        if (vazio) { const [perspectiva, letra] = campo.id.split("-"); faltas.push(NOMES_CAMPOS[perspectiva] + " (" + letra.toUpperCase() + ")"); }
        if (erro) primeiro ||= campo;
    }
    if (primeiro) {
        const lista = faltas.length > 1 ? faltas.slice(0, -1).join(", ") + " e " + faltas.at(-1) : faltas[0];
        if (faltas.length) el("resultado-bloqueio").textContent = (faltas.length === 1 ? "Falta responder: " : "Faltam responder: ") + lista + ". Preencha para concluir a técnica.";
        else el("mensagem").textContent = "Confira os campos indicados antes de continuar.";
        primeiro.focus();
    }
    return !primeiro;
}
function corpo() {
    const resposta = {};
    for (const letra of ["a", "b"]) {
        const chave = "opcao" + letra.toUpperCase();
        resposta[chave] = { opcaoId: dados[chave].id };
        for (const prazo of prazos) resposta[chave][prazo] = el(prazo + "-" + letra).value.trim() || null;
    }
    return resposta;
}
async function confirmar() {
    confirmando = true;
    const dialogo = el("confirmar-tecnica"); dialogo.returnValue = "";
    return new Promise(resolve => {
        dialogo.addEventListener("close", () => { confirmando = false; resolve(dialogo.returnValue === "confirmar"); }, { once:true });
        dialogo.showModal();
    });
}
function navegar(rota) {
    const url = new URL(rota, location.href); url.searchParams.set("decisaoId", id); location.assign(url.href);
}
function resultado() {
    const evento = new CustomEvent("bussola:inversao-concluida", { cancelable:true, detail:{ decisaoId:id, etapa:6 } });
    if (!window.dispatchEvent(evento)) return;
    if (rotas.resultado) navegar(rotas.resultado);
    else aviso("Reflexões salvas", "O resultado final estará disponível em breve. Suas reflexões estão guardadas. Você pode continuar explorando as outras técnicas de análise.");
}
el("reflexoes-form").addEventListener("submit", async evento => {
    evento.preventDefault();
    if (ocupado || confirmando || !editavel()) return;
    const concluir = evento.submitter?.id === "ver-resultado";
    el("resultado-bloqueio").textContent = "";
    if (!validar(concluir)) return;
    if (concluir && dados.concluida && !sujo()) { resultado(); return; }
    if (concluir && !await confirmar()) return;
    estado(true); el("mensagem").textContent = "Salvando suas reflexões…";
    let gravacaoConfirmada = false, abrirResultado = false;
    try {
        const resposta = await api.requisitar("/decisoes/" + id + "/inversao" + (concluir ? "/concluir" : ""), concluir ? "POST" : "PUT", corpo());
        gravacaoConfirmada = true;
        renderizar(resposta); el("mensagem").textContent = concluir ? "Técnica de Inversão concluída. Suas reflexões foram salvas." : "Reflexões salvas. Você pode continuar depois pela Análise da sua decisão.";
        abrirResultado = concluir;
    } catch (erro) {
        bloqueado = gravacaoConfirmada || erro.incerto || erro.status >= 500 || [404,409].includes(erro.status);
        el("mensagem").textContent = erro.message;
        el("recarregar").hidden = !bloqueado || encerrada;
    } finally { estado(false); }
    if (abrirResultado) resultado();
});
campos.forEach(campo => campo.addEventListener("input", () => {
    campo.removeAttribute("aria-invalid"); el(campo.id + "-erro").textContent = ""; el("resultado-bloqueio").textContent = "";
    progresso(); window.BussolaAlteracoes.refletir(el("mensagem"), sujo());
}));
el("recarregar").addEventListener("click", () => {
    if (ocupado || confirmando) return;
    if (!sujo() || window.confirm("Recarregar substitui as respostas da tela pelos dados salvos. Deseja continuar?")) carregar();
});
el("resultado-consulta").addEventListener("click", resultado);
el("revisar-avaliacoes").addEventListener("click", () => {
    if (ocupado || confirmando) return;
    window.BussolaAlteracoes.permitir(el("revisar-avaliacoes"), () => location.assign(urlRevisarAvaliacoes(id, "inversao")));
});
window.addEventListener("pageshow", evento => { if (evento.persisted) location.reload(); });
carregar();
