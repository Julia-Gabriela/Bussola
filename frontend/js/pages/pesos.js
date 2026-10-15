import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";

const el = id => document.getElementById(id);
const id = new URLSearchParams(location.search).get("decisaoId");
let dados = null, ocupado = false, bloqueado = false, encerrada = false, referencia = "";
const api = criarApi(() => { encerrada = true; location.replace("login.html?motivo=sessao-expirada"); });
const emRevisao = () => dados && window.BussolaRevisao.emRevisao(dados.etapaAtual, 5);
const editavel = () => dados?.status === "EM_ANDAMENTO"
    && (dados.etapaAtual === 5 || (dados.etapaAtual === 6 && emRevisao()))
    && !bloqueado && !encerrada;
const foto = () => JSON.stringify(dados?.criterios?.map(item => [item.id, item.peso]) || []);
const niveis = ["", "Baixo", "Moderado", "Médio", "Alto", "Essencial"];
const icones = { "Segurança":"seguranca", "Crescimento":"crescimento", "Risco":"risco",
    "Impacto emocional":"emocional", "Alinhamento a valores":"valores" };
window.BussolaAlteracoes.registrar({ sujo: () => referencia !== "" && foto() !== referencia, ocupado: () => ocupado });

function aviso(titulo, texto) {
    el("aviso-titulo").textContent = titulo;
    el("aviso-texto").textContent = texto;
    if (!el("aviso").open) el("aviso").showModal();
}
function estado(valor) {
    ocupado = valor;
    el("conteudo").setAttribute("aria-busy", String(valor));
    el("avancar").disabled = valor || !editavel();
    el("recarregar").disabled = valor;
    document.querySelectorAll(".peso-controle input").forEach(input => { input.disabled = valor || !editavel(); });
}
const preencherConta = configurarConta({ requisitar: api.requisitar, aviso, podeSair: () => !ocupado,
    aoSair: () => { referencia = ""; }, ocupado: estado, rotas: { historico:"historico.html" } });
function criar(tag, classe, texto) {
    const no = document.createElement(tag);
    if (classe) no.className = classe;
    if (texto !== undefined) no.textContent = texto;
    return no;
}
function atualizarSoma() {
    const lista = dados.criterios;
    const soma = lista.reduce((total, criterio) => total + criterio.peso, 0);
    const maximo = lista.length * 5;
    el("soma-pesos").textContent = soma;
    el("barra-total").setAttribute("aria-valuemax", String(maximo));
    el("barra-total").setAttribute("aria-valuenow", String(soma));
    el("barra-total-valor").style.width = String(maximo ? soma / maximo * 100 : 0) + "%";
}
function renderizar() {
    const lista = dados.criterios, maximo = lista.length * 5;
    el("maximo-pesos").textContent = "/ " + maximo + " máx.";
    el("criterios-lista").replaceChildren(...lista.map(criterio => {
        const card = criar("article", "peso-criterio");
        const cabecalho = criar("div", "peso-criterio-cabecalho");
        const icone = criar("img", "peso-criterio-icone");
        icone.src = "../assets/images/criterios/" + (icones[criterio.nome] || "valores") + ".svg";
        icone.alt = "";
        const texto = criar("div", "peso-criterio-texto");
        texto.append(criar("h2", "", criterio.nome));
        texto.append(criar("p", "", criterio.descricao || "Critério personalizado"));
        const nivel = criar("span", "peso-nivel", niveis[criterio.peso]);
        const valor = criar("output", "peso-valor", criterio.peso);
        valor.setAttribute("aria-label", "Peso de " + criterio.nome);
        cabecalho.append(icone, texto, nivel, valor);
        const controle = criar("div", "peso-controle");
        const trilho = criar("div", "peso-trilho");
        const progresso = criar("span", "peso-trilho-progresso");
        const marcador = criar("span", "peso-trilho-marcador");
        trilho.setAttribute("aria-hidden", "true");
        for (let marca = 1; marca <= 5; marca++) trilho.append(criar("span", "peso-trilho-ponto" + (marca <= criterio.peso ? " passado" : "")));
        trilho.append(progresso, marcador);
        const slider = document.createElement("input");
        slider.type = "range"; slider.min = "1"; slider.max = "5"; slider.step = "1";
        slider.value = String(criterio.peso);
        slider.setAttribute("aria-label", "Importância de " + criterio.nome);
        slider.setAttribute("aria-valuetext", criterio.peso + ": " + niveis[criterio.peso]);
        trilho.style.setProperty("--peso-posicao", String((criterio.peso - 1) * 25) + "%");
        slider.addEventListener("input", () => {
            const peso = Number(slider.value);
            criterio.peso = peso;
            nivel.textContent = niveis[peso];
            valor.value = String(peso); valor.textContent = String(peso);
            slider.setAttribute("aria-valuetext", peso + ": " + niveis[peso]);
            trilho.style.setProperty("--peso-posicao", String((peso - 1) * 25) + "%");
            trilho.querySelectorAll(".peso-trilho-ponto").forEach((ponto, indice) => ponto.classList.toggle("passado", indice + 1 <= peso));
            atualizarSoma();
        });
        const escala = criar("div", "peso-escala");
        for (let marca = 1; marca <= 5; marca++) escala.append(criar("span", "", marca));
        controle.append(trilho, slider, escala); card.append(cabecalho, controle);
        return card;
    }));
    atualizarSoma();
    el("painel-pesos").hidden = false;
    el("avancar").hidden = !editavel();
    el("avancar").replaceChildren(document.createTextNode(emRevisao() ? "Salvar alterações " : "Próxima etapa "));
    el("avancar").append(criar("span", "", emRevisao() ? "✓" : "→"));
    window.BussolaRevisao.sincronizar({ etapaAtual:dados.etapaAtual, status:dados.status, etapaPagina:5, decisaoId:id });
    estado(ocupado);
}
async function carregar() {
    if (ocupado || encerrada || !api.autenticada) return;
    estado(true); bloqueado = false;
    try {
        if (!id || !/^[1-9]\d*$/.test(id)) throw new Error("Abra uma decisão pela Home.");
        preencherConta(await api.requisitar("/auth/me"));
        const resposta = await api.requisitar("/decisoes/" + id + "/pesos");
        if (String(resposta.decisaoId) !== id || !Array.isArray(resposta.criterios) || resposta.criterios.length < 5
            || resposta.criterios.some(item => !Number.isInteger(item.id) || !Number.isInteger(item.peso) || item.peso < 1 || item.peso > 5))
            throw new Error("Os dados dos pesos vieram em um formato inesperado. Recarregue a decisão.");
        dados = resposta; referencia = foto(); renderizar();
        el("recarregar").hidden = true;
        el("mensagem").textContent = dados.status === "CONCLUIDA" ? "Decisão concluída: somente consulta."
            : dados.etapaAtual === 6 && !emRevisao() ? "Etapa já concluída. Use o indicador para revisar os pesos." : "";
    } catch (erro) {
        bloqueado = true; el("painel-pesos").hidden = true;
        el("recarregar").hidden = encerrada || [404, 409].includes(erro.status);
        el("mensagem").textContent = erro.message;
    } finally { estado(false); }
}
el("avancar").addEventListener("click", async () => {
    if (ocupado || !editavel()) return;
    const revisao = emRevisao();
    if (!revisao && !await window.BussolaEtapas.confirmarAvanco()) return;
    estado(true);
    el("mensagem").textContent = revisao ? "Salvando alterações…" : "Salvando pesos e avançando…";
    el("recarregar").hidden = true;
    try {
        const corpo = { criterios:dados.criterios.map(item => ({ id:item.id, peso:item.peso })) };
        const resposta = await api.requisitar("/decisoes/" + id + "/pesos" + (revisao ? "" : "/concluir"),
            revisao ? "PUT" : "POST", corpo);
        if (revisao) {
            dados = resposta; referencia = foto(); renderizar(); el("mensagem").textContent = "Alterações salvas.";
        } else location.assign("analise.html?decisaoId=" + encodeURIComponent(id));
    } catch (erro) {
        const recarregar = erro.incerto || erro.status >= 500 || [404, 409].includes(erro.status);
        bloqueado = recarregar; el("recarregar").hidden = !recarregar;
        el("mensagem").textContent = recarregar ? erro.message + " Recarregue os dados antes de continuar." : erro.message;
    } finally { estado(false); }
});
el("recarregar").addEventListener("click", () => window.BussolaAlteracoes.permitir(el("recarregar"), carregar));
window.addEventListener("pageshow", evento => { if (evento.persisted) location.reload(); });
carregar();
