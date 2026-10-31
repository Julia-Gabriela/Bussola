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
const niveis = ["", "Baixo", "Moderado", "Importante", "Muito importante", "Essencial"];
const icones = { "Segurança":"seguranca", "Crescimento":"crescimento", "Risco":"risco",
    "Impacto emocional":"emocional", "Alinhamento a valores":"valores" };
const ajudas = {
    "Segurança": ["Pode merecer peso maior quando estabilidade e previsibilidade são fundamentais para você.",
        "Ex.: quando você precisa de renda garantida ou de uma rotina previsível."],
    "Crescimento": ["Pode merecer peso maior quando aprender e evoluir é prioridade.",
        "Ex.: quando você busca novas habilidades ou avanço na carreira."],
    "Risco": ["Pode merecer peso maior quando evitar incertezas é especialmente importante.",
        "Um peso maior em Risco significa dar mais importância a evitar riscos: a alternativa com menor nota de risco ganha mais pontos."],
    "Impacto emocional": ["Pode merecer peso maior quando o bem-estar é central nesta decisão.",
        "Ex.: quando a decisão afeta sua saúde, sua rotina ou suas relações."],
    "Alinhamento a valores": ["Pode merecer peso maior quando princípios e prioridades pessoais têm grande importância.",
        "Ex.: quando a escolha precisa combinar com o que você acredita."]
};
const AJUDA_PERSONALIZADO = ["Este é um critério que você criou. Dê peso maior se ele pesa muito na sua escolha e peso menor se ele é só um detalhe.",
    "O peso vale igual para as duas alternativas."];
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
    el("salvar-depois").disabled = valor || !editavel();
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
/**
 * Monta o texto de um nível de peso, como "3 — Importante".
 *
 * @param {number} peso valor inteiro de 1 a 5
 * @returns {string} número e rótulo separados por travessão
 */
function rotuloNivel(peso) {
    return peso + " — " + niveis[peso];
}
/**
 * Atualiza a soma dos pesos e a participação percentual de cada critério.
 *
 * A participação é só visual: peso / somaDosPesos × 100, arredondada para o
 * inteiro mais próximo, no formato "Segurança representa 36% da importância
 * total (4 de 11).". Não altera os pesos nem o cálculo do resultado, que
 * continua no backend usando os valores de 1 a 5.
 *
 * @returns {void}
 */
function atualizarSoma() {
    const lista = dados.criterios;
    const soma = lista.reduce((total, criterio) => total + criterio.peso, 0);
    const maximo = lista.length * 5;
    el("soma-pesos").textContent = soma;
    el("barra-total").setAttribute("aria-valuemax", String(maximo));
    el("barra-total").setAttribute("aria-valuenow", String(soma));
    el("barra-total-valor").style.width = String(maximo ? soma / maximo * 100 : 0) + "%";
    lista.forEach(criterio => {
        const alvo = document.querySelector(`[data-participacao="${criterio.id}"]`);
        if (!alvo) return;
        const percentual = soma ? Math.round(criterio.peso / soma * 100) : 0;
        alvo.textContent = criterio.nome + " representa " + percentual + "% da importância total ("
            + criterio.peso + " de " + soma + ").";
    });
}
/**
 * Cria o botão "?" e o painel com exemplos de quando o critério pode merecer peso maior.
 *
 * Usa o componente compartilhado de ajuda contextual (ajuda-contextual.js):
 * o botão controla o painel por aria-controls e abre com clique, toque ou
 * teclado. Critérios personalizados recebem uma explicação genérica.
 *
 * @param {{id: number, nome: string}} criterio critério exibido no cartão
 * @returns {{botao: HTMLButtonElement, painel: HTMLDivElement}} elementos a inserir no cartão
 */
function criarAjuda(criterio) {
    const idPainel = "ajuda-peso-" + criterio.id;
    const botao = criar("button", "ajuda-botao");
    botao.type = "button";
    botao.setAttribute("aria-expanded", "false");
    botao.setAttribute("aria-controls", idPainel);
    const simbolo = criar("span", "", "?");
    simbolo.setAttribute("aria-hidden", "true");
    botao.append(simbolo, criar("span", "ajuda-sr", "Quando " + criterio.nome + " pode merecer peso maior"));
    const painel = criar("div", "ajuda-painel");
    painel.id = idPainel;
    painel.hidden = true;
    (ajudas[criterio.nome] || AJUDA_PERSONALIZADO).forEach(texto => painel.append(criar("p", "", texto)));
    const fechar = criar("button", "ajuda-fechar", "×");
    fechar.type = "button";
    fechar.setAttribute("aria-label", "Fechar ajuda");
    painel.append(fechar);
    return { botao, painel };
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
        const titulo = criar("h2", "", criterio.nome);
        const ajuda = criarAjuda(criterio);
        titulo.append(ajuda.botao);
        texto.append(titulo);
        texto.append(criar("p", "", criterio.descricao || "Critério personalizado"));
        const nivel = criar("span", "peso-nivel", rotuloNivel(criterio.peso));
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
            nivel.textContent = rotuloNivel(peso);
            valor.value = String(peso); valor.textContent = String(peso);
            slider.setAttribute("aria-valuetext", peso + ": " + niveis[peso]);
            trilho.style.setProperty("--peso-posicao", String((peso - 1) * 25) + "%");
            trilho.querySelectorAll(".peso-trilho-ponto").forEach((ponto, indice) => ponto.classList.toggle("passado", indice + 1 <= peso));
            atualizarSoma();
        });
        const escala = criar("div", "peso-escala");
        for (let marca = 1; marca <= 5; marca++) escala.append(criar("span", "", marca));
        const participacao = criar("p", "peso-participacao");
        participacao.dataset.participacao = String(criterio.id);
        controle.append(trilho, slider, escala); card.append(cabecalho, ajuda.painel, controle, participacao);
        return card;
    }));
    atualizarSoma();
    el("painel-pesos").hidden = false;
    el("avancar").hidden = !editavel();
    el("salvar-depois").hidden = !editavel() || emRevisao();
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
        if (resposta.etapaAtual === 6 && !window.BussolaRevisao.emRevisao(resposta.etapaAtual, 5)) {
            location.replace("analise.html?decisaoId=" + encodeURIComponent(id) + "&etapa=6");
            return;
        }
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
        } else if (resposta.etapaAtual === 6) {
            dados = resposta;
            referencia = foto();
            estado(false);
            location.replace("analise.html?decisaoId=" + encodeURIComponent(id) + "&etapa=6");
        } else {
            throw new Error("Os pesos foram salvos, mas a decisão não avançou. Recarregue os dados antes de tentar novamente.");
        }
    } catch (erro) {
        const recarregar = erro.incerto || erro.status >= 500 || [404, 409].includes(erro.status);
        bloqueado = recarregar; el("recarregar").hidden = !recarregar;
        el("mensagem").textContent = recarregar ? erro.message + " Recarregue os dados antes de continuar." : erro.message;
    } finally { estado(false); }
});
/**
 * Salva os pesos atuais sem concluir a etapa ("Salvar para depois").
 *
 * Usa o endpoint existente PUT /decisoes/{id}/pesos, que grava todos os pesos
 * de 1 a 5 e mantém etapaAtual. Depois do sucesso, a resposta do backend vira
 * a nova referência, então a proteção de alterações não salvas deixa de
 * bloquear a saída. Em falha, os pesos escolhidos continuam na tela e a
 * mensagem explica o problema; se a resposta for incerta (rede, 5xx, 404 ou
 * 409), pede para recarregar antes de continuar, como o botão de avanço.
 *
 * @returns {Promise<void>} Resolve depois de mostrar o resultado do salvamento.
 */
async function salvarDepois() {
    if (ocupado || !editavel()) return;
    estado(true);
    el("mensagem").textContent = "Salvando pesos…";
    el("recarregar").hidden = true;
    try {
        const corpo = { criterios:dados.criterios.map(item => ({ id:item.id, peso:item.peso })) };
        dados = await api.requisitar("/decisoes/" + id + "/pesos", "PUT", corpo);
        referencia = foto();
        renderizar();
        el("mensagem").textContent = "Pesos salvos. Você pode continuar depois, de onde parou.";
    } catch (erro) {
        const recarregar = erro.incerto || erro.status >= 500 || [404, 409].includes(erro.status);
        bloqueado = recarregar; el("recarregar").hidden = !recarregar;
        el("mensagem").textContent = recarregar ? erro.message + " Recarregue os dados antes de continuar." : erro.message;
    } finally { estado(false); }
}
el("salvar-depois").addEventListener("click", salvarDepois);
el("recarregar").addEventListener("click", () => window.BussolaAlteracoes.permitir(el("recarregar"), carregar));
window.addEventListener("pageshow", evento => { if (evento.persisted) location.reload(); });
carregar();
