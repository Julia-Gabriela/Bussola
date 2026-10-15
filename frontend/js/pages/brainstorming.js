(() => {
    "use strict";
    const api = window.BussolaDecisaoApi;
    const el = id => document.getElementById(id);
    const formulario = el("ideia-form");
    const conteudo = el("conteudo-ideia");
    const dialogo = el("confirmar-exclusao");
    const categorias = [
        { codigo: "FAVORAVEL", nome: "Favorável", classe: "favoravel" },
        { codigo: "DESFAVORAVEL", nome: "Desfavorável", classe: "desfavoravel" },
        { codigo: "DUVIDA", nome: "Dúvida", classe: "duvida" },
        { codigo: null, nome: "Sem categoria", classe: "" }
    ];
    let id;
    let decisao;
    let ideias = [];
    let editando = null;
    let excluindo = null;
    let ocupado = true;
    let bloqueado = false;
    let referenciaForm = null;

    /**
     * Fotografa o rascunho da ideia, inclusive a edição ainda aberta.
     *
     * @returns {string} texto, categoria e ideia em edição
     */
    function fotoFormulario() {
        return JSON.stringify({
            editando,
            conteudo: conteudo.value,
            categoria: formulario.elements.categoria.value
        });
    }

    /**
     * Indica rascunho diferente do último estado estável ou exclusão ainda não confirmada.
     * Ideias já gravadas pelo botão Adicionar, Salvar ou Excluir não entram aqui.
     *
     * @returns {boolean} true quando sair perderia o que está no formulário ou na exclusão aberta
     */
    function sujo() {
        return excluindo !== null || (referenciaForm !== null && fotoFormulario() !== referenciaForm);
    }

    /**
     * Considera o formulário atual como já compatível com o que está salvo.
     *
     * @returns {void}
     */
    function gravarReferencia() {
        referenciaForm = fotoFormulario();
        window.BussolaAlteracoes.refletir(el("mensagem"), false);
    }

    window.BussolaAlteracoes.registrar({ sujo, ocupado: () => ocupado });

    const emRevisao = () => window.BussolaRevisao.emRevisao(decisao?.etapaAtual, 2) && decisao?.status === "EM_ANDAMENTO";
    const editavel = () => decisao?.status === "EM_ANDAMENTO" && (decisao.etapaAtual === 2 || emRevisao());

    function atualizarControles() {
        const desabilitado = ocupado || bloqueado || !editavel();
        el("campos").disabled = desabilitado;
        el("concluir").disabled = desabilitado || editando !== null || conteudo.value.trim() !== "";
        el("concluir").title = editando !== null || conteudo.value.trim() !== "" ? "Salve ou cancele sua ideia antes de avançar." : "";
        document.querySelectorAll("#ideias button").forEach(botao => { botao.disabled = desabilitado; });
        el("excluir").disabled = desabilitado;
        el("recarregar").disabled = ocupado;
    }

    function limparFormulario() {
        editando = null;
        formulario.reset();
        conteudo.setCustomValidity("");
        el("form-titulo").textContent = "Adicionar ideia";
        el("salvar-ideia").textContent = "+ Adicionar";
        el("cancelar-edicao").hidden = true;
        atualizarControles();
        gravarReferencia();
    }

    function renderizar() {
        const destino = el("ideias");
        destino.replaceChildren();
        if (!ideias.length) {
            const vazio = document.createElement("p");
            vazio.textContent = "Nenhuma ideia registrada. Escreva o que vem à mente sobre essa decisão.";
            destino.append(vazio);
        }
        categorias.forEach(categoria => {
            const itens = ideias.filter(ideia => categoria.codigo === null
                ? !categorias.some(c => c.codigo !== null && c.codigo === ideia.categoria)
                : ideia.categoria === categoria.codigo);
            if (!itens.length) return;
            const grupo = document.createElement("section");
            grupo.className = `grupo-ideias ${categoria.classe}`;
            const titulo = document.createElement("h2");
            titulo.textContent = `${categoria.nome} (${itens.length})`;
            const lista = document.createElement("ul");
            itens.forEach(ideia => {
                const item = document.createElement("li");
                item.className = "ideia";
                const texto = document.createElement("p");
                texto.textContent = ideia.conteudo;
                item.append(texto);
                if (editavel()) {
                    const editar = document.createElement("button");
                    editar.type = "button";
                    editar.textContent = "Editar";
                    editar.setAttribute("aria-label", `Editar ideia: ${ideia.conteudo}`);
                    editar.addEventListener("click", () => {
                        if (ocupado || bloqueado) return;
                        if ((editando !== null || conteudo.value.trim()) && !window.confirm("Descartar o texto ainda não salvo para editar esta ideia?")) return;
                        editando = ideia.id;
                        conteudo.value = ideia.conteudo;
                        formulario.elements.categoria.value = categorias.some(c => c.codigo === ideia.categoria && c.codigo !== null) ? ideia.categoria : "FAVORAVEL";
                        el("form-titulo").textContent = "Editar ideia e categoria";
                        el("salvar-ideia").textContent = "Salvar alteração";
                        el("cancelar-edicao").hidden = false;
                        conteudo.setCustomValidity("");
                        atualizarControles();
                        gravarReferencia();
                        conteudo.focus();
                    });
                    const excluir = document.createElement("button");
                    excluir.type = "button";
                    excluir.textContent = "Excluir";
                    excluir.setAttribute("aria-label", `Excluir ideia: ${ideia.conteudo}`);
                    excluir.addEventListener("click", () => {
                        if (ocupado || bloqueado) return;
                        excluindo = ideia.id;
                        el("ideia-excluir").textContent = ideia.conteudo;
                        window.BussolaAlteracoes.refletir(el("mensagem"), true);
                        dialogo.showModal();
                    });
                    item.append(editar, excluir);
                }
                lista.append(item);
            });
            grupo.append(titulo, lista);
            destino.append(grupo);
        });
        const somenteConsulta = !editavel();
        formulario.hidden = somenteConsulta;
        el("concluir").hidden = somenteConsulta;
        el("proxima-etapa").hidden = !somenteConsulta;
        window.BussolaRevisao.sincronizar({
            etapaAtual: decisao.etapaAtual, status: decisao.status, etapaPagina: 2, decisaoId: id
        });
        el("continuar-decisao").href = "nova-decisao.html?decisaoId=" + encodeURIComponent(id);
        if (somenteConsulta && (decisao.status === "CONCLUIDA" || decisao.etapaAtual > 3)) {
            el("proxima-descricao").textContent = "Essas são as ideias salvas da sua decisão. A consulta não altera seu progresso.";
        }
        el("etapa").textContent = "Etapa 1 de 5 · Brainstorming";
        atualizarControles();
    }

    async function executar(acao, sucesso) {
        if (ocupado || bloqueado || !editavel()) return;
        ocupado = true;
        el("mensagem").textContent = "Salvando…";
        atualizarControles();
        try {
            await acao();
            el("mensagem").textContent = sucesso;
            renderizar();
        } catch (erro) {
            bloqueado = Boolean(erro.status === 404 || api.encerrada);
            el("mensagem").textContent = erro.message;
            el("recarregar").hidden = false;
        } finally {
            ocupado = false;
            atualizarControles();
        }
    }

    formulario.addEventListener("submit", evento => {
        evento.preventDefault();
        conteudo.setCustomValidity(conteudo.value.trim() ? "" : "Escreva uma ideia antes de salvar.");
        if (!formulario.reportValidity()) return;
        const dados = { conteudo: conteudo.value.trim(), categoria: formulario.elements.categoria.value };
        executar(async () => {
            const ideia = await api.requisitar(`/decisoes/${id}/ideias${editando === null ? "" : `/${editando}`}`, editando === null ? "POST" : "PUT", dados);
            if (editando === null) ideias.push(ideia);
            else ideias = ideias.map(item => item.id === editando ? ideia : item);
            limparFormulario();
        }, "Ideia salva.");
    });
    conteudo.addEventListener("input", () => {
        conteudo.setCustomValidity("");
        el("cancelar-edicao").hidden = editando === null && conteudo.value === "";
        atualizarControles();
        window.BussolaAlteracoes.refletir(el("mensagem"), sujo());
    });
    formulario.addEventListener("change", () => {
        window.BussolaAlteracoes.refletir(el("mensagem"), sujo());
    });
    el("cancelar-edicao").addEventListener("click", limparFormulario);
    el("cancelar-exclusao").addEventListener("click", () => {
        excluindo = null;
        dialogo.close();
        window.BussolaAlteracoes.refletir(el("mensagem"), sujo());
    });
    dialogo.addEventListener("close", () => {
        if (excluindo === null) return;
        excluindo = null;
        window.BussolaAlteracoes.refletir(el("mensagem"), sujo());
    });
    el("excluir").addEventListener("click", () => {
        const ideiaId = excluindo;
        excluindo = null;
        dialogo.close();
        executar(async () => {
            await api.requisitar(`/decisoes/${id}/ideias/${ideiaId}`, "DELETE");
            ideias = ideias.filter(ideia => ideia.id !== ideiaId);
            if (editando === ideiaId) limparFormulario();
        }, "Ideia excluída.");
    });
    el("concluir").addEventListener("click", async () => {
        if (ocupado || bloqueado || !editavel() || window.BussolaEtapas.aberta || editando !== null || conteudo.value.trim()) return;
        if (emRevisao()) {
            window.location.assign("opcoes.html?decisaoId=" + encodeURIComponent(id)
                + (decisao.etapaAtual > 3 ? "&revisao=1" : ""));
            return;
        }
        if (!await window.BussolaEtapas.confirmarAvanco()) return;
        executar(async () => {
            decisao = await api.requisitar(`/decisoes/${id}/brainstorming/concluir`, "POST");
            window.location.assign("opcoes.html?decisaoId=" + encodeURIComponent(id));
        }, "Brainstorming concluído. Suas ideias foram preservadas.");
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
            limparFormulario();
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
    el("recarregar").addEventListener("click", carregar);
    window.addEventListener("pageshow", evento => { if (evento.persisted) window.location.reload(); });
    carregar();
})();
