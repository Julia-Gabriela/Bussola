import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";

const el = id => document.getElementById(id);
const id = new URLSearchParams(window.location.search).get("decisaoId");
const rotas = window.BUSSOLA_CRITERIOS_ROTAS || {};
let decisao = null, itens = [], ocupado = false, alterado = false, encerrada = false;
const api = criarApi(() => {
    encerrada = true; alterado = false;
    window.location.replace("login.html?motivo=sessao-expirada");
});
const editavel = () => decisao?.etapaAtual === 4 && decisao.status === "EM_ANDAMENTO";
const chaveNome = nome => nome.trim().toLocaleLowerCase("pt-BR");

function aviso(titulo, mensagem) {
    if (encerrada) return;
    el("aviso-titulo").textContent = titulo; el("aviso-texto").textContent = mensagem;
    if (!el("aviso").open) el("aviso").showModal();
}
function estado(valor) {
    ocupado = valor;
    el("conteudo").setAttribute("aria-busy", String(valor));
    el("criterios-campos").disabled = valor || !editavel() || encerrada;
    for (const nome of ["recarregar", "ir-pesos", "sair"]) el(nome).disabled = valor;
}
const preencherConta = configurarConta({ requisitar: api.requisitar, aviso, rotas,
    podeSair: () => !ocupado && (!alterado || window.confirm("Há avaliações não salvas. Deseja sair mesmo assim?")),
    aoSair: () => { alterado = false; }, ocupado: estado });

function criar(tag, classe, texto) {
    const elemento = document.createElement(tag);
    if (classe) elemento.className = classe;
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
}
function icone(codigo) {
    const img = criar("img");
    img.src = "../assets/images/criterios/" + codigo + ".svg";
    img.alt = ""; img.width = 16; img.height = 16;
    return img;
}
function grupoNotas(item, indice, letra) {
    const grupo = criar("fieldset", "criterio-notas cor-" + letra);
    grupo.dataset.indice = indice; grupo.dataset.letra = letra;
    const legenda = criar("legend", "", letra.toUpperCase() + ": " + decisao["opcao" + letra.toUpperCase()].nome);
    legenda.append(criar("span", "fluxo-sr-only", " — " + item.nome));
    grupo.append(legenda);
    if (item.sentido === "MENOR_MELHOR") grupo.setAttribute("aria-describedby", "risco-sentido");
    const botoes = criar("div", "notas-botoes");
    const campo = "nota" + letra.toUpperCase();
    for (let nota = 1; nota <= 5; nota++) {
        const label = criar("label", "nota-item" + (item[campo] >= nota ? " preenchida" : ""));
        const input = criar("input"); input.type = "radio"; input.name = "nota-" + indice + "-" + letra;
        input.value = nota; input.checked = item[campo] === nota;
        input.setAttribute("aria-label", String(nota) + (item.sentido === "MENOR_MELHOR"
            ? nota === 1 ? " — menor risco" : nota === 5 ? " — maior risco" : ""
            : nota === 1 ? " — menos favorável" : nota === 5 ? " — mais favorável" : ""));
        input.addEventListener("change", () => {
            if (!editavel() || ocupado) return;
            item[campo] = nota; grupo.removeAttribute("aria-invalid");
            botoes.querySelectorAll(".nota-item").forEach((botao, i) => botao.classList.toggle("preenchida", i < nota));
            marcarAlterado(); totais();
        });
        label.append(input, criar("span", "", String(nota))); botoes.append(label);
    }
    grupo.append(botoes); return grupo;
}
function tabela() {
    el("criterios-lista").replaceChildren();
    itens.forEach((item, indice) => {
        const linha = criar("section", "criterio-linha criterios-grade");
        const texto = criar("div", "criterio-identificacao");
        const titulo = criar("h2", "criterio-titulo"); titulo.id = "criterio-" + indice;
        if (["seguranca", "crescimento", "risco", "emocional", "valores"].includes(item.codigo)) titulo.append(icone(item.codigo));
        titulo.append(criar("span", "", item.nome));
        linha.setAttribute("aria-labelledby", titulo.id);
        texto.append(titulo, criar("p", "criterio-descricao", item.descricao || "Critério personalizado"));
        if (item.sentido === "MENOR_MELHOR") {
            const sentido = criar("span", "criterio-sentido", "1 = menor risco · 5 = maior risco"); sentido.id = "risco-sentido"; texto.append(sentido);
        }
        if (item.personalizado && editavel()) {
            const remover = criar("button", "criterio-remover", "Remover"); remover.type = "button";
            remover.setAttribute("aria-label", "Remover critério " + item.nome);
            remover.addEventListener("click", () => {
                if (ocupado || !window.confirm('Remover o critério "' + item.nome + '" e suas notas? A mudança será aplicada ao salvar.')) return;
                itens.splice(indice, 1); marcarAlterado(); tabela(); el("adicionar").focus();
            });
            texto.append(remover);
        }
        linha.append(texto, grupoNotas(item, indice, "a"), grupoNotas(item, indice, "b"));
        el("criterios-lista").append(linha);
    });
    el("adicionar").hidden = !editavel(); el("adicionar").disabled = itens.length >= 20;
    totais();
}
function totais() {
    let preenchidas = 0;
    for (const letra of ["a", "b"]) {
        const valores = itens.map(i => i["nota" + letra.toUpperCase()]);
        preenchidas += valores.filter(v => v !== null).length;
        el("total-" + letra).textContent = valores.reduce((s, v) => s + (v || 0), 0);
        el("maximo-" + letra).textContent = " / " + itens.length * 5;
    }
    const completa = preenchidas === itens.length * 2;
    el("total-rotulo").textContent = completa ? "Total (sem pesos)" : "Total parcial (sem pesos)";
    el("progresso").textContent = preenchidas + " de " + itens.length * 2 + " avaliações preenchidas";
}
function marcarAlterado() { alterado = true; el("mensagem").textContent = "Alterações ainda não salvas."; }
function renderizar(dados) {
    if (String(dados.decisaoId) !== id || !Array.isArray(dados.criterios) || !dados.opcaoA?.id || !dados.opcaoB?.id
            || dados.criterios.some(i => typeof i.nome !== "string" || [i.notaA, i.notaB].some(n => n !== null && (!Number.isInteger(n) || n < 1 || n > 5))))
        throw new Error("Não foi possível interpretar as avaliações. Recarregue os dados.");
    decisao = dados; itens = dados.criterios.map(i => ({ ...i }));
    el("decisao-resumo").textContent = dados.titulo;
    el("nome-a").textContent = dados.opcaoA.nome; el("nome-b").textContent = dados.opcaoB.nome;
    el("acoes-edicao").hidden = !editavel();
    el("ir-pesos").hidden = dados.etapaAtual !== 5 || dados.status !== "EM_ANDAMENTO";
    tabela(); alterado = false;
    el("mensagem").textContent = editavel() ? "" : "Avaliações salvas. Esta etapa está disponível para consulta.";
}
async function carregar() {
    if (ocupado || !api.autenticada || encerrada) return;
    if (!id || !/^[1-9]\d*$/.test(id)) {
        el("mensagem").textContent = "Abra esta tela a partir de uma decisão da sua Home."; estado(false); return;
    }
    estado(true); el("recarregar").hidden = true; el("mensagem").textContent = "Carregando sua avaliação…";
    try {
        const [usuario, dados] = await Promise.all([api.requisitar("/auth/me"), api.requisitar("/decisoes/" + id + "/criterios")]);
        preencherConta(usuario); renderizar(dados);
    } catch (erro) {
        decisao = null; el("mensagem").textContent = erro.message;
        el("recarregar").hidden = encerrada || [404, 409].includes(erro.status);
    } finally { estado(false); }
}
function irParaPesos() {
    const evento = new CustomEvent("bussola:criterios-concluidos", { cancelable: true, detail: { decisaoId: id, etapa: 5 } });
    if (!window.dispatchEvent(evento)) return;
    if (rotas.pesos) {
        const url = new URL(rotas.pesos, window.location.href); url.searchParams.set("decisaoId", id); window.location.assign(url.href);
    } else aviso("Avaliação salva", "Suas notas estão guardadas. A etapa de pesos estará disponível em breve. Você pode retomar esta decisão pela Home.");
}
function validarCompleta() {
    let primeiro;
    document.querySelectorAll(".criterio-notas").forEach(grupo => {
        const item = itens[Number(grupo.dataset.indice)];
        const vazio = item["nota" + grupo.dataset.letra.toUpperCase()] === null;
        grupo.setAttribute("aria-invalid", String(vazio));
        if (vazio) primeiro ||= grupo.querySelector("input");
    });
    if (primeiro) { el("mensagem").textContent = "Avalie todos os critérios das duas opções para continuar. Você também pode salvar as notas atuais para depois."; primeiro.focus(); }
    return !primeiro;
}
el("criterios-form").addEventListener("submit", async evento => {
    evento.preventDefault();
    if (ocupado || !editavel() || window.BussolaEtapas.aberta) return;
    const avancar = evento.submitter?.id === "avancar";
    if (avancar && !validarCompleta()) return;
    if (avancar && !await window.BussolaEtapas.confirmarAvanco()) return;
    const dados = { criterios: itens.map(({ nome, descricao, notaA, notaB }) => ({ nome, descricao, notaA, notaB })) };
    estado(true); el("mensagem").textContent = "Salvando suas avaliações…"; el("recarregar").hidden = true;
    try {
        const resposta = await api.requisitar("/decisoes/" + id + "/criterios" + (avancar ? "/concluir" : ""), avancar ? "POST" : "PUT", dados);
        renderizar(resposta); el("mensagem").textContent = avancar ? "Avaliação salva. Você concluiu esta etapa." : "Avaliações salvas. Você pode retomar pela Home.";
        if (avancar) irParaPesos();
    } catch (erro) {
        el("mensagem").textContent = erro.message;
        if (erro.incerto || [404, 409].includes(erro.status)) { decisao = null; el("recarregar").hidden = false; }
    } finally { estado(false); }
});
el("adicionar").addEventListener("click", () => {
    if (ocupado || !editavel() || itens.length >= 20) return;
    el("novo-form").reset(); el("novo-erro").textContent = ""; el("criterio-nome").removeAttribute("aria-invalid");
    el("novo-criterio").showModal(); el("criterio-nome").focus();
});
el("cancelar-criterio").addEventListener("click", () => el("novo-criterio").close());
el("novo-form").addEventListener("submit", evento => {
    evento.preventDefault();
    if (ocupado || !editavel() || itens.length >= 20) return;
    const nome = el("criterio-nome").value.trim();
    if (!nome || itens.some(i => chaveNome(i.nome) === chaveNome(nome))) {
        el("novo-erro").textContent = nome ? "Já existe um critério com esse nome." : "Informe o nome do critério.";
        el("criterio-nome").setAttribute("aria-invalid", "true"); el("criterio-nome").focus(); return;
    }
    itens.push({ id: null, codigo: "personalizado", nome, descricao: el("criterio-descricao").value.trim() || null,
        personalizado: true, sentido: "MAIOR_MELHOR", peso: 1, notaA: null, notaB: null });
    el("novo-criterio").close(); marcarAlterado(); tabela();
    el("criterios-lista").lastElementChild.querySelector("input").focus();
});
el("recarregar").addEventListener("click", () => {
    if (alterado && !window.confirm("Recarregar substitui as alterações atuais pelos dados salvos. Deseja continuar?")) return;
    carregar();
});
el("ir-pesos").addEventListener("click", irParaPesos);
window.addEventListener("beforeunload", evento => { if (alterado) { evento.preventDefault(); evento.returnValue = ""; } });
window.addEventListener("pageshow", evento => { if (evento.persisted) window.location.reload(); });
carregar();
