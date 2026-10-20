import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";

const el = id => document.getElementById(id);
const id = new URLSearchParams(location.search).get("decisaoId");
const prazos = ["curtoPrazo", "medioPrazo", "longoPrazo"];
const campos = [...document.querySelectorAll("#reflexoes-form textarea")];
const rotas = window.BUSSOLA_101010_ROTAS || {};
let dados, ocupado = false, bloqueado = false, encerrada = false, confirmando = false, referencia = "";
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
    el("proxima-consulta").disabled = valor;
}
const preencherConta = configurarConta({ requisitar: api.requisitar, aviso,
    podeSair: () => !ocupado && !confirmando, aoSair: () => { referencia = ""; }, ocupado: estado, rotas });
function progresso() {
    el("progresso").textContent = campos.filter(c => c.value.trim()).length + " de 6 reflexões preenchidas";
    el("situacao").textContent = dados?.concluida && !sujo() ? "Técnica concluída" : "Preencha os seis campos e use “Próxima: Inversão” para concluir.";
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
    el("metodo").hidden = false;
    window.BussolaRevisao.sincronizar({ etapaAtual: dados.etapaAtual, status: dados.status, etapaPagina: 6, decisaoId: id });
    for (const tecnica of ["pros", "gut"]) el("tecnica-" + tecnica).href = "analise.html?decisaoId=" + encodeURIComponent(id) + "&tecnica=" + tecnica;
    if (rotas.inversao) { el("tecnica-inversao").removeAttribute("aria-disabled"); el("tecnica-inversao").querySelector("small").textContent = "Antifrágil"; el("tecnica-inversao").removeAttribute("title"); }
}
async function carregar() {
    if (ocupado || encerrada || !api.autenticada) return;
    estado(true);
    try {
        if (!id || !/^[1-9]\d*$/.test(id)) throw new Error("Abra uma decisão pela Home para acessar esta técnica.");
        const [usuario, resposta] = await Promise.all([api.requisitar("/auth/me"), api.requisitar("/decisoes/" + id + "/reflexoes-101010")]);
        preencherConta(usuario); renderizar(resposta); bloqueado = false; el("recarregar").hidden = true;
        el("mensagem").textContent = dados.status === "CONCLUIDA" ? "Decisão concluída: suas reflexões estão disponíveis apenas para consulta." : "";
    } catch (erro) { bloqueado = true; el("mensagem").textContent = erro.message; el("recarregar").hidden = encerrada || [404,409].includes(erro.status); }
    finally { estado(false); }
}
function validar(completa) {
    let primeiro;
    for (const campo of campos) {
        const erro = campo.value.length > 16000 ? "Use até 16.000 caracteres." : completa && !campo.value.trim() ? "Escreva sua reflexão para este prazo." : "";
        el(campo.id + "-erro").textContent = erro; campo.setAttribute("aria-invalid", String(!!erro));
        if (erro) primeiro ||= campo;
    }
    if (primeiro) { el("mensagem").textContent = "Confira os campos indicados antes de continuar."; primeiro.focus(); }
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
/**
 * Segue para a técnica de Inversão, próxima da sequência da Análise.
 *
 * Quando o método já está concluído, dispara antes o evento cancelável
 * `bussola:101010-concluido` (detail: decisaoId e etapa 6) para integrações.
 * Não grava nada: quem chama deve garantir que a tela está salva.
 *
 * @returns {void}
 */
function proxima() {
    if (dados?.concluida && !window.dispatchEvent(new CustomEvent("bussola:101010-concluido", { cancelable:true, detail:{ decisaoId:id, etapa:6 } }))) return;
    if (rotas.inversao) navegar(rotas.inversao);
    else aviso("Reflexões salvas", "A técnica de Inversão ainda não está disponível. Suas reflexões estão guardadas.");
}
el("reflexoes-form").addEventListener("submit", async evento => {
    evento.preventDefault();
    if (ocupado || confirmando || !editavel()) return;
    const avancar = evento.submitter?.id === "proxima-inversao";
    // Só conclui com os seis campos preenchidos; incompleto, salva o rascunho e segue sem concluir.
    const concluir = avancar && campos.every(campo => campo.value.trim());
    if (!validar(concluir)) return;
    if (avancar && !sujo() && (dados.concluida || !concluir)) { proxima(); return; }
    if (concluir && !await confirmar()) return;
    estado(true); el("mensagem").textContent = "Salvando suas reflexões…";
    let gravacaoConfirmada = false, seguir = false;
    try {
        const resposta = await api.requisitar("/decisoes/" + id + "/reflexoes-101010" + (concluir ? "/concluir" : ""), concluir ? "POST" : "PUT", corpo());
        gravacaoConfirmada = true;
        renderizar(resposta); el("mensagem").textContent = concluir ? "Método 10–10–10 concluído. Suas reflexões foram salvas." : "Reflexões salvas. Você pode continuar depois pela Análise da sua decisão.";
        seguir = avancar;
    } catch (erro) {
        bloqueado = gravacaoConfirmada || erro.incerto || erro.status >= 500 || [404,409].includes(erro.status);
        el("mensagem").textContent = erro.message;
        el("recarregar").hidden = !bloqueado || encerrada;
    } finally { estado(false); }
    if (seguir) proxima();
});
campos.forEach(campo => campo.addEventListener("input", () => {
    campo.removeAttribute("aria-invalid"); el(campo.id + "-erro").textContent = "";
    progresso(); window.BussolaAlteracoes.refletir(el("mensagem"), sujo());
}));
el("recarregar").addEventListener("click", () => {
    if (ocupado || confirmando) return;
    if (!sujo() || window.confirm("Recarregar substitui as respostas da tela pelos dados salvos. Deseja continuar?")) carregar();
});
el("tecnica-inversao").addEventListener("click", evento => {
    if (!rotas.inversao) return;
    window.BussolaAlteracoes.permitir(evento.currentTarget, () => navegar(rotas.inversao));
});
el("proxima-consulta").addEventListener("click", proxima);
window.addEventListener("pageshow", evento => { if (evento.persisted) location.reload(); });
carregar();
