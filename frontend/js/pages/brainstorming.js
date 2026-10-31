(() => {
    "use strict";
    const api = window.BussolaDecisaoApi;
    const el = id => document.getElementById(id);
    const formulario = el("ideia-form");
    const conteudo = el("conteudo-ideia");
    const dialogo = el("confirmar-exclusao");
    /* `codigo` é o valor gravado no backend e não muda; `nome` (coluna) e `singular` (formulário) são só rótulos. */
    const categorias = [
        { codigo: "FAVORAVEL", nome: "Pontos positivos", singular: "Ponto positivo", classe: "favoravel",
            dica: "O que você ganha ou gosta nesta decisão.", exemplo: "Ex.: Posso aprender na prática." },
        { codigo: "DESFAVORAVEL", nome: "Pontos negativos", singular: "Ponto negativo", classe: "desfavoravel",
            dica: "Perdas, custos ou receios.", exemplo: "Ex.: Pode custar mais do que planejei." },
        { codigo: "DUVIDA", nome: "Dúvidas", singular: "Dúvida", classe: "duvida",
            dica: "O que você ainda precisa descobrir.", exemplo: "Ex.: Será que consigo me adaptar?" }
    ];
    const colunas = {};
    let id;
    let decisao;
    let ideias = [];
    /** Ideia em edição: `{ id, conteudo, categoria, original: { conteudo, categoria } }`, ou null. */
    let editando = null;
    let excluindo = null;
    /** Retorno da última ação, mostrado dentro da coluna: `{ codigo, texto, erro }`, ou null. */
    let retorno = null;
    /** Id da ideia recém-adicionada ou salva, destacada discretamente até a próxima ação. */
    let destaque = null;
    let ocupado = true;
    let bloqueado = false;

    /**
     * Cria um elemento com classe e texto opcionais.
     *
     * @param {string} tag nome da tag HTML
     * @param {string} [classe] classes CSS separadas por espaço
     * @param {string} [texto] texto inserido com textContent (nunca como HTML)
     * @returns {HTMLElement} o elemento criado, ainda fora do documento
     */
    function criar(tag, classe, texto) {
        const elemento = document.createElement(tag);
        if (classe) elemento.className = classe;
        if (texto !== undefined) elemento.textContent = texto;
        return elemento;
    }

    /**
     * Procura a categoria pelo código gravado no backend.
     *
     * @param {(string|null)} codigo FAVORAVEL, DESFAVORAVEL, DUVIDA ou null
     * @returns {(object|undefined)} a categoria correspondente, ou undefined para
     *          ideias antigas sem categoria
     */
    const categoriaDe = codigo => categorias.find(categoria => categoria.codigo === codigo);

    /**
     * Monta, uma única vez, as três colunas de visualização das ideias.
     *
     * Ordem de cada coluna: título com contagem, descrição, retorno da última
     * ação (região anunciada por leitores de tela), exemplo quando vazia e a
     * lista de ideias. Guarda as referências em `colunas`, indexadas pelo código
     * da categoria (FAVORAVEL, DESFAVORAVEL, DUVIDA).
     *
     * @returns {void}
     */
    function montarColunas() {
        const destino = el("ideias");
        categorias.forEach(categoria => {
            const secao = criar("section", `brain-coluna ${categoria.classe}`);
            const tituloId = `titulo-${categoria.classe}`;
            secao.setAttribute("aria-labelledby", tituloId);
            const titulo = criar("h2", "", categoria.nome);
            titulo.id = tituloId;
            const contagem = criar("span", "coluna-contagem");
            titulo.append(" ", contagem);
            const aviso = criar("p", "coluna-retorno");
            aviso.setAttribute("role", "status");
            aviso.setAttribute("aria-live", "polite");
            const vazia = criar("p", "coluna-vazia", categoria.exemplo);
            const lista = criar("ul", "coluna-lista");
            secao.append(titulo, criar("p", "coluna-dica", categoria.dica), aviso, vazia, lista);
            destino.append(secao);
            colunas[categoria.codigo] = { secao, contagem, aviso, vazia, lista };
        });
    }

    /**
     * Envia o formulário com Enter; Shift + Enter continua quebrando a linha.
     *
     * Ignora o Enter usado para confirmar acentos ou sugestões do teclado
     * (composição de texto). O envio passa pelas mesmas validações do botão,
     * então uma ideia sem texto ou sem categoria nunca é enviada.
     *
     * @param {KeyboardEvent} evento tecla pressionada no campo
     * @param {HTMLFormElement} alvo formulário a enviar
     * @returns {void}
     */
    function enviarComEnter(evento, alvo) {
        if (evento.key !== "Enter" || evento.shiftKey || evento.isComposing) return;
        evento.preventDefault();
        alvo.requestSubmit();
    }

    /**
     * Mostra ou esconde uma mensagem de validação junto ao campo correspondente.
     *
     * @param {string} idErro id do parágrafo de erro (`erro-conteudo` ou `erro-categoria`)
     * @param {string} texto mensagem a exibir; texto vazio esconde o parágrafo
     * @returns {void}
     */
    function mostrarErro(idErro, texto) {
        const erro = el(idErro);
        erro.textContent = texto;
        erro.hidden = !texto;
        if (texto) erro.setAttribute("role", "alert");
        else erro.removeAttribute("role");
        if (idErro === "erro-conteudo") conteudo.setAttribute("aria-invalid", String(Boolean(texto)));
        if (idErro === "erro-categoria") formulario.querySelector(".categorias").classList.toggle("invalida", Boolean(texto));
    }

    /**
     * Mostra o estado do envio junto ao botão + Adicionar.
     *
     * O mesmo lugar recebe o aviso "Alterações ainda não salvas." (via
     * `BussolaAlteracoes.refletir`), para que ele apareça sem deslocar o
     * formulário nem as colunas enquanto a pessoa digita.
     *
     * @param {string} texto "Salvando…", mensagem de erro ou vazio para limpar
     * @param {boolean} [erro=false] true para o visual de erro
     * @returns {void}
     */
    function estadoFormulario(texto, erro = false) {
        const estado = el("estado-form");
        estado.textContent = texto;
        estado.classList.toggle("erro", erro);
    }

    const categoriaEscolhida = () => formulario.elements.categoria.value;
    const temRascunho = () => conteudo.value.trim() !== "";

    /**
     * Indica se a ideia em edição foi alterada em relação ao que está salvo.
     *
     * @returns {boolean} true quando o texto ou a categoria da edição aberta mudou
     */
    function edicaoAlterada() {
        return editando !== null && (editando.conteudo !== editando.original.conteudo
            || editando.categoria !== editando.original.categoria);
    }

    /**
     * Indica alterações que seriam perdidas ao sair da tela: texto ainda não
     * adicionado, edição alterada ou exclusão aberta e não confirmada. A
     * categoria escolhida sem texto não conta: não há o que perder.
     *
     * @returns {boolean} true quando sair perderia algo
     */
    function sujo() {
        return excluindo !== null || temRascunho() || edicaoAlterada();
    }

    window.BussolaAlteracoes.registrar({ sujo, ocupado: () => ocupado });

    const emRevisao = () => window.BussolaRevisao.emRevisao(decisao?.etapaAtual, 2) && decisao?.status === "EM_ANDAMENTO";
    const editavel = () => decisao?.status === "EM_ANDAMENTO" && (decisao.etapaAtual === 2 || emRevisao());

    /**
     * Habilita ou bloqueia campos e botões conforme o estado da tela.
     *
     * Durante um salvamento o campo de texto fica só leitura (e não
     * desabilitado), para que o foco continue onde a pessoa estava escrevendo.
     *
     * @returns {void}
     */
    function atualizarControles() {
        const desabilitado = ocupado || bloqueado || !editavel();
        el("campos").disabled = bloqueado || !editavel();
        conteudo.readOnly = ocupado;
        el("salvar-ideia").disabled = desabilitado;
        const semIdeias = ideias.length === 0 && !emRevisao();
        const pendente = editando !== null || temRascunho();
        el("concluir").disabled = desabilitado || semIdeias || pendente;
        el("concluir").title = semIdeias ? "Adicione pelo menos uma ideia para avançar."
            : pendente ? "Adicione, salve ou apague o texto ainda não salvo antes de avançar." : "";
        document.querySelectorAll(".coluna-lista button, .coluna-lista select, .coluna-lista textarea").forEach(controle => {
            controle.disabled = desabilitado;
        });
        el("excluir").disabled = desabilitado;
        el("recarregar").disabled = ocupado;
    }

    /**
     * Registra o retorno de uma ação para exibi-lo dentro da coluna afetada.
     *
     * @param {(string|null)} codigo categoria da coluna; para ideias sem categoria
     *        a mensagem vai para o status geral da página
     * @param {string} texto mensagem curta
     * @param {boolean} [erro=false] true para o visual de erro
     * @returns {void}
     */
    function avisar(codigo, texto, erro = false) {
        if (!colunas[codigo]) {
            retorno = null;
            el("mensagem").textContent = texto;
            return;
        }
        retorno = { codigo, texto, erro };
    }

    /**
     * Cria o cartão de uma ideia salva, com Editar e Excluir quando a etapa
     * pode ser alterada.
     *
     * @param {{id: number, conteudo: string, categoria: (string|null)}} ideia ideia vinda da API
     * @returns {HTMLLIElement} item pronto para entrar na lista da coluna
     */
    function itemIdeia(ideia) {
        const item = criar("li", "ideia" + (ideia.id === destaque ? " recente" : ""));
        item.dataset.id = ideia.id;
        item.append(criar("p", "", ideia.conteudo));
        if (!editavel()) return item;
        const acoes = criar("div", "ideia-acoes");
        const editar = criar("button", "ideia-acao ideia-editar", "Editar");
        editar.type = "button";
        editar.setAttribute("aria-label", `Editar ideia: ${ideia.conteudo}`);
        editar.addEventListener("click", () => iniciarEdicao(ideia));
        const excluir = criar("button", "ideia-acao ideia-excluir", "Excluir");
        excluir.type = "button";
        excluir.setAttribute("aria-label", `Excluir ideia: ${ideia.conteudo}`);
        excluir.addEventListener("click", () => {
            if (ocupado || bloqueado) return;
            excluindo = ideia.id;
            el("ideia-excluir").textContent = ideia.conteudo;
            window.BussolaAlteracoes.refletir(el("estado-form"), true);
            dialogo.showModal();
        });
        acoes.append(editar, excluir);
        item.append(acoes);
        return item;
    }

    /**
     * Cria o editor da ideia aberta para edição: texto, categoria e os botões
     * Salvar e Cancelar. O rascunho fica em `editando`, então o editor pode ser
     * redesenhado sem perder o que já foi digitado.
     *
     * @returns {HTMLLIElement} item em modo de edição
     */
    function itemEdicao() {
        const item = criar("li", "ideia editando");
        item.dataset.id = editando.id;
        const form = criar("form", "ideia-edicao");
        const rotuloTexto = criar("label", "ajuda-sr", "Texto da ideia");
        rotuloTexto.htmlFor = "editar-conteudo";
        const texto = criar("textarea");
        texto.id = "editar-conteudo";
        texto.rows = 3;
        texto.maxLength = 16000;
        texto.value = editando.conteudo;
        const rotuloCategoria = criar("label", "ideia-edicao-rotulo", "Categoria");
        rotuloCategoria.htmlFor = "editar-categoria";
        const seletor = criar("select");
        seletor.id = "editar-categoria";
        if (!categoriaDe(editando.categoria)) {
            const vazio = criar("option", "", "Escolha uma categoria");
            vazio.value = "";
            seletor.append(vazio);
        }
        categorias.forEach(categoria => {
            const opcao = criar("option", "", categoria.singular);
            opcao.value = categoria.codigo;
            seletor.append(opcao);
        });
        seletor.value = editando.categoria || "";
        const acoes = criar("div", "ideia-edicao-acoes");
        const salvar = criar("button", "home-primary", "Salvar");
        salvar.type = "submit";
        const cancelar = criar("button", "home-secondary", "Cancelar");
        cancelar.type = "button";
        acoes.append(salvar, cancelar);
        form.append(rotuloTexto, texto, rotuloCategoria, seletor, acoes);
        item.append(form);

        texto.addEventListener("input", () => {
            editando.conteudo = texto.value;
            texto.setCustomValidity("");
            window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
        });
        texto.addEventListener("keydown", evento => {
            if (evento.key === "Escape") { evento.preventDefault(); cancelarEdicao(); return; }
            enviarComEnter(evento, form);
        });
        seletor.addEventListener("change", () => {
            editando.categoria = seletor.value || null;
            seletor.setCustomValidity("");
            window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
        });
        cancelar.addEventListener("click", cancelarEdicao);
        form.addEventListener("submit", evento => {
            evento.preventDefault();
            texto.setCustomValidity(texto.value.trim() ? "" : "Escreva a ideia antes de salvar.");
            seletor.setCustomValidity(seletor.value ? "" : "Escolha a categoria da ideia.");
            if (!form.reportValidity()) return;
            salvarEdicao();
        });
        return item;
    }

    /**
     * Abre uma ideia para edição no próprio cartão.
     *
     * Se outra ideia estava em edição com mudanças, pede confirmação antes de
     * descartá-las. O texto do formulário de nova ideia não é afetado.
     *
     * @param {{id: number, conteudo: string, categoria: (string|null)}} ideia ideia a editar
     * @returns {void}
     */
    function iniciarEdicao(ideia) {
        if (ocupado || bloqueado) return;
        if (edicaoAlterada() && !window.confirm("Descartar as mudanças ainda não salvas na outra ideia?")) return;
        editando = { id: ideia.id, conteudo: ideia.conteudo, categoria: ideia.categoria,
            original: { conteudo: ideia.conteudo, categoria: ideia.categoria } };
        renderizar();
        window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
        el("editar-conteudo")?.focus();
    }

    /**
     * Fecha a edição aberta, confirmando antes se houver mudanças não salvas,
     * e devolve o foco ao botão Editar da mesma ideia.
     *
     * @returns {void}
     */
    function cancelarEdicao() {
        if (editando === null) return;
        if (edicaoAlterada() && !window.confirm("Descartar as mudanças desta ideia?")) return;
        const ideiaId = editando.id;
        editando = null;
        renderizar();
        window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
        focarEditar(ideiaId);
    }

    /**
     * Coloca o foco no botão Editar de uma ideia, se ela ainda estiver na tela.
     *
     * @param {number} ideiaId id da ideia
     * @returns {void}
     */
    function focarEditar(ideiaId) {
        document.querySelector(`.ideia[data-id="${ideiaId}"] .ideia-editar`)?.focus();
    }

    /**
     * Redesenha as listas das colunas, as contagens, o retorno da última ação e
     * o estado da etapa.
     *
     * Colunas vazias continuam visíveis: em edição mostram um exemplo discreto;
     * em consulta, avisam que não há ideias naquela categoria. Ideias antigas sem
     * categoria aparecem num bloco separado, abaixo das colunas.
     *
     * @returns {void}
     */
    function renderizar() {
        const podeEditar = editavel();
        const desenhar = ideia => editando?.id === ideia.id ? itemEdicao() : itemIdeia(ideia);
        categorias.forEach(categoria => {
            const coluna = colunas[categoria.codigo];
            const itens = ideias.filter(ideia => ideia.categoria === categoria.codigo);
            coluna.lista.replaceChildren(...itens.map(desenhar));
            coluna.contagem.textContent = `(${itens.length})`;
            coluna.vazia.hidden = itens.length > 0;
            coluna.vazia.textContent = podeEditar ? categoria.exemplo : "Nenhuma ideia nesta categoria.";
            const texto = retorno?.codigo === categoria.codigo ? retorno.texto : "";
            if (coluna.aviso.textContent !== texto) coluna.aviso.textContent = texto;
            coluna.aviso.classList.toggle("erro", Boolean(texto && retorno.erro));
        });
        const semCategoria = ideias.filter(ideia => !categoriaDe(ideia.categoria));
        el("lista-sem-categoria").replaceChildren(...semCategoria.map(desenhar));
        el("sem-categoria").hidden = semCategoria.length === 0;

        const somenteConsulta = !podeEditar;
        if (podeEditar && ideias.length === 0 && !emRevisao()) {
            el("mensagem").textContent = "Adicione pelo menos uma ideia para liberar a próxima etapa.";
        }
        formulario.hidden = somenteConsulta;
        el("concluir").hidden = somenteConsulta;
        el("proxima-etapa").hidden = !somenteConsulta;
        window.BussolaRevisao.sincronizar({
            etapaAtual: decisao.etapaAtual, status: decisao.status, etapaPagina: 2, decisaoId: id
        });
        el("continuar-decisao").href = paginaDaRetomada(decisao) + "?decisaoId=" + encodeURIComponent(id);
        if (somenteConsulta && (decisao.status === "CONCLUIDA" || decisao.etapaAtual > 3)) {
            el("proxima-descricao").textContent = "Essas são as ideias salvas da sua decisão. A consulta não altera seu progresso.";
        }
        atualizarControles();
    }

    /**
     * Escolhe a tela aberta por "Continuar decisão" no cartão "Brainstorming concluído".
     *
     * Leva direto à etapa salva no backend, sem passar pela Contextualização
     * (que só redirecionaria e deixaria uma entrada extra no histórico do navegador).
     *
     * @param {{etapaAtual: number, status: string}} dados decisão carregada da API
     * @returns {string} página relativa a frontend/pages/: resultado.html para decisões
     *          concluídas; analise.html, pesos.html, criterios.html ou opcoes.html conforme
     *          a etapa salva (6, 5, 4 ou 3); brainstorming.html se ainda estiver na etapa 2
     */
    function paginaDaRetomada(dados) {
        if (dados.status === "CONCLUIDA") return "resultado.html";
        if (dados.etapaAtual >= 6) return "analise.html";
        if (dados.etapaAtual === 5) return "pesos.html";
        if (dados.etapaAtual === 4) return "criterios.html";
        if (dados.etapaAtual === 3) return "opcoes.html";
        return "brainstorming.html";
    }

    /**
     * Executa uma gravação na API e mostra o andamento onde a pessoa está agindo.
     *
     * Nada de sucesso é exibido aqui: a própria `acao`, depois que a API
     * confirma, troca o "Salvando…" pela mensagem final. Em erro, os dados
     * digitados continuam onde estavam.
     *
     * @param {function(): Promise<void>} acao chamada à API e atualização de `ideias`
     * @param {function(string, boolean): void} mostrar exibe o andamento ("Salvando…")
     *        ou o erro (segundo argumento true) no lugar certo da tela
     * @returns {Promise<boolean>} true se a gravação deu certo; false se foi
     *          ignorada (tela ocupada, bloqueada ou só leitura) ou falhou
     */
    async function executar(acao, mostrar) {
        if (ocupado || bloqueado || !editavel()) return false;
        ocupado = true;
        retorno = null;
        destaque = null;
        if (el("mensagem").textContent === "Adicione pelo menos uma ideia para liberar a próxima etapa.") el("mensagem").textContent = "";
        mostrar("Salvando…", false);
        atualizarControles();
        try {
            await acao();
            renderizar();
            return true;
        } catch (erro) {
            bloqueado = Boolean(erro.status === 404 || api.encerrada);
            mostrar(erro.message || "Não foi possível salvar. Tente novamente.", true);
            if (bloqueado) {
                el("mensagem").textContent = erro.message;
                el("recarregar").hidden = false;
            }
            renderizar();
            return false;
        } finally {
            ocupado = false;
            atualizarControles();
        }
    }

    /**
     * Valida e grava a ideia do formulário único.
     *
     * Sem categoria, avisa junto ao botão; sem texto, avisa junto ao campo. O
     * texto digitado nunca é apagado por uma validação ou por um erro da API.
     * Depois de salvar, limpa só o texto (a categoria escolhida continua
     * marcada para a próxima ideia) e anuncia o resultado na coluna de destino.
     *
     * @returns {Promise<void>}
     */
    async function adicionar() {
        const texto = conteudo.value.trim();
        const codigo = categoriaEscolhida();
        mostrarErro("erro-conteudo", texto ? "" : "Escreva sua ideia antes de adicionar.");
        mostrarErro("erro-categoria", codigo ? "" : "Escolha uma categoria para adicionar.");
        if (!codigo) { formulario.querySelector("input[name='categoria']").focus(); return; }
        if (!texto) { conteudo.focus(); return; }
        estadoFormulario("");
        let nova;
        const salvou = await executar(async () => {
            nova = await api.requisitar(`/decisoes/${id}/ideias`, "POST", { conteudo: texto, categoria: codigo });
            ideias.push(nova);
            conteudo.value = "";
            estadoFormulario("");
            avisar(codigo, `Adicionada em ${categoriaDe(codigo).nome}.`);
            destaque = nova.id;
        }, estadoFormulario);
        if (!salvou) return;
        window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
        atualizarControles();
        conteudo.focus();
    }

    /**
     * Grava a ideia em edição (texto e categoria). Se a categoria mudar, o
     * cartão passa para a coluna correspondente e o retorno aparece lá.
     *
     * @returns {Promise<void>}
     */
    async function salvarEdicao() {
        const ideiaId = editando.id;
        const anterior = editando.original.categoria;
        const dados = { conteudo: editando.conteudo.trim(), categoria: editando.categoria };
        const salvou = await executar(async () => {
            const ideia = await api.requisitar(`/decisoes/${id}/ideias/${ideiaId}`, "PUT", dados);
            ideias = ideias.map(item => item.id === ideiaId ? ideia : item);
            editando = null;
            const nome = categoriaDe(ideia.categoria)?.nome;
            avisar(ideia.categoria, ideia.categoria !== anterior ? `Movida para ${nome}.` : "Alteração salva.");
            destaque = ideia.id;
        }, (texto, erro) => avisar(anterior, texto, erro));
        if (salvou) {
            window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
            focarEditar(ideiaId);
        }
    }

    formulario.addEventListener("submit", evento => {
        evento.preventDefault();
        adicionar();
    });
    conteudo.addEventListener("keydown", evento => enviarComEnter(evento, formulario));
    conteudo.addEventListener("input", () => {
        if (conteudo.value.trim()) mostrarErro("erro-conteudo", "");
        if (el("estado-form").classList.contains("erro")) estadoFormulario("");
        atualizarControles();
        window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
    });
    formulario.addEventListener("change", evento => {
        if (evento.target.name === "categoria") mostrarErro("erro-categoria", "");
    });
    el("cancelar-exclusao").addEventListener("click", () => {
        excluindo = null;
        dialogo.close();
        window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
    });
    dialogo.addEventListener("close", () => {
        if (excluindo === null) return;
        excluindo = null;
        window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
    });
    el("excluir").addEventListener("click", async () => {
        const ideiaId = excluindo;
        const codigo = ideias.find(ideia => ideia.id === ideiaId)?.categoria ?? null;
        excluindo = null;
        dialogo.close();
        const excluiu = await executar(async () => {
            await api.requisitar(`/decisoes/${id}/ideias/${ideiaId}`, "DELETE");
            ideias = ideias.filter(ideia => ideia.id !== ideiaId);
            if (editando?.id === ideiaId) editando = null;
            avisar(codigo, "Ideia excluída.");
        }, (texto, erro) => avisar(codigo, texto, erro));
        if (excluiu) {
            window.BussolaAlteracoes.refletir(el("estado-form"), sujo());
            conteudo.focus();
        }
    });
    el("concluir").addEventListener("click", async () => {
        if (ocupado || bloqueado || !editavel() || window.BussolaEtapas.aberta || editando !== null || temRascunho()) return;
        if (!emRevisao() && ideias.length === 0) {
            el("mensagem").textContent = "Adicione pelo menos uma ideia antes de avançar.";
            conteudo.focus();
            return;
        }
        if (emRevisao()) {
            window.BussolaAlteracoes.navegar("opcoes.html?decisaoId=" + encodeURIComponent(id)
                + (decisao.etapaAtual > 3 ? "&revisao=1" : ""));
            return;
        }
        if (!await window.BussolaEtapas.confirmarAvanco()) return;
        executar(async () => {
            decisao = await api.requisitar(`/decisoes/${id}/brainstorming/concluir`, "POST");
            window.BussolaAlteracoes.navegar("opcoes.html?decisaoId=" + encodeURIComponent(id));
        }, texto => { el("mensagem").textContent = texto; });
    });

    async function carregar() {
        ocupado = true;
        atualizarControles();
        try {
            const usuario = await api.requisitar("/auth/me");
            window.BussolaCabecalho.preencherUsuario(usuario);
            id = api.idDaUrl();
            if (!id) throw new Error("Selecione uma decisão na Home para começar.");
            decisao = await api.requisitar(`/decisoes/${id}`);
            if (decisao.etapaAtual < 2) {
                bloqueado = true;
                window.location.replace(`nova-decisao.html?decisaoId=${encodeURIComponent(id)}`);
                return;
            }
            ideias = await api.requisitar(`/decisoes/${id}/ideias`);
            el("decisao-titulo").textContent = decisao.titulo;
            el("decisao-contexto").textContent = decisao.contexto || "";
            el("contexto-painel").hidden = !decisao.contexto;
            bloqueado = false;
            editando = null;
            retorno = null;
            destaque = null;
            formulario.reset();
            mostrarErro("erro-conteudo", "");
            mostrarErro("erro-categoria", "");
            estadoFormulario("");
            renderizar();
            el("mensagem").textContent = "";
            el("recarregar").hidden = true;
        } catch (erro) {
            bloqueado = true;
            el("mensagem").textContent = erro.message;
            el("recarregar").hidden = false;
        } finally {
            ocupado = false;
            atualizarControles();
        }
    }
    montarColunas();
    el("recarregar").addEventListener("click", carregar);
    window.addEventListener("pageshow", evento => { if (evento.persisted) window.location.reload(); });
    carregar();
})();
