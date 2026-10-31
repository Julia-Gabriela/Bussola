import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js";
import { requisitosAnalise, marcarRequisito } from "../shared/requisitos-resultado.js";
import { estadoTecnicas, marcarNavegacao } from "../shared/tecnicas-estado.js?v=1";

const el = id => document.getElementById(id);
const id = new URLSearchParams(location.search).get("decisaoId");
let dados, ocupado = false, bloqueado = false, encerrada = false;
let exclusao = null;
/**
 * Formulário de pró/contra aberto em um card, ou null quando nenhum está aberto.
 * Guarda o ID real da opção, o tipo e, na edição, o ID do registro e o texto salvo.
 * @type {{opcaoId: number, letra: "a"|"b", tipo: "PRO"|"CONTRA", id: number|null, original: string, origem: HTMLElement|null}|null}
 */
let argumento = null;
const CAMPOS_GUT = ["gravidade", "urgencia", "tendencia"];
const NOMES_GUT = { gravidade: "Gravidade", urgencia: "Urgência", tendencia: "Tendência" };
/**
 * Notas GUT escolhidas na tela que ainda diferem do que o servidor confirmou.
 * Chave "p{id}" para um pró/contra e "f{id}" para um fator antigo, digitado à mão.
 * Só guarda itens com diferença: notas iguais às salvas saem do mapa.
 * @type {Map<string, {gravidade: string, urgencia: string, tendencia: string}>}
 */
const rascunhosGut = new Map();
/** Quantos salvamentos de cada item estão na fila ou em andamento. @type {Map<string, number>} */
const filaPorItem = new Map();
/** Última falha de salvamento de cada item, exibida na linha. @type {Map<string, string>} */
const errosGut = new Map();
let filaGut = Promise.resolve();
let salvamentosGut = 0;
const api = criarApi(() => { encerrada = true; location.replace("login.html?motivo=sessao-expirada"); });
const editavel = () => dados?.status === "EM_ANDAMENTO" && !bloqueado && !encerrada;

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
 * Diz se alguma classificação GUT está na tela sem estar salva nem a caminho do servidor.
 *
 * Notas incompletas e salvamentos que falharam contam; um salvamento ainda na
 * fila não conta, porque a saída apenas espera ele terminar.
 *
 * @returns {boolean} true quando sair agora perderia notas GUT.
 */
const sujoGut = () => [...rascunhosGut.keys()].some(chave => !filaPorItem.get(chave));

/**
 * Diz se há algo nos formulários que ainda não foi gravado.
 *
 * @returns {boolean} true quando sair agora descartaria um rascunho.
 */
const pendente = () => sujoArgumento() || sujoGut();
window.BussolaAlteracoes.registrar({
    sujo: pendente,
    ocupado: () => ocupado || salvamentosGut > 0,
    aguardar: () => filaGut
});
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
    aoSair: () => { rascunhosGut.clear(); argumento = null; }, ocupado: estado, rotas: { historico: "historico.html" } });
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
 * Abre o formulário de pró/contra no topo do card escolhido, acima da lista.
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
    el("salvar-argumento").textContent = item ? "Salvar alterações" : "Adicionar";
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

function criar(tag, texto) { const no = document.createElement(tag); no.textContent = texto; return no; }

/**
 * Atualiza o resumo do que falta para liberar o resultado comparativo.
 *
 * Usa apenas os dados já carregados de /analise e as mesmas condições que o
 * backend exige: ao menos um pró e um contra em cada alternativa e ao menos um
 * item classificado na Matriz GUT. A conclusão do 10/10/10 e da Inversão não
 * vem nessa resposta, por isso esses itens apenas orientam onde concluir.
 *
 * @returns {void}
 */
function atualizarPendencias() {
    const situacao = requisitosAnalise(dados);
    for (const requisito of ["pros", "gut"])
        marcarRequisito(el("pendencias"), requisito, situacao[requisito].pronto, situacao[requisito].texto);
    marcarNavegacao(document.querySelector(".tecnicas-nav"), { pros: situacao.pros.pronto, gut: situacao.gut.pronto });
}

const TEXTOS_EXTERNOS = {
    "101010": ["concluída.", "falta concluir: preencha as seis reflexões e use “Próxima: Inversão”."],
    inversao: ["concluída.", "falta concluir: preencha as quatro reflexões e use “Ver resultado comparativo”."]
};

/**
 * Lê se o 10/10/10 e a Inversão estão concluídos e atualiza a barra de
 * técnicas e a lista "Para liberar o resultado comparativo".
 *
 * Usa GET /reflexoes-101010 e GET /inversao (campo `concluida`), sem gravar
 * nada. Se a leitura falhar, o item diz que não foi possível verificar.
 * As condições continuam as mesmas que o backend já exige para o Resultado.
 *
 * @returns {Promise<void>} Resolve depois de atualizar a tela.
 */
async function atualizarTecnicasExternas() {
    const estado = await estadoTecnicas(api, id, dados);
    const externas = { "101010": estado["101010"], inversao: estado.inversao };
    for (const [tecnica, concluida] of Object.entries(externas)) {
        marcarRequisito(el("pendencias"), tecnica, concluida === true,
            concluida === null ? "não foi possível verificar agora." : TEXTOS_EXTERNOS[tecnica][concluida ? 0 : 1]);
    }
    marcarNavegacao(document.querySelector(".tecnicas-nav"), externas);
}

/**
 * Cria os botões Editar e Excluir de um pró/contra, ou só Excluir de um fator GUT antigo.
 *
 * @param {{id: number, descricao: string, tipo?: string}} item Registro exibido.
 * @param {"argumento"|"gut"} tipo Técnica do registro.
 * @param {"a"|"b"} [letra] Alternativa do pró/contra; não se aplica à GUT.
 * @returns {HTMLDivElement} Contêiner com os botões (vazio se a decisão não for editável).
 */
function acoes(item, tipo, letra) {
    const div = document.createElement("div"); div.className = "registro-acoes";
    if (!editavel()) return div;
    const excluir = criar("button", "Excluir");
    excluir.type = "button";
    excluir.className = "botao-perigo";
    excluir.setAttribute("aria-label", "Excluir " + item.descricao);
    if (tipo === "argumento") {
        const editar = criar("button", "Editar");
        editar.type = "button";
        editar.setAttribute("aria-label", "Editar " + item.descricao);
        editar.addEventListener("click", () => abrirArgumento(letra, item.tipo, item, editar));
        excluir.addEventListener("click", () => {
            if (ocupado) return;
            if (sujoArgumento()) { avisarRascunho("excluir outro registro"); return; }
            fecharArgumento(false); limparRetornos(); posicionarRetorno(cartao(letra, item.tipo));
            exclusao = { tipo, id: item.id, opcao: dados.opcoes[letra === "a" ? 0 : 1].id, nome: item.tipo === "PRO" ? "Pró" : "Contra", origem: excluir };
            el("exclusao-texto").textContent = item.descricao; el("exclusao").showModal();
        });
        div.append(editar);
    } else {
        excluir.addEventListener("click", () => {
            if (ocupado) return;
            limparRetornos();
            exclusao = { tipo, id: item.id, nome: "Fator", origem: excluir };
            el("exclusao-texto").textContent = item.descricao; el("exclusao").showModal();
        });
    }
    div.append(excluir); return div;
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

/**
 * Chave de um item GUT: "p{id}" quando classifica um pró/contra, "f{id}" para um fator antigo.
 *
 * @param {{id: number, proContraId: number|null}} fator item vindo de /analise
 * @returns {string} chave usada nos rascunhos e nas linhas da tabela
 */
const chaveFator = fator => fator.proContraId != null ? "p" + fator.proContraId : "f" + fator.id;

/**
 * Procura a classificação salva de um item.
 *
 * @param {string} chave chave do item
 * @returns {object|null} fator salvo de /analise ou null se o item ainda não foi classificado
 */
const fatorSalvo = chave => dados.fatoresGut.find(fator => chaveFator(fator) === chave) || null;

/**
 * Notas confirmadas pelo servidor, como texto (vazio quando o item não foi classificado).
 *
 * @param {string} chave chave do item
 * @returns {{gravidade: string, urgencia: string, tendencia: string}}
 */
function notasSalvas(chave) {
    const fator = fatorSalvo(chave);
    return Object.fromEntries(CAMPOS_GUT.map(campo => [campo, fator ? String(fator[campo]) : ""]));
}

/**
 * Notas que a tela deve mostrar: o rascunho, se houver, ou as salvas.
 *
 * @param {string} chave chave do item
 * @returns {{gravidade: string, urgencia: string, tendencia: string}}
 */
const notasNaTela = chave => rascunhosGut.get(chave) || notasSalvas(chave);
const notasIguais = (x, y) => CAMPOS_GUT.every(campo => x[campo] === y[campo]);
const notasCompletas = notas => CAMPOS_GUT.every(campo => notas[campo] !== "");

/**
 * Atualiza a pontuação e a situação exibidas em uma linha da GUT.
 *
 * A pontuação mostrada para notas ainda não salvas é a mesma conta do
 * servidor (G × U × T); o valor oficial chega na resposta do salvamento.
 *
 * @param {string} chave chave do item
 * @returns {void}
 */
function atualizarLinhaGut(chave) {
    const linha = document.querySelector(`#gut-grupos [data-chave="${chave}"]`);
    if (!linha) return;
    const notas = notasNaTela(chave);
    const pontos = notasCompletas(notas)
        ? String(CAMPOS_GUT.reduce((produto, campo) => produto * Number(notas[campo]), 1)) : "—";
    linha.querySelector(".gut-conta").textContent = CAMPOS_GUT.map(campo => notas[campo] || "—").join(" × ") + " = ";
    linha.querySelector(".gut-pontos").textContent = pontos;
    for (const campo of CAMPOS_GUT) {
        const marcado = linha.querySelector(`input[data-campo="${campo}"]:checked`);
        if ((marcado?.value ?? "") !== notas[campo]) linha.querySelectorAll(`input[data-campo="${campo}"]`).forEach(input => { input.checked = input.value === notas[campo]; });
    }
    linha.querySelector(".gut-desfazer").hidden = !editavel() || !rascunhosGut.has(chave) || Boolean(filaPorItem.get(chave));
    const situacao = linha.querySelector(".gut-situacao");
    const erro = errosGut.get(chave);
    let texto, tipo = "";
    if (filaPorItem.get(chave)) texto = "Salvando…";
    else if (erro) { texto = "Não salvo: " + erro; tipo = "erro"; }
    else if (rascunhosGut.has(chave)) { texto = "Escolha as três notas para salvar."; tipo = "erro"; }
    else if (recemSalvos.has(chave)) { texto = "✓ Salvo"; tipo = "sucesso"; }
    else texto = "";
    situacao.textContent = texto;
    situacao.dataset.tipo = tipo;
    linha.classList.toggle("gut-pendente", rascunhosGut.has(chave) && !filaPorItem.get(chave));
    atualizarProgressoGut();
}

/**
 * Atualiza o indicador "X de Y itens classificados." da Matriz GUT.
 *
 * Conta como classificado o item que tem classificação salva no servidor
 * (o mesmo dado que a lista de requisitos usa). Notas ainda não salvas não
 * contam. Y inclui os prós e contras das duas alternativas e os fatores
 * antigos. Não muda a regra de conclusão: para o Resultado, continua
 * bastando ao menos um item classificado.
 *
 * @returns {void}
 */
function atualizarProgressoGut() {
    const chaves = [...document.querySelectorAll("#gut-grupos [data-chave]")].map(linha => linha.dataset.chave);
    const classificados = chaves.filter(chave => fatorSalvo(chave)).length;
    el("gut-progresso").hidden = !chaves.length;
    el("gut-progresso").textContent = textoProgressoGut(classificados, chaves.length);
}

/**
 * Monta a frase de progresso da Matriz GUT.
 *
 * @param {number} classificados quantidade de itens com classificação salva no servidor
 * @param {number} total quantidade de itens exibidos na Matriz GUT
 * @returns {string} frase como "3 de 10 itens classificados."
 */
const textoProgressoGut = (classificados, total) =>
    classificados + " de " + total + (total === 1 ? " item classificado." : " itens classificados.");

/** Itens cujo salvamento acabou de ser confirmado pelo servidor e ainda mostram "✓ Salvo". */
const recemSalvos = new Map();
const DURACAO_SALVO_MS = 2500;

/**
 * Mostra "✓ Salvo" por alguns segundos no cartão cujo salvamento o servidor
 * acabou de confirmar e anuncia uma única vez, para leitores de tela, a
 * confirmação com o progresso atualizado.
 *
 * Só deve ser chamada depois da resposta de sucesso da API. Passado o tempo,
 * o texto some do cartão; os dados salvos e a pontuação continuam na tela.
 * Um novo salvamento do mesmo item reinicia o tempo.
 *
 * @param {string} chave chave do item salvo ("p{id}" ou "f{id}")
 * @returns {void}
 */
function confirmarSalvo(chave) {
    clearTimeout(recemSalvos.get(chave));
    recemSalvos.set(chave, setTimeout(() => { recemSalvos.delete(chave); atualizarLinhaGut(chave); }, DURACAO_SALVO_MS));
    const total = document.querySelectorAll("#gut-grupos [data-chave]").length;
    const classificados = [...document.querySelectorAll("#gut-grupos [data-chave]")].filter(linha => fatorSalvo(linha.dataset.chave)).length;
    anunciarGut("Classificação salva. " + textoProgressoGut(classificados, total));
}

/**
 * Escreve uma mensagem na região viva oculta da Matriz GUT, lida por leitores de tela.
 *
 * A região é limpa antes de receber o texto, para que a mesma frase possa ser
 * anunciada de novo em um salvamento posterior.
 *
 * @param {string} texto mensagem a anunciar
 * @returns {void}
 */
function anunciarGut(texto) {
    const regiao = el("gut-anuncio");
    regiao.textContent = "";
    requestAnimationFrame(() => { regiao.textContent = texto; });
}

/**
 * Marca com o selo "Maior prioridade" os itens salvos com a maior pontuação.
 * Só destaca quando há mais de um item classificado.
 *
 * @returns {void}
 */
function atualizarDestaques() {
    const fatores = dados.fatoresGut;
    const maior = fatores.length > 1 ? Math.max(...fatores.map(item => item.prioridade)) : null;
    document.querySelectorAll("#gut-grupos [data-chave]").forEach(linha => {
        const fator = fatorSalvo(linha.dataset.chave);
        const topo = maior !== null && fator?.prioridade === maior && !rascunhosGut.has(linha.dataset.chave);
        linha.classList.toggle("gut-topo", topo);
        linha.querySelector(".gut-selo")?.remove();
        if (topo) {
            const selo = criar("span", "★ Maior prioridade"); selo.className = "gut-selo";
            linha.querySelector(".gut-card-cabecalho").append(selo);
        }
    });
}

/**
 * Registra a mudança de nota de uma linha e agenda o salvamento automático
 * quando as três notas estão escolhidas.
 *
 * @param {string} chave chave do item
 * @param {HTMLElement} linha cartão com os três grupos de botões de 1 a 5
 * @returns {void}
 */
function alterarGut(chave, linha) {
    const notas = Object.fromEntries(CAMPOS_GUT.map(campo => [campo, linha.querySelector(`input[data-campo="${campo}"]:checked`)?.value ?? ""]));
    if (notasIguais(notas, notasSalvas(chave))) rascunhosGut.delete(chave);
    else rascunhosGut.set(chave, notas);
    errosGut.delete(chave); limparRetornos();
    if (rascunhosGut.has(chave) && notasCompletas(notas)) enfileirarGut(chave);
    atualizarLinhaGut(chave); atualizarDestaques();
}

/**
 * Coloca o salvamento de um item na fila. Os salvamentos rodam um de cada
 * vez, na ordem das alterações, e cada um lê as notas mais recentes da linha.
 *
 * @param {string} chave chave do item
 * @returns {Promise<void>} fila atualizada, resolvida quando este salvamento termina
 */
function enfileirarGut(chave) {
    filaPorItem.set(chave, (filaPorItem.get(chave) || 0) + 1);
    salvamentosGut++;
    atualizarLinhaGut(chave);
    filaGut = filaGut.then(() => salvarGut(chave)).catch(() => {}).then(() => {
        const restantes = filaPorItem.get(chave) - 1;
        if (restantes > 0) filaPorItem.set(chave, restantes); else filaPorItem.delete(chave);
        salvamentosGut--;
        atualizarLinhaGut(chave); atualizarDestaques();
    });
    return filaGut;
}

/**
 * Envia as notas de um item: PUT /gut/pros-contras/{id} para um pró/contra
 * (cria ou atualiza, sem duplicar) ou PUT /gut/{id} para um fator antigo.
 *
 * Em caso de falha, as notas continuam na tela, a linha indica o erro e a
 * saída da página passa a avisar sobre alterações não salvas.
 *
 * @param {string} chave chave do item
 * @returns {Promise<void>} resolve depois da tentativa, com sucesso ou falha
 */
async function salvarGut(chave) {
    const notas = rascunhosGut.get(chave);
    if (bloqueado || encerrada || !notas || !notasCompletas(notas)) return;
    const corpo = Object.fromEntries(CAMPOS_GUT.map(campo => [campo, Number(notas[campo])]));
    try {
        const salvo = chave.startsWith("p")
            ? await api.requisitar(`/decisoes/${id}/gut/pros-contras/${chave.slice(1)}`, "PUT", corpo)
            : await api.requisitar(`/decisoes/${id}/gut/${chave.slice(1)}`, "PUT", { descricao: fatorSalvo(chave).descricao, ...corpo });
        const indice = dados.fatoresGut.findIndex(fator => chaveFator(fator) === chave);
        if (indice >= 0) dados.fatoresGut[indice] = salvo; else dados.fatoresGut.push(salvo);
        const atual = rascunhosGut.get(chave);
        if (atual && notasIguais(atual, notasSalvas(chave))) rascunhosGut.delete(chave);
        errosGut.delete(chave);
        if (!rascunhosGut.has(chave)) confirmarSalvo(chave);
        atualizarPendencias();
    } catch (erro) {
        if (encerrada) return;
        errosGut.set(chave, erro.status ? erro.message : "não foi possível falar com o servidor.");
        clearTimeout(recemSalvos.get(chave)); recemSalvos.delete(chave);
        el("gut-anuncio").textContent = "";
        if (erro.status === 404 || erro.status === 409) {
            bloqueado = true; el("recarregar").hidden = false; estado(ocupado);
            el("mensagem").textContent = erro.message + " Recarregue os dados antes de continuar.";
        } else {
            retorno("gut", "Não foi possível salvar uma classificação. Suas notas continuam na tela: clique em “Salvar para depois” para tentar de novo.", "erro");
        }
    }
}

/** Perguntas-guia de cada fator; só orientam, a escala de 1 a 5 continua a mesma. */
const PERGUNTAS_GUT = {
    gravidade: "Qual é o tamanho do impacto?",
    urgencia: "Quão rapidamente isso precisa de atenção?",
    tendencia: "Qual é a chance de piorar com o tempo?"
};
/** Significado das extremidades da escala de cada fator, iguais para prós e contras. */
const EXTREMOS_GUT = {
    gravidade: ["1 = pouco impacto", "5 = muito impacto"],
    urgencia: ["1 = pode esperar", "5 = precisa de atenção agora"],
    tendencia: ["1 = não tende a piorar", "5 = tende a piorar rapidamente"]
};
let sequenciaCartao = 0;

/**
 * Cria o cartão de um item da GUT, com botões de 1 a 5 para cada fator.
 *
 * O cartão mostra se é Pró ou Contra, a descrição, os três fatores com nome
 * completo, pergunta-guia e o significado das notas 1 e 5, a conta G × U × T
 * com as notas atuais e a situação do salvamento. O nome da alternativa não
 * aparece no cartão porque já está no título da seção; para leitores de
 * tela, ele entra no nome acessível do cartão e de cada grupo de notas.
 *
 * Cada fator é um grupo de botões de opção (radio) com o mesmo `name`: Tab
 * entra no grupo e as setas do teclado mudam a nota, como em qualquer grupo
 * de rádio nativo. Mudar uma nota chama `alterarGut`, que mantém o
 * salvamento automático existente. "Desfazer" volta às notas salvas (ou a
 * nenhuma nota), já que um botão de opção não pode ser desmarcado.
 *
 * @param {string} chave chave do item ("p{id}" ou "f{id}")
 * @param {string} descricao texto do pró/contra (ou do fator antigo)
 * @param {"PRO"|"CONTRA"|null} tipo tipo do pró/contra; null para fator antigo
 * @param {object|null} fatorAntigo fator antigo, para o botão Excluir; null nos prós/contras
 * @param {string} [alternativa=""] rótulo da alternativa, como "Alternativa A — Nome"; vazio para fator antigo
 * @returns {HTMLElement} cartão pronto para a lista
 */
function linhaGut(chave, descricao, tipo, fatorAntigo, alternativa = "") {
    const linha = document.createElement("article"); linha.className = "gut-card"; linha.dataset.chave = chave;
    const sequencia = ++sequenciaCartao;
    const idDescricao = "gut-desc-" + sequencia, idTipo = "gut-tipo-" + sequencia, idAlternativa = "gut-alt-" + sequencia;
    const cabecalho = document.createElement("div"); cabecalho.className = "gut-card-cabecalho";
    if (alternativa) { const alt = criar("span", alternativa + ","); alt.className = "fluxo-sr-only"; alt.id = idAlternativa; cabecalho.append(alt); }
    const nomeTipo = tipo === "PRO" ? "Pró" : tipo === "CONTRA" ? "Contra" : "Fator antigo";
    const marca = criar("span", nomeTipo); marca.id = idTipo;
    marca.className = "gut-tipo" + (tipo === "PRO" ? " lista-pros" : tipo === "CONTRA" ? " lista-contras" : "");
    cabecalho.append(marca);
    linha.setAttribute("aria-labelledby", (alternativa ? idAlternativa + " " : "") + idTipo + " " + idDescricao);
    const texto = criar("p", descricao); texto.className = "gut-descricao"; texto.id = idDescricao;
    linha.append(cabecalho, texto);
    const fatores = document.createElement("div"); fatores.className = "gut-fatores";
    const notas = notasNaTela(chave);
    const contextoItem = nomeTipo + (alternativa ? " da " + alternativa : "") + ": " + descricao;
    for (const campo of CAMPOS_GUT) {
        const grupo = document.createElement("fieldset"); grupo.className = "gut-fator";
        const [minimo, maximo] = EXTREMOS_GUT[campo];
        const legenda = criar("legend", NOMES_GUT[campo] + ": " + PERGUNTAS_GUT[campo] + " " + minimo + "; " + maximo + ". " + contextoItem);
        legenda.className = "fluxo-sr-only";
        const corpo = document.createElement("div"); corpo.className = "gut-fator-corpo";
        const pergunta = document.createElement("div"); pergunta.className = "gut-fator-texto"; pergunta.setAttribute("aria-hidden", "true");
        pergunta.append(criar("strong", NOMES_GUT[campo]), criar("span", PERGUNTAS_GUT[campo]));
        const controle = document.createElement("div"); controle.className = "gut-fator-controle";
        const botoes = document.createElement("div"); botoes.className = "gut-botoes";
        for (let nota = 1; nota <= 5; nota++) {
            const rotulo = document.createElement("label"); rotulo.className = "gut-nota-item";
            const input = document.createElement("input"); input.type = "radio";
            input.name = "gut-" + chave + "-" + campo; input.value = String(nota); input.dataset.campo = campo;
            input.checked = notas[campo] === String(nota);
            input.addEventListener("change", () => alterarGut(chave, linha));
            rotulo.append(input, criar("span", String(nota)));
            botoes.append(rotulo);
        }
        botoes.addEventListener("keydown", navegarNotas);
        const escala = document.createElement("div"); escala.className = "gut-escala"; escala.setAttribute("aria-hidden", "true");
        escala.append(criar("span", minimo), criar("span", maximo));
        controle.append(botoes, escala);
        corpo.append(pergunta, controle);
        grupo.append(legenda, corpo); fatores.append(grupo);
    }
    linha.append(fatores);
    const rodape = document.createElement("div"); rodape.className = "gut-card-rodape";
    const calculo = document.createElement("p"); calculo.className = "gut-calculo";
    const rotuloConta = criar("span", "Pontuação: "); rotuloConta.className = "fluxo-sr-only";
    const conta = criar("span", ""); conta.className = "gut-conta";
    const valor = criar("strong", "—"); valor.className = "gut-pontos";
    calculo.append(rotuloConta, conta, valor);
    const situacao = criar("small", ""); situacao.className = "gut-situacao";
    const desfazer = criar("button", "Desfazer"); desfazer.type = "button"; desfazer.className = "gut-desfazer"; desfazer.hidden = true;
    desfazer.setAttribute("aria-label", "Desfazer as notas não salvas de: " + descricao);
    desfazer.addEventListener("click", () => {
        if (ocupado || filaPorItem.get(chave)) return;
        const salvas = notasSalvas(chave);
        linha.querySelectorAll("input[data-campo]").forEach(input => { input.checked = input.value === salvas[input.dataset.campo]; });
        alterarGut(chave, linha);
        (linha.querySelector("input:checked") || linha.querySelector("input"))?.focus();
    });
    rodape.append(calculo, situacao, desfazer);
    if (fatorAntigo) rodape.append(acoes(fatorAntigo, "gut"));
    linha.append(rodape);
    return linha;
}

/**
 * Trata o teclado dentro de um grupo de notas de 1 a 5, seguindo o padrão de
 * grupo de rádio: seta para a direita ou para baixo escolhe a próxima nota
 * (da 5 volta para a 1), seta para a esquerda ou para cima escolhe a anterior
 * (da 1 vai para a 5), Home escolhe 1, End escolhe 5 e Espaço marca a nota em
 * foco.
 *
 * O comportamento nativo do navegador é cancelado para que a nota mude uma
 * única vez por tecla. A nota escolhida recebe o foco e dispara o mesmo
 * evento "change" de um clique, então o salvamento automático continua
 * passando por `alterarGut`. Nada acontece com o grupo desativado.
 *
 * @param {KeyboardEvent} evento tecla pressionada com o foco em um dos botões de nota
 * @returns {void}
 */
function navegarNotas(evento) {
    const opcoes = [...evento.currentTarget.querySelectorAll("input[type=radio]")];
    const atual = opcoes.indexOf(evento.target);
    if (atual < 0 || evento.altKey || evento.ctrlKey || evento.metaKey) return;
    const destinos = { ArrowRight: atual + 1, ArrowDown: atual + 1, ArrowLeft: atual - 1, ArrowUp: atual - 1, Home: 0, End: opcoes.length - 1, " ": atual };
    if (!(evento.key in destinos)) return;
    evento.preventDefault();
    if (evento.target.disabled) return;
    const alvo = opcoes[(destinos[evento.key] + opcoes.length) % opcoes.length];
    alvo.focus();
    if (alvo.checked) return;
    alvo.checked = true;
    alvo.dispatchEvent(new Event("change", { bubbles: true }));
}

/**
 * Agrupa os cartões da GUT em uma lista responsiva.
 *
 * @param {string} legenda descrição da lista para leitores de tela
 * @param {HTMLElement[]} linhas cartões dos itens
 * @returns {HTMLDivElement} contêiner com os cartões
 */
function tabelaGut(legenda, linhas) {
    const contem = document.createElement("div"); contem.className = "gut-cartoes";
    contem.setAttribute("role", "group"); contem.setAttribute("aria-label", legenda);
    contem.append(...linhas);
    return contem;
}

/**
 * Desenha a Matriz GUT a partir dos prós e contras de cada alternativa.
 *
 * Cada pró/contra aparece uma única vez, no grupo da sua alternativa, com a
 * descrição atual. Notas ainda não salvas são preservadas entre desenhos;
 * rascunhos de itens que deixaram de existir (pró/contra excluído) são descartados.
 * Fatores antigos, digitados à mão antes da integração, ficam numa seção própria.
 *
 * @returns {void}
 */
function renderizarGut() {
    const antigos = dados.fatoresGut.filter(fator => fator.proContraId == null);
    const existentes = new Set([...dados.opcoes.flatMap(opcao => opcao.prosContras.map(item => "p" + item.id)), ...antigos.map(chaveFator)]);
    for (const chave of [...rascunhosGut.keys()]) {
        if (!existentes.has(chave) || notasIguais(rascunhosGut.get(chave), notasSalvas(chave))) rascunhosGut.delete(chave);
    }
    for (const chave of [...errosGut.keys()]) if (!existentes.has(chave)) errosGut.delete(chave);
    const grupos = [];
    const semItens = dados.opcoes.every(opcao => !opcao.prosContras.length);
    if (semItens) {
        const vazio = document.createElement("div"); vazio.className = "gut-vazio";
        vazio.append(criar("p", "Ainda não há prós e contras para classificar. Cadastre-os em Prós e Contras e eles aparecerão aqui automaticamente."));
        const ir = criar("button", "Ir para Prós e Contras"); ir.type = "button"; ir.className = "fluxo-salvar";
        ir.addEventListener("click", () => abrirAba("pros"));
        vazio.append(ir); grupos.push(vazio);
    }
    dados.opcoes.forEach((opcao, i) => {
        if (semItens) return;
        const letra = i === 0 ? "a" : "b";
        const grupo = document.createElement("section"); grupo.className = "gut-grupo"; grupo.dataset.letra = letra;
        const titulo = document.createElement("h3"); titulo.className = "argumentos-titulo"; titulo.id = "gut-titulo-" + letra;
        const marca = criar("span", letra.toUpperCase()); marca.className = "argumentos-letra"; marca.setAttribute("aria-hidden", "true");
        const nome = document.createElement("span"); nome.append("Alternativa " + letra.toUpperCase() + " — ");
        const nomeOpcao = criar("span", opcao.nome); nomeOpcao.dataset.nomeOpcao = ""; nome.append(nomeOpcao);
        titulo.append(marca, nome); grupo.setAttribute("aria-labelledby", titulo.id); grupo.append(titulo);
        const itens = [...opcao.prosContras.filter(item => item.tipo === "PRO"), ...opcao.prosContras.filter(item => item.tipo === "CONTRA")];
        if (itens.length) grupo.append(tabelaGut("Classificação GUT dos prós e contras da alternativa " + letra.toUpperCase(),
            itens.map(item => linhaGut("p" + item.id, item.descricao, item.tipo, null, "Alternativa " + letra.toUpperCase() + " — " + opcao.nome))));
        else grupo.append(criar("p", "Nenhum pró ou contra cadastrado nesta alternativa."));
        grupos.push(grupo);
    });
    if (antigos.length) {
        const grupo = document.createElement("section"); grupo.className = "gut-grupo gut-antigos"; grupo.setAttribute("aria-labelledby", "gut-titulo-antigos");
        const titulo = criar("h3", "Fatores cadastrados antes da integração"); titulo.id = "gut-titulo-antigos"; titulo.className = "argumentos-titulo";
        grupo.append(titulo, criar("p", "Estes fatores foram digitados diretamente na Matriz GUT, antes de ela usar seus prós e contras. Eles foram preservados: você pode ajustar as notas ou excluí-los."));
        grupo.append(tabelaGut("Fatores GUT cadastrados antes da integração", antigos.map(fator => linhaGut(chaveFator(fator), fator.descricao, null, fator))));
        grupos.push(grupo);
    }
    el("gut-grupos").replaceChildren(...grupos);
    document.querySelectorAll("#gut-grupos [data-chave]").forEach(linha => atualizarLinhaGut(linha.dataset.chave));
    atualizarProgressoGut();
    atualizarDestaques();
}

function renderizar() {
    el("decisao-titulo").textContent = dados.titulo;
    el("abrir-inversao").href = "inversao.html?decisaoId=" + encodeURIComponent(id);
    el("abrir-101010").href = "metodo-101010.html?decisaoId=" + encodeURIComponent(id);
    el("salvar-depois").hidden = dados.status !== "EM_ANDAMENTO";
    window.BussolaRevisao.sincronizar({ etapaAtual: dados.etapaAtual, status: dados.status, etapaPagina: 6, decisaoId: id });
    renderizarArgumentos();
    renderizarGut();
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
        fecharArgumento(false); rascunhosGut.clear(); errosGut.clear(); renderizar(); el("recarregar").hidden = true;
        el("mensagem").textContent = dados.status === "CONCLUIDA" ? "Decisão concluída: somente consulta." : "";
    } catch (erro) { bloqueado = true; el("analise").hidden = true; el("recarregar").hidden = false; el("mensagem").textContent = erro.message; }
    finally { estado(false); }
    if (dados && !bloqueado && new URLSearchParams(location.search).get("tecnica") === "gut") abrirAba("gut");
    if (dados && !bloqueado) atualizarTecnicasExternas();
}
/**
 * Envia uma alteração de Prós e Contras (ou a exclusão de um fator GUT antigo) e atualiza a tela.
 *
 * Espera antes os salvamentos automáticos da GUT, para não redesenhar a tela
 * no meio deles. O retorno aparece perto do formulário da técnica (em Prós e
 * Contras, dentro do card envolvido). Depois de adicionar um pró ou contra, o
 * formulário reabre vazio no mesmo card, pronto para o próximo item; depois de
 * editar, ele fecha. Em erro simples, o formulário e o texto digitado permanecem.
 * Falhas que deixam o estado salvo incerto bloqueiam novas edições e exibem o
 * aviso do topo, junto a "Recarregar dados", que recebe o foco.
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
    const proximo = painel === "pros" && metodo === "POST" && argumento ? { letra: argumento.letra, tipo: argumento.tipo } : null;
    estado(true); limparRetornos(); el("mensagem").textContent = ""; retorno(painel, "Salvando…");
    let confirmado = false;
    try {
        await filaGut;
        await api.requisitar(caminho, metodo, corpo); confirmado = true;
        if (painel === "pros") fecharArgumento(false);
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
    else if (confirmado && proximo) {
        abrirArgumento(proximo.letra, proximo.tipo, null, cartao(proximo.letra, proximo.tipo).querySelector(".argumentos-adicionar"));
        retorno("pros", sucesso + " Escreva o próximo ou clique em Cancelar.", "sucesso");
    }
    else if (confirmado) (painel === "gut" ? document.querySelector("#gut-grupos input") : card?.querySelector(".argumentos-adicionar"))?.focus();
}
el("argumento-descricao").addEventListener("input", () => el("argumento-descricao").setCustomValidity(""));
el("argumento-form").addEventListener("input", limparRetornos);
el("argumento-form").addEventListener("change", limparRetornos);
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
el("cancelar-argumento").addEventListener("click", () => { if (!ocupado) { fecharArgumento(); limparRetornos(); } });
/**
 * Explica por que a ação foi impedida e como liberá-la.
 *
 * Ao fechar o aviso, o foco volta ao campo do rascunho pendente (pró/contra
 * ou a primeira linha da GUT com notas não salvas), para a pessoa continuar
 * sem procurar o formulário.
 *
 * @param {string} destino Complemento da frase, por exemplo "trocar de técnica".
 * @returns {void}
 */
function avisarRascunho(destino) {
    if (sujoArgumento()) {
        aviso("Alterações não salvas", "Há um pró ou contra em edição que ainda não foi salvo. Clique em “Adicionar” ou “Salvar alterações” para gravá-lo, ou em “Cancelar” para descartar a edição antes de " + destino + ".");
    } else {
        aviso("Classificação GUT não salva", "Há itens da Matriz GUT com notas incompletas ou que não foram salvas. Escolha Gravidade, Urgência e Tendência (ou volte as notas ao valor anterior) antes de " + destino + ".");
    }
    el("aviso").addEventListener("close", () => {
        if (sujoArgumento()) { el("argumento-descricao").focus(); return; }
        const chave = [...rascunhosGut.keys()].find(item => !filaPorItem.get(item));
        document.querySelector(`#gut-grupos [data-chave="${chave}"] input`)?.focus();
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
    if (tipo !== "gut" && salvamentosGut) { filaGut.then(() => abrirAba(tipo)); return; }
    if (tipo === "gut" ? sujoArgumento() : pendente()) { avisarRascunho("trocar de técnica"); return; }
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
        limparRetornos();
        const incompletas = [...rascunhosGut].filter(([, notas]) => !notasCompletas(notas)).length;
        [...rascunhosGut].filter(([chave, notas]) => notasCompletas(notas) && !filaPorItem.get(chave)).forEach(([chave]) => {
            errosGut.delete(chave); enfileirarGut(chave);
        });
        if (incompletas) {
            retorno("gut", "Há itens com notas incompletas. Escolha Gravidade, Urgência e Tendência para salvá-los.", "erro");
            return;
        }
        if (salvamentosGut) retorno("gut", "Salvando suas classificações…");
        filaGut.then(() => {
            if (!sujoGut()) retorno("gut", "Suas classificações estão salvas. Você pode continuar depois pela Análise da sua decisão.", "sucesso");
        });
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
    if (salvamentosGut) retorno("gut", "Aguardando o salvamento das classificações…");
    window.BussolaAlteracoes.permitir(el("proxima-tecnica"), () => location.assign("metodo-101010.html?decisaoId=" + encodeURIComponent(id)));
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
