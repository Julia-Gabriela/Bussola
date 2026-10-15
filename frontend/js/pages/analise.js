import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";

const el = id => document.getElementById(id);
const id = new URLSearchParams(location.search).get("decisaoId");
let dados, ocupado = false, bloqueado = false, encerrada = false;
let argumentoId = null, fatorId = null, exclusao = null, referencia = "";
const api = criarApi(() => { encerrada = true; location.replace("login.html?motivo=sessao-expirada"); });
const editavel = () => dados?.status === "EM_ANDAMENTO" && !bloqueado && !encerrada;
const foto = () => JSON.stringify([el("tipo").value, el("argumento-descricao").value,
    el("gut-descricao").value, ...["gravidade", "urgencia", "tendencia"].map(campo => el(campo).value)]);
const sujo = () => referencia !== "" && foto() !== referencia;
window.BussolaAlteracoes.registrar({ sujo, ocupado: () => ocupado });
function aviso(titulo, texto) {
    el("aviso-titulo").textContent = titulo; el("aviso-texto").textContent = texto;
    if (!el("aviso").open) el("aviso").showModal();
}
function estado(valor) {
    ocupado = valor;
    el("conteudo").setAttribute("aria-busy", String(valor));
    el("argumento-campos").disabled = valor || !editavel();
    el("gut-campos").disabled = valor || !editavel();
    el("alternativa").disabled = valor || argumentoId !== null;
    document.querySelectorAll(".registro-acoes button").forEach(botao => { botao.disabled = valor || !editavel(); });
    el("excluir").disabled = valor || !editavel();
    el("recarregar").disabled = valor;
}
const preencherConta = configurarConta({ requisitar: api.requisitar, aviso, podeSair: () => !ocupado,
    aoSair: () => { referencia = ""; }, ocupado: estado, rotas: { historico: "historico.html" } });
function limpar() {
    argumentoId = null; fatorId = null;
    el("argumento-form").reset(); el("gut-form").reset();
    el("argumento-titulo").textContent = "Adicionar pró ou contra"; el("gut-titulo").textContent = "Adicionar fator";
    referencia = foto(); estado(ocupado);
}
function criar(tag, texto) { const no = document.createElement(tag); no.textContent = texto; return no; }
function acoes(item, tipo) {
    const div = document.createElement("div"); div.className = "registro-acoes";
    if (!editavel()) return div;
    const editar = criar("button", "Editar"), excluir = criar("button", "Excluir");
    editar.type = excluir.type = "button";
    editar.setAttribute("aria-label", "Editar " + item.descricao); excluir.setAttribute("aria-label", "Excluir " + item.descricao);
    editar.addEventListener("click", () => window.BussolaAlteracoes.permitir(editar, () => {
        limpar();
        if (tipo === "argumento") {
            argumentoId = item.id; el("tipo").value = item.tipo; el("argumento-descricao").value = item.descricao;
            el("argumento-titulo").textContent = "Editar pró ou contra"; el("argumento-descricao").focus();
        } else {
            fatorId = item.id; el("gut-descricao").value = item.descricao;
            ["gravidade", "urgencia", "tendencia"].forEach(campo => { el(campo).value = item[campo]; });
            el("gut-titulo").textContent = "Editar fator"; el("gut-descricao").focus();
        }
        referencia = foto(); estado(false);
    }));
    excluir.addEventListener("click", () => window.BussolaAlteracoes.permitir(excluir, () => {
        exclusao = { tipo, id: item.id, opcao: el("alternativa").value };
        el("exclusao-texto").textContent = item.descricao; el("exclusao").showModal();
    }));
    div.append(editar, excluir); return div;
}
function renderizar() {
    el("decisao-titulo").textContent = dados.titulo;
    el("abrir-101010").href = "metodo-101010.html?decisaoId=" + encodeURIComponent(id);
    el("argumento-form").hidden = el("gut-form").hidden = dados.status !== "EM_ANDAMENTO";
    window.BussolaRevisao.sincronizar({ etapaAtual: dados.etapaAtual, status: dados.status, etapaPagina: 6, decisaoId: id });
    const selecionada = el("alternativa").value;
    el("alternativa").replaceChildren(...dados.opcoes.map((opcao, i) => {
        const no = criar("option", (i === 0 ? "A — " : "B — ") + opcao.nome); no.value = opcao.id; return no;
    }));
    if (dados.opcoes.some(opcao => String(opcao.id) === selecionada)) el("alternativa").value = selecionada;
    const opcao = dados.opcoes.find(item => String(item.id) === el("alternativa").value);
    for (const [tipo, lista] of [["PRO", "pros"], ["CONTRA", "contras"]]) {
        const itens = opcao.prosContras.filter(item => item.tipo === tipo);
        el(lista).replaceChildren(...itens.map(item => { const li = document.createElement("li"); li.append(criar("p", item.descricao), acoes(item, "argumento")); return li; }));
        if (!itens.length) el(lista).append(criar("li", "Nenhum registro ainda."));
    }
    el("fatores").replaceChildren(...dados.fatoresGut.map(item => {
        const linha = document.createElement("tr");
        for (const campo of ["descricao", "gravidade", "urgencia", "tendencia", "prioridade"]) linha.append(criar("td", item[campo]));
        const celula = document.createElement("td"); celula.append(acoes(item, "gut")); linha.append(celula); return linha;
    }));
    if (!dados.fatoresGut.length) { const linha = document.createElement("tr"), celula = criar("td", "Nenhum fator registrado."); celula.colSpan = 6; linha.append(celula); el("fatores").append(linha); }
    el("analise").hidden = false; estado(ocupado);
}
async function consultar() {
    const resposta = await api.requisitar(`/decisoes/${id}/analise`);
    if (String(resposta.decisaoId) !== id || resposta.etapaAtual !== 6 || resposta.opcoes?.length !== 2
            || !Array.isArray(resposta.fatoresGut)) throw new Error("Resposta inválida. Recarregue os dados.");
    dados = resposta;
}
async function carregar() {
    if (ocupado || encerrada || !api.autenticada) return;
    estado(true);
    try {
        if (!id || !/^[1-9]\d*$/.test(id)) throw new Error("Abra uma decisão pela Home.");
        preencherConta(await api.requisitar("/auth/me")); await consultar(); bloqueado = false;
        limpar(); renderizar(); el("recarregar").hidden = true;
        el("mensagem").textContent = dados.status === "CONCLUIDA" ? "Decisão concluída: somente consulta." : "";
    } catch (erro) { bloqueado = true; el("analise").hidden = true; el("recarregar").hidden = false; el("mensagem").textContent = erro.message; }
    finally { estado(false); }
    if (dados && !bloqueado && new URLSearchParams(location.search).get("tecnica") === "gut") abrirAba("gut");
}
async function salvar(caminho, metodo, corpo) {
    if (ocupado || !editavel()) return;
    estado(true); el("mensagem").textContent = "Salvando…";
    let confirmado = false;
    try {
        await api.requisitar(caminho, metodo, corpo); confirmado = true;
        limpar(); await consultar(); renderizar(); el("mensagem").textContent = "Alteração salva.";
    } catch (erro) {
        bloqueado = confirmado || erro.incerto || erro.status >= 500 || [404,409].includes(erro.status);
        el("recarregar").hidden = !bloqueado;
        el("mensagem").textContent = bloqueado
            ? confirmado ? "Alteração salva, mas não foi possível atualizar a tela. Recarregue os dados antes de continuar."
                : erro.message.includes("Recarregue") ? erro.message : erro.message + " Recarregue os dados antes de continuar."
            : erro.message;
    } finally { estado(false); }
}
for (const campo of ["argumento-descricao", "gut-descricao"]) el(campo).addEventListener("input", () => el(campo).setCustomValidity(""));
el("argumento-form").addEventListener("submit", evento => {
    evento.preventDefault(); const descricao = el("argumento-descricao").value.trim();
    el("argumento-descricao").setCustomValidity(descricao ? "" : "Descreva o pró ou contra.");
    if (!el("argumento-form").reportValidity()) return;
    salvar(`/decisoes/${id}/opcoes/${el("alternativa").value}/pros-contras${argumentoId === null ? "" : "/" + argumentoId}`,
        argumentoId === null ? "POST" : "PUT", { tipo: el("tipo").value, descricao });
});
el("gut-form").addEventListener("submit", evento => {
    evento.preventDefault(); const descricao = el("gut-descricao").value.trim();
    el("gut-descricao").setCustomValidity(descricao ? "" : "Descreva o fator.");
    if (!el("gut-form").reportValidity()) return;
    salvar(`/decisoes/${id}/gut${fatorId === null ? "" : "/" + fatorId}`, fatorId === null ? "POST" : "PUT",
        { descricao, gravidade: Number(el("gravidade").value), urgencia: Number(el("urgencia").value), tendencia: Number(el("tendencia").value) });
});
for (const botao of ["cancelar-argumento", "cancelar-gut"]) el(botao).addEventListener("click", limpar);
let alternativaAnterior;
el("alternativa").addEventListener("focus", () => { alternativaAnterior = el("alternativa").value; });
el("alternativa").addEventListener("change", () => {
    if (sujo()) { el("alternativa").value = alternativaAnterior; aviso("Alterações não salvas", "Salve ou cancele o rascunho antes de trocar a alternativa."); return; }
    alternativaAnterior = el("alternativa").value; renderizar();
});
function abrirAba(tipo) {
    if (ocupado || sujo()) { aviso("Alterações não salvas", "Salve ou cancele o rascunho antes de trocar de técnica."); return; }
    for (const nome of ["pros", "gut"]) { el("painel-" + nome).hidden = nome !== tipo; if (nome === tipo) el("aba-" + nome).setAttribute("aria-current", "true"); else el("aba-" + nome).removeAttribute("aria-current"); }
    el("aba-" + tipo).focus();
}
for (const tipo of ["pros", "gut"]) {
    el("aba-" + tipo).addEventListener("click", () => abrirAba(tipo));
}
el("cancelar-exclusao").addEventListener("click", () => el("exclusao").close());
el("exclusao").addEventListener("close", () => { exclusao = null; });
el("excluir").addEventListener("click", () => {
    if (!exclusao || ocupado) return;
    const item = exclusao; el("exclusao").close();
    salvar(`/decisoes/${id}/` + (item.tipo === "gut" ? `gut/${item.id}` : `opcoes/${item.opcao}/pros-contras/${item.id}`), "DELETE");
});
el("recarregar").addEventListener("click", () => {
    if (bloqueado) { if (!sujo() || window.confirm("Descartar o rascunho e conferir o que foi salvo?")) carregar(); }
    else window.BussolaAlteracoes.permitir(el("recarregar"), carregar);
});
window.addEventListener("pageshow", evento => { if (evento.persisted) location.reload(); });
carregar();
