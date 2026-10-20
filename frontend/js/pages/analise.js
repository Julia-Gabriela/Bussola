import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";
import { requisitosAnalise, marcarRequisito } from "../shared/requisitos-resultado.js";

const el = id => document.getElementById(id);
const id = new URLSearchParams(location.search).get("decisaoId");
let dados, ocupado = false, bloqueado = false, encerrada = false;
let fatorId = null, exclusao = null, referencia = "";
/**
 * Formulário de pró/contra aberto em um card, ou null quando nenhum está aberto.
 * Guarda o ID real da opção, o tipo e, na edição, o ID do registro e o texto salvo.
 * @type {{opcaoId: number, letra: "a"|"b", tipo: "PRO"|"CONTRA", id: number|null, original: string, origem: HTMLElement|null}|null}
 */
let argumento = null;
const api = criarApi(() => { encerrada = true; location.replace("login.html?motivo=sessao-expirada"); });
const editavel = () => dados?.status === "EM_ANDAMENTO" && !bloqueado && !encerrada;
const fotoGut = () => JSON.stringify([el("gut-descricao").value, ...["gravidade", "urgencia", "tendencia"].map(campo => el(campo).value)]);
const sujoGut = () => referencia !== "" && fotoGut() !== referencia;

/**
 * Diz se o formulário de pró/contra aberto tem texto diferente do salvo.
 *
 * Abrir um formulário vazio, ou abrir a edição sem mudar nada, não conta
 * como alteração: só o conteúdo efetivamente modificado.
 *
 * @returns {boolean} true quando há rascunho de pró/contra não salvo.
 */
const sujoArgumento = () => argumento !== null && el("argumento-descricao").value !== argumento.original;

/**
 * Diz se há algo nos formulários que ainda não foi gravado.
 *
 * Em Prós e Contras, só o texto modificado conta. Na Matriz GUT, mantém a
 * regra existente: rascunho digitado ou fator aberto em edição.
 *
 * @returns {boolean} true quando sair agora descartaria um rascunho.
 */
const pendente = () => sujoArgumento() || sujoGut() || fatorId !== null;
window.BussolaAlteracoes.registrar({ sujo: pendente, ocupado: () => ocupado });
function aviso(titulo, texto) {
    el("aviso-titulo").textContent = titulo; el("aviso-texto").textContent = texto;
    if (!el("aviso").open) el("aviso").showModal();
}
function estado(valor) {
    ocupado = valor;
    el("conteudo").setAttribute("aria-busy", String(valor));
    el("argumento-campos").disabled = valor || !editavel();
    el("gut-campos").disabled = valor || !editavel();
    document.querySelectorAll(".registro-acoes button, .argumentos-adicionar").forEach(botao => { botao.disabled = valor || !editavel(); });
    el("excluir").disabled = valor || !editavel();
    el("recarregar").disabled = valor;
    el("salvar-depois").disabled = valor || !editavel();
    el("proxima-tecnica").disabled = valor;
}
const preencherConta = configurarConta({ requisitar: api.requisitar, aviso, podeSair: () => !ocupado,
    aoSair: () => { referencia = ""; argumento = null; }, ocupado: estado, rotas: { historico: "historico.html" } });
const EXEMPLOS = { PRO: "Ex.: Maior flexibilidade de horários", CONTRA: "Ex.: Custo mensal mais elevado" };
const VAZIOS = { PRO: "Nenhum pró cadastrado para esta alternativa.", CONTRA: "Nenhum contra cadastrado para esta alternativa." };
const NOMES = { PRO: "pró", CONTRA: "contra" };

/**
 * Exibe uma mensagem de retorno perto do formulário da técnica.
 *
 * Substitui qualquer mensagem anterior da mesma técnica, evitando que avisos
 * antigos de sucesso permaneçam visíveis depois de novas interações.
 *
 * @param {"pros"|"gut"} painel Técnica cujo formulário recebe a mensagem.
 * @param {string} texto Mensagem a exibir; texto vazio oculta o retorno.
 * @param {"sucesso"|"erro"|""} [tipo=""] Define a cor da mensagem.
 * @returns {void}
 */
function retorno(painel, texto, tipo = "") {
    const no = el("feedback-" + painel);
    no.textContent = texto;
    no.dataset.tipo = tipo;
}

/**
 * Remove as mensagens de retorno das duas técnicas.
 *
 * Chamada quando o usuário volta a interagir (digita, abre um formulário,
 * troca de técnica ou começa uma edição), para não confundir o estado atual com o anterior.
 *
 * @returns {void}
 */
function limparRetornos() { retorno("pros", ""); retorno("gut", ""); }

/**
 * Localiza o card de uma lista de Prós e Contras.
 *
 * @param {"a"|"b"} letra Alternativa do grupo (A é a primeira opção de /analise, B a segunda).
 * @param {"PRO"|"CONTRA"} tipo Lista desejada dentro do grupo.
 * @returns {HTMLElement} Elemento `section.argumentos-card` correspondente.
 */
const cartao = (letra, tipo) => document.querySelector(`.argumentos-alternativa[data-letra="${letra}"] .argumentos-card[data-tipo="${tipo}"]`);

/**
 * Coloca a mensagem de retorno de Prós e Contras dentro de um card.
 *
 * @param {HTMLElement|null} card Card que recebe a mensagem; null devolve a
 *        mensagem ao final do painel (usada quando nenhum card está envolvido).
 * @returns {void}
 */
function posicionarRetorno(card) {
    (card ? card.querySelector(".argumentos-slot") : el("retorno-geral-pros")).append(el("feedback-pros"));
}

/**
 * Abre o formulário de pró/contra dentro do card escolhido.
 *
 * O formulário já sabe a alternativa (pelo ID real da opção vindo da API) e o
 * tipo, então o usuário só escreve a descrição. Na edição, também guarda o ID
 * do registro e preenche o texto atual. Se houver outro rascunho com texto
 * modificado, nada é aberto e a pessoa é orientada a salvar ou cancelar.
 *
 * @param {"a"|"b"} letra Alternativa do card.
 * @param {"PRO"|"CONTRA"} tipo Tipo do registro.
 * @param {{id: number, descricao: string}|null} [item=null] Registro a editar; null para cadastrar.
 * @param {HTMLElement|null} [origem=null] Botão que abriu o formulário, para devolver o foco ao cancelar.
 * @returns {void}
 */
function abrirArgumento(letra, tipo, item = null, origem = null) {
    if (ocupado || !editavel()) return;
    if (sujoArgumento()) { avisarRascunho("abrir outro formulário"); return; }
    fecharArgumento(false); limparRetornos();
    const opcao = dados.opcoes[letra === "a" ? 0 : 1];
    argumento = { opcaoId: opcao.id, letra, tipo, id: item?.id ?? null, original: item?.descricao ?? "", origem };
    const card = cartao(letra, tipo);
    card.querySelector(".argumentos-slot").prepend(el("argumento-form"));
    posicionarRetorno(card);
    card.querySelector(".argumentos-adicionar").hidden = true;
    if (item) card.querySelector(`li[data-id="${item.id}"]`)?.classList.add("em-edicao");
    el("argumento-rotulo").textContent = (item ? "Editar " : "Novo ") + NOMES[tipo];
    el("argumento-alternativa").textContent = " da alternativa " + letra.toUpperCase() + " — " + opcao.nome;
    el("salvar-argumento").textContent = item ? "Salvar alterações" : "Salvar";
    el("argumento-descricao").placeholder = EXEMPLOS[tipo];
    el("argumento-descricao").value = argumento.original;
    el("argumento-descricao").setCustomValidity("");
    el("argumento-form").hidden = false; estado(ocupado);
    el("argumento-descricao").focus();
}

/**
 * Fecha o formulário de pró/contra e descarta apenas o rascunho dele.
 *
 * Não grava nem apaga nada no backend.
 *
 * @param {boolean} [focar=true] Devolve o foco ao botão que abriu o formulário
 *        (ou ao "+ Adicionar" do card, se esse botão não existir mais).
 * @returns {void}
 */
function fecharArgumento(focar = true) {
    if (!argumento) return;
    const { letra, tipo, origem } = argumento;
    argumento = null;
    el("argumento-form").hidden = true; el("argumento-form").reset();
    const adicionar = cartao(letra, tipo).querySelector(".argumentos-adicionar");
    adicionar.hidden = !editavel();
    document.querySelectorAll("#painel-pros .em-edicao").forEach(no => no.classList.remove("em-edicao"));
    if (focar) (origem?.isConnected ? origem : adicionar).focus();
}

function limparGut() {
    fatorId = null;
    el("gut-form").reset(); el("gut-titulo").textContent = "Adicionar fator";
    document.querySelectorAll("#painel-gut .em-edicao").forEach(no => no.classList.remove("em-edicao"));
    referencia = fotoGut(); estado(ocupado);
}
function criar(tag, texto) { const no = document.createElement(tag); no.textContent = texto; return no; }

/**
 * Atualiza o resumo do que falta para liberar o resultado comparativo.
 *
 * Usa apenas os dados já carregados de /analise e as mesmas condições que o
 * backend exige: ao menos um pró e um contra em cada alternativa e ao menos um
 * fator GUT. A conclusão do 10/10/10 e da Inversão não vem nessa resposta, por
 * isso esses itens apenas orientam onde concluir, sem afirmar um estado.
 *
 * @returns {void}
 */
function atualizarPendencias() {
    const situacao = requisitosAnalise(dados);
    for (const requisito of ["pros", "gut"])
        marcarRequisito(el("pendencias"), requisito, situacao[requisito].pronto, situacao[requisito].texto);
}

/**
 * Cria os botões Editar e Excluir de um registro.
 *
 * @param {{id: number, descricao: string, tipo?: string}} item Registro exibido.
 * @param {"argumento"|"gut"} tipo Técnica do registro.
 * @param {"a"|"b"} [letra] Alternativa do pró/contra; não se aplica à GUT.
 * @returns {HTMLDivElement} Contêiner com os botões (vazio se a decisão não for editável).
 */
function acoes(item, tipo, letra) {
    const div = document.createElement("div"); div.className = "registro-acoes";
    if (!editavel()) return div;
    const editar = criar("button", "Editar"), excluir = criar("button", "Excluir");
    editar.type = excluir.type = "button";
    editar.setAttribute("aria-label", "Editar " + item.descricao); excluir.setAttribute("aria-label", "Excluir " + item.descricao);
    if (tipo === "argumento") {
        editar.addEventListener("click", () => abrirArgumento(letra, item.tipo, item, editar));
        excluir.addEventListener("click", () => {
            if (ocupado) return;
            if (sujoArgumento()) { avisarRascunho("excluir outro registro"); return; }
            fecharArgumento(false); limparRetornos(); posicionarRetorno(cartao(letra, item.tipo));
            exclusao = { tipo, id: item.id, opcao: dados.opcoes[letra === "a" ? 0 : 1].id, nome: item.tipo === "PRO" ? "Pró" : "Contra", origem: excluir };
            el("exclusao-texto").textContent = item.descricao; el("exclusao").showModal();
        });
    } else {
        editar.addEventListener("click", () => window.BussolaAlteracoes.permitir(editar, () => {
            limparGut(); limparRetornos();
            editar.closest("tr")?.classList.add("em-edicao");
            fatorId = item.id; el("gut-descricao").value = item.descricao;
            ["gravidade", "urgencia", "tendencia"].forEach(campo => { el(campo).value = item[campo]; });
            el("gut-titulo").textContent = "Editar fator"; el("gut-descricao").focus();
            referencia = fotoGut(); estado(false);
        }));
        excluir.addEventListener("click", () => window.BussolaAlteracoes.permitir(excluir, () => {
            limparRetornos();
            exclusao = { tipo, id: item.id, nome: "Fator" };
            el("exclusao-texto").textContent = item.descricao; el("exclusao").showModal();
        }));
    }
    div.append(editar, excluir); return div;
}

/**
 * Desenha as quatro listas de Prós e Contras (A e B, cada uma com prós e contras).
 *
 * Cada grupo usa a opção da mesma posição na resposta de /analise (A = primeira,
 * B = segunda, como no backend) e cada registro guarda o próprio ID em data-id.
 *
 * @returns {void}
 */
function renderizarArgumentos() {
    dados.opcoes.forEach((opcao, i) => {
        const letra = i === 0 ? "a" : "b";
        document.querySelector(`.argumentos-alternativa[data-letra="${letra}"] [data-nome-opcao]`).textContent = opcao.nome;
        for (const [tipo, titulo] of [["PRO", "Prós"], ["CONTRA", "Contras"]]) {
            const card = cartao(letra, tipo);
            const itens = opcao.prosContras.filter(item => item.tipo === tipo);
            card.querySelector("h3").textContent = titulo + " (" + itens.length + ")";
            card.querySelector("ul").replaceChildren(...itens.map(item => {
                const li = document.createElement("li"); li.dataset.id = item.id;
                if (argumento?.id === item.id) li.classList.add("em-edicao");
                li.append(criar("p", item.descricao), acoes(item, "argumento", letra)); return li;
            }));
            if (!itens.length) { const vazio = criar("li", VAZIOS[tipo]); vazio.className = "lista-vazia"; card.querySelector("ul").append(vazio); }
            card.querySelector(".argumentos-adicionar").hidden = !editavel() || (argumento?.letra === letra && argumento?.tipo === tipo);
        }
    });
}
function renderizar() {
    el("decisao-titulo").textContent = dados.titulo;
    el("abrir-inversao").href = "inversao.html?decisaoId=" + encodeURIComponent(id);
    el("abrir-101010").href = "metodo-101010.html?decisaoId=" + encodeURIComponent(id);
    el("gut-form").hidden = el("salvar-depois").hidden = dados.status !== "EM_ANDAMENTO";
    window.BussolaRevisao.sincronizar({ etapaAtual: dados.etapaAtual, status: dados.status, etapaPagina: 6, decisaoId: id });
    renderizarArgumentos();
    const maior = Math.max(...dados.fatoresGut.map(item => item.prioridade));
    el("fatores").replaceChildren(...dados.fatoresGut.map(item => {
        const linha = document.createElement("tr");
        const fator = criar("td", item.descricao);
        if (dados.fatoresGut.length > 1 && item.prioridade === maior) {
            linha.classList.add("gut-topo");
            const selo = criar("span", "Maior prioridade"); selo.className = "gut-selo"; fator.append(selo);
        }
        linha.append(fator);
        for (const campo of ["gravidade", "urgencia", "tendencia"]) { const celula = criar("td", item[campo]); celula.className = "gut-nota"; linha.append(celula); }
        const prioridade = document.createElement("td"); prioridade.className = "gut-nota";
        prioridade.append(criar("strong", item.prioridade)); linha.append(prioridade);
        const celula = document.createElement("td"); celula.append(acoes(item, "gut")); linha.append(celula); return linha;
    }));
    if (!dados.fatoresGut.length) {
        const linha = document.createElement("tr"), celula = criar("td", "Nenhum fator cadastrado. Adicione acima um problema ou fator que pode afetar sua decisão.");
        celula.colSpan = 6; celula.className = "lista-vazia"; linha.append(celula); el("fatores").append(linha);
    }
    atualizarPendencias();
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
        fecharArgumento(false); limparGut(); renderizar(); el("recarregar").hidden = true;
        el("mensagem").textContent = dados.status === "CONCLUIDA" ? "Decisão concluída: somente consulta." : "";
    } catch (erro) { bloqueado = true; el("analise").hidden = true; el("recarregar").hidden = false; el("mensagem").textContent = erro.message; }
    finally { estado(false); }
    if (dados && !bloqueado && new URLSearchParams(location.search).get("tecnica") === "gut") abrirAba("gut");
}
/**
 * Envia uma alteração de Prós e Contras ou da Matriz GUT e atualiza a tela.
 *
 * O retorno aparece perto do formulário da técnica (em Prós e Contras, dentro
 * do card envolvido). Em caso de sucesso, o formulário de pró/contra é fechado
 * e o foco volta ao "+ Adicionar" do card; em erro simples, o formulário e o
 * texto digitado permanecem. Falhas que deixam o estado salvo incerto bloqueiam
 * novas edições e exibem o aviso do topo, junto a "Recarregar dados", que recebe o foco.
 *
 * @param {string} caminho Rota da API relativa ao backend.
 * @param {"POST"|"PUT"|"DELETE"} metodo Método HTTP usado na requisição.
 * @param {object|undefined} corpo Dados enviados; ausente na exclusão.
 * @param {"pros"|"gut"} painel Técnica que recebe a mensagem de retorno.
 * @param {string} sucesso Mensagem exibida quando a alteração é confirmada.
 * @returns {Promise<void>} Resolve depois que a tela reflete o resultado.
 */
async function salvar(caminho, metodo, corpo, painel, sucesso) {
    if (ocupado || !editavel()) return;
    const card = painel === "pros" ? el("feedback-pros").closest(".argumentos-card") : null;
    estado(true); limparRetornos(); el("mensagem").textContent = ""; retorno(painel, "Salvando…");
    let confirmado = false;
    try {
        await api.requisitar(caminho, metodo, corpo); confirmado = true;
        if (painel === "gut") limparGut(); else fecharArgumento(false);
        await consultar(); renderizar(); retorno(painel, sucesso, "sucesso");
    } catch (erro) {
        bloqueado = confirmado || erro.incerto || erro.status >= 500 || [404,409].includes(erro.status);
        el("recarregar").hidden = !bloqueado;
        if (bloqueado) {
            retorno(painel, "");
            el("mensagem").textContent = confirmado ? "Alteração salva, mas não foi possível atualizar a tela. Recarregue os dados antes de continuar."
                : erro.message.includes("Recarregue") ? erro.message : erro.message + " Recarregue os dados antes de continuar.";
        } else retorno(painel, erro.message, "erro");
    } finally { estado(false); }
    if (bloqueado) el("recarregar").focus();
    else if (!confirmado && argumento) el("argumento-descricao").focus();
    else if (confirmado) (painel === "gut" ? el("gut-descricao") : card?.querySelector(".argumentos-adicionar"))?.focus();
}
for (const campo of ["argumento-descricao", "gut-descricao"]) el(campo).addEventListener("input", () => el(campo).setCustomValidity(""));
for (const formulario of ["argumento-form", "gut-form"]) {
    el(formulario).addEventListener("input", limparRetornos);
    el(formulario).addEventListener("change", limparRetornos);
}
document.querySelectorAll(".argumentos-adicionar").forEach(botao => botao.addEventListener("click", () => {
    abrirArgumento(botao.closest(".argumentos-alternativa").dataset.letra, botao.closest(".argumentos-card").dataset.tipo, null, botao);
}));
el("argumento-form").addEventListener("submit", evento => {
    evento.preventDefault();
    if (!argumento) return;
    const descricao = el("argumento-descricao").value.trim();
    el("argumento-descricao").setCustomValidity(descricao ? "" : "Descreva o " + NOMES[argumento.tipo] + ".");
    if (!el("argumento-form").reportValidity()) return;
    const { opcaoId, tipo, id: registro } = argumento;
    const nome = tipo === "PRO" ? "Pró" : "Contra";
    salvar(`/decisoes/${id}/opcoes/${opcaoId}/pros-contras${registro === null ? "" : "/" + registro}`,
        registro === null ? "POST" : "PUT", { tipo, descricao },
        "pros", nome + (registro === null ? " adicionado." : " atualizado."));
});
el("argumento-descricao").addEventListener("keydown", evento => {
    if (evento.key === "Escape" && !ocupado) { evento.preventDefault(); fecharArgumento(); limparRetornos(); }
});
el("gut-form").addEventListener("submit", evento => {
    evento.preventDefault(); const descricao = el("gut-descricao").value.trim();
    el("gut-descricao").setCustomValidity(descricao ? "" : "Descreva o fator.");
    if (!el("gut-form").reportValidity()) return;
    salvar(`/decisoes/${id}/gut${fatorId === null ? "" : "/" + fatorId}`, fatorId === null ? "POST" : "PUT",
        { descricao, gravidade: Number(el("gravidade").value), urgencia: Number(el("urgencia").value), tendencia: Number(el("tendencia").value) },
        "gut", fatorId === null ? "Fator adicionado e posicionado pela prioridade." : "Fator atualizado e reposicionado pela prioridade.");
});
el("cancelar-argumento").addEventListener("click", () => { if (!ocupado) { fecharArgumento(); limparRetornos(); } });
el("cancelar-gut").addEventListener("click", () => { limparGut(); limparRetornos(); });
/**
 * Explica por que a ação foi impedida e como liberá-la.
 *
 * Ao fechar o aviso, o foco volta ao campo do rascunho pendente (pró/contra
 * ou fator GUT), para a pessoa continuar a digitação sem procurar o formulário.
 *
 * @param {string} destino Complemento da frase, por exemplo "trocar de técnica".
 * @returns {void}
 */
function avisarRascunho(destino) {
    aviso("Alterações não salvas", "Há um pró, contra ou fator em edição que ainda não foi salvo. Clique em “Salvar” para gravá-lo ou em “Cancelar” para descartar a edição antes de " + destino + ".");
    el("aviso").addEventListener("close", () => {
        if (sujoArgumento()) el("argumento-descricao").focus();
        else if (sujoGut() || fatorId !== null) el("gut-descricao").focus();
    }, { once: true });
}

/**
 * Indica qual técnica desta página está visível.
 *
 * @returns {"pros"|"gut"} Painel aberto no momento.
 */
const painelAtivo = () => el("painel-gut").hidden ? "pros" : "gut";

function abrirAba(tipo) {
    if (ocupado) return;
    if (pendente()) { avisarRascunho("trocar de técnica"); return; }
    fecharArgumento(false); limparRetornos();
    for (const nome of ["pros", "gut"]) { el("painel-" + nome).hidden = nome !== tipo; if (nome === tipo) el("aba-" + nome).setAttribute("aria-current", "true"); else el("aba-" + nome).removeAttribute("aria-current"); }
    el("proxima-texto").textContent = tipo === "pros" ? "Próxima: Matriz GUT" : "Próxima: 10/10/10";
    const url = new URL(location.href); url.searchParams.set("tecnica", tipo);
    history.replaceState(history.state, "", url.href);
    el("aba-" + tipo).focus();
}
for (const tipo of ["pros", "gut"]) {
    el("aba-" + tipo).addEventListener("click", () => abrirAba(tipo));
}
el("salvar-depois").addEventListener("click", () => {
    if (ocupado || !editavel()) return;
    if (painelAtivo() === "gut") {
        if (sujoGut() || fatorId !== null) { el("gut-form").requestSubmit(); return; }
        retorno("gut", "Seus fatores já estão salvos. Você pode continuar depois pela Análise da sua decisão.", "sucesso");
        return;
    }
    if (sujoArgumento()) { el("argumento-form").requestSubmit(); return; }
    if (!argumento) posicionarRetorno(null);
    retorno("pros", "Seus prós e contras já estão salvos. Você pode continuar depois pela Análise da sua decisão.", "sucesso");
});
el("proxima-tecnica").addEventListener("click", () => {
    if (ocupado) return;
    if (painelAtivo() === "pros") { abrirAba("gut"); return; }
    if (pendente()) { avisarRascunho("seguir para o método 10/10/10"); return; }
    location.assign("metodo-101010.html?decisaoId=" + encodeURIComponent(id));
});
el("cancelar-exclusao").addEventListener("click", () => el("exclusao").close());
el("exclusao").addEventListener("close", () => {
    const origem = exclusao?.origem; exclusao = null;
    if (el("exclusao").returnValue !== "confirmado" && origem?.isConnected) origem.focus();
    el("exclusao").returnValue = "";
});
el("excluir").addEventListener("click", () => {
    if (!exclusao || ocupado) return;
    const item = exclusao; el("exclusao").close("confirmado");
    salvar(`/decisoes/${id}/` + (item.tipo === "gut" ? `gut/${item.id}` : `opcoes/${item.opcao}/pros-contras/${item.id}`), "DELETE",
        undefined, item.tipo === "gut" ? "gut" : "pros", item.nome + " excluído.");
});
el("recarregar").addEventListener("click", () => {
    if (bloqueado) { if (!pendente() || window.confirm("Descartar o rascunho e conferir o que foi salvo?")) carregar(); }
    else window.BussolaAlteracoes.permitir(el("recarregar"), carregar);
});
window.addEventListener("pageshow", evento => { if (evento.persisted) location.reload(); });
carregar();
