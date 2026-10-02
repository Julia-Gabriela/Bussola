/**
 * Origem do backend local. O Spring Boot sobe em http://localhost:8080
 * quando nenhuma porta é configurada. Uma página pode definir
 * window.BUSSOLA_API_BASE antes deste script para apontar outro endereço.
 */
const ORIGEM_API = window.BUSSOLA_API_BASE || "http://localhost:8080";

let enviando = false;

/**
 * Remove espaços das pontas de um valor de formulário.
 *
 * @param {HTMLFormElement} formulario formulário de cadastro
 * @param {string} nome atributo name do campo
 * @returns {string} texto sem espaços nas pontas; string vazia se o campo não existir
 */
function textoDoCampo(formulario, nome) {
    const campo = formulario.elements.namedItem(nome);
    return campo ? String(campo.value).trim() : "";
}

/**
 * Confere se uma data ISO representa um dia real do calendário.
 *
 * @param {string} iso data no formato yyyy-MM-dd
 * @returns {Date|null} data local à meia-noite, ou null se o texto não for um dia existente
 */
function dataCalendario(iso) {
    const partes = iso.split("-");
    if (partes.length !== 3) {
        return null;
    }
    const ano = Number(partes[0]);
    const mes = Number(partes[1]);
    const dia = Number(partes[2]);
    if (!Number.isInteger(ano) || !Number.isInteger(mes) || !Number.isInteger(dia)) {
        return null;
    }
    const data = new Date(ano, mes - 1, dia);
    data.setHours(0, 0, 0, 0);
    if (data.getFullYear() !== ano || data.getMonth() !== mes - 1 || data.getDate() !== dia) {
        return null;
    }
    return data;
}

/**
 * Informa se a pessoa já completou 16 anos, inclusive no aniversário de hoje.
 *
 * @param {Date} nascimento data de nascimento já validada, à meia-noite local
 * @returns {boolean} true quando a data é anterior ou igual ao dia em que a pessoa completa 16 anos
 */
function temPeloMenosDezesseisAnos(nascimento) {
    const hoje = new Date();
    const limite = new Date(hoje.getFullYear() - 16, hoje.getMonth(), hoje.getDate());
    limite.setHours(0, 0, 0, 0);
    return nascimento.getTime() <= limite.getTime();
}

/**
 * Confere o formato básico de e-mail aceito pelo formulário.
 *
 * @param {string} email e-mail já sem espaços nas pontas
 * @returns {boolean} true quando há um texto antes e depois de @ e um ponto no domínio
 */
function emailComFormatoValido(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

/**
 * Apaga as mensagens de erro e o estado inválido dos campos.
 *
 * @param {HTMLFormElement} formulario formulário de cadastro
 * @returns {void}
 */
function limparErros(formulario) {
    formulario.querySelectorAll("[aria-invalid]").forEach((campo) => {
        campo.removeAttribute("aria-invalid");
    });
    formulario.querySelectorAll(".field-error, .form-message").forEach((mensagem) => {
        mensagem.textContent = "";
    });
}

/**
 * Mostra uma mensagem curta ligada a um campo e marca esse campo como inválido.
 *
 * @param {HTMLElement} campo input associado ao erro
 * @param {HTMLElement} destino parágrafo que recebe a mensagem
 * @param {string} mensagem texto apresentado ao usuário
 * @returns {void}
 */
function mostrarErroCampo(campo, destino, mensagem) {
    campo.setAttribute("aria-invalid", "true");
    destino.textContent = mensagem;
}

/**
 * Publica uma mensagem geral do formulário, fora de um campo específico.
 *
 * @param {HTMLFormElement} formulario formulário de cadastro
 * @param {string} mensagem texto apresentado ao usuário
 * @returns {void}
 */
function mostrarMensagemGlobal(formulario, mensagem) {
    const destino = formulario.querySelector("#form-message");
    destino.textContent = mensagem;
}

/**
 * Valida nome, data, e-mail, senha e aceite dos termos antes do envio.
 *
 * @param {HTMLFormElement} formulario formulário de cadastro
 * @returns {boolean} true quando todos os campos atendem ao contrato atual
 */
function validarFormulario(formulario) {
    limparErros(formulario);
    let valido = true;
    const nome = textoDoCampo(formulario, "nomeCompleto");
    const dataTexto = textoDoCampo(formulario, "dataNascimento");
    const email = textoDoCampo(formulario, "email");
    const senha = formulario.elements.namedItem("senha").value;
    const termos = formulario.elements.namedItem("aceitouTermos").checked;

    if (!nome) {
        mostrarErroCampo(formulario.elements.namedItem("nomeCompleto"), formulario.querySelector("#nome-erro"), "Informe seu nome completo.");
        valido = false;
    } else if (nome.length > 150) {
        mostrarErroCampo(formulario.elements.namedItem("nomeCompleto"), formulario.querySelector("#nome-erro"), "O nome completo pode ter no máximo 150 caracteres.");
        valido = false;
    }

    const nascimento = dataCalendario(dataTexto);
    if (!nascimento) {
        mostrarErroCampo(formulario.elements.namedItem("dataNascimento"), formulario.querySelector("#data-erro"), "Informe uma data de nascimento válida.");
        valido = false;
    } else if (!temPeloMenosDezesseisAnos(nascimento)) {
        mostrarErroCampo(formulario.elements.namedItem("dataNascimento"), formulario.querySelector("#data-erro"), "Você precisa ter pelo menos 16 anos.");
        valido = false;
    }

    if (!email || !emailComFormatoValido(email)) {
        mostrarErroCampo(formulario.elements.namedItem("email"), formulario.querySelector("#email-erro"), "Informe um e-mail válido.");
        valido = false;
    } else if (email.length > 150) {
        mostrarErroCampo(formulario.elements.namedItem("email"), formulario.querySelector("#email-erro"), "O e-mail pode ter no máximo 150 caracteres.");
        valido = false;
    }

    if (!senha.trim()) {
        mostrarErroCampo(formulario.elements.namedItem("senha"), formulario.querySelector("#senha-erro"), "Informe sua senha.");
        valido = false;
    }

    if (!termos) {
        mostrarErroCampo(formulario.elements.namedItem("aceitouTermos"), formulario.querySelector("#termos-erro"), "Você precisa aceitar os termos para continuar.");
        valido = false;
    }

    return valido;
}

/**
 * Monta o JSON aceito por POST /auth/cadastro, sem campos extras.
 *
 * @param {HTMLFormElement} formulario formulário já validado
 * @returns {{nomeCompleto: string, dataNascimento: string, email: string, senha: string, aceitouTermos: boolean}} payload da API
 */
function montarPayload(formulario) {
    return {
        nomeCompleto: textoDoCampo(formulario, "nomeCompleto"),
        dataNascimento: textoDoCampo(formulario, "dataNascimento"),
        email: textoDoCampo(formulario, "email"),
        senha: formulario.elements.namedItem("senha").value,
        aceitouTermos: true
    };
}

/**
 * Liga ou desliga o estado visual de envio do botão principal.
 *
 * @param {HTMLFormElement} formulario formulário de cadastro
 * @param {boolean} ocupado true enquanto o POST está em andamento
 * @returns {void}
 */
function definirEnviando(formulario, ocupado) {
    const botao = formulario.querySelector(".submit");
    formulario.setAttribute("aria-busy", ocupado ? "true" : "false");
    botao.disabled = ocupado;
    botao.textContent = ocupado ? "Criando conta..." : "Criar conta gratuita →";
}

/**
 * Lê o corpo JSON de uma resposta. Corpo vazio ou inválido vira um objeto vazio.
 *
 * @param {Response} resposta resposta do fetch
 * @returns {Promise<object>} objeto JSON ou objeto vazio
 */
async function lerCorpo(resposta) {
    const texto = await resposta.text();
    if (!texto) {
        return {};
    }
    try {
        return JSON.parse(texto);
    } catch (erro) {
        return {};
    }
}

/**
 * Distribui 400 e 409 nos campos correspondentes e usa mensagem geral nos demais erros.
 *
 * @param {HTMLFormElement} formulario formulário de cadastro
 * @param {number} status código HTTP da resposta
 * @param {object} corpo corpo JSON já interpretado
 * @returns {void}
 */
function tratarErroHttp(formulario, status, corpo) {
    const erro = typeof corpo.erro === "string" ? corpo.erro : "";
    if (status === 409) {
        mostrarErroCampo(
            formulario.elements.namedItem("email"),
            formulario.querySelector("#email-erro"),
            erro || "Este e-mail já está cadastrado."
        );
        return;
    }
    if (status === 400 && erro.includes("16 anos")) {
        mostrarErroCampo(
            formulario.elements.namedItem("dataNascimento"),
            formulario.querySelector("#data-erro"),
            erro
        );
        return;
    }
    if (status === 400 && erro) {
        mostrarMensagemGlobal(formulario, erro);
        return;
    }
    mostrarMensagemGlobal(formulario, "Não foi possível concluir o cadastro. Tente novamente.");
}

/**
 * Abre a Home provisória depois que a API confirma o cadastro.
 * home.html fica na mesma pasta que cadastro.html.
 *
 * @returns {void}
 */
function irParaHome() {
    window.location.href = "home.html";
}

/**
 * Envia o cadastro para POST /auth/cadastro e trata sucesso, validação e falha de rede.
 *
 * @param {SubmitEvent} evento envio do formulário
 * @returns {Promise<void>}
 */
async function enviarCadastro(evento) {
    evento.preventDefault();
    if (enviando) {
        return;
    }
    const formulario = evento.currentTarget;
    if (!validarFormulario(formulario)) {
        return;
    }
    const payload = montarPayload(formulario);
    enviando = true;
    definirEnviando(formulario, true);
    try {
        const resposta = await fetch(`${ORIGEM_API}/auth/cadastro`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify(payload)
        });
        const corpo = await lerCorpo(resposta);
        if (resposta.status === 201) {
            irParaHome();
            return;
        }
        tratarErroHttp(formulario, resposta.status, corpo);
    } catch (erro) {
        mostrarMensagemGlobal(formulario, "Não foi possível concluir o cadastro. Tente novamente.");
    } finally {
        enviando = false;
        if (!formulario.hidden) {
            definirEnviando(formulario, false);
        }
    }
}

/**
 * Alterna o campo de senha entre oculto e visível.
 *
 * @param {HTMLButtonElement} botao botão do ícone de olho
 * @returns {void}
 */
function alternarSenha(botao) {
    const campo = document.getElementById(botao.getAttribute("aria-controls"));
    const visivel = campo.type === "text";
    campo.type = visivel ? "password" : "text";
    botao.setAttribute("aria-pressed", visivel ? "false" : "true");
    botao.setAttribute("aria-label", visivel ? "Mostrar senha" : "Ocultar senha");
}

let origemDoModal = null;

/**
 * Monta o texto oficial de um documento dentro da área rolável do modal.
 *
 * @param {HTMLElement} destino área de conteúdo do modal
 * @param {{blocos: Array<{tipo: string, texto: string}>}} documento política ou termos
 * @returns {void}
 */
function preencherDocumento(destino, documento) {
    destino.replaceChildren();
    destino.scrollTop = 0;
    let lista = null;
    documento.blocos.forEach((bloco) => {
        if (bloco.tipo !== "item") {
            lista = null;
        }
        if (bloco.tipo === "secao") {
            const titulo = document.createElement("h3");
            titulo.textContent = bloco.texto;
            destino.append(titulo);
            return;
        }
        if (bloco.tipo === "subsecao") {
            const titulo = document.createElement("h4");
            titulo.textContent = bloco.texto;
            destino.append(titulo);
            return;
        }
        if (bloco.tipo === "item") {
            if (!lista) {
                lista = document.createElement("ul");
                destino.append(lista);
            }
            const item = document.createElement("li");
            item.textContent = bloco.texto;
            lista.append(item);
            return;
        }
        const paragrafo = document.createElement("p");
        paragrafo.textContent = bloco.texto;
        destino.append(paragrafo);
    });
}

/**
 * Lista os controles do modal que podem receber foco pelo teclado.
 *
 * @param {HTMLElement} dialog elemento do diálogo
 * @returns {HTMLElement[]} controles visíveis e habilitados, na ordem do documento
 */
function controlesDoModal(dialog) {
    return [...dialog.querySelectorAll("button, [href], [tabindex]")].filter((elemento) => {
        return !elemento.disabled && elemento.getAttribute("tabindex") !== "-1";
    });
}

/**
 * Mantém o Tab e o Shift+Tab circulando apenas dentro do modal.
 *
 * @param {KeyboardEvent} evento tecla pressionada enquanto o modal está aberto
 * @returns {void}
 */
function prenderFoco(evento) {
    if (evento.key !== "Tab") {
        return;
    }
    const dialog = document.getElementById("legal-dialog");
    const controles = controlesDoModal(dialog);
    if (controles.length === 0) {
        return;
    }
    const primeiro = controles[0];
    const ultimo = controles[controles.length - 1];
    if (evento.shiftKey && document.activeElement === primeiro) {
        evento.preventDefault();
        ultimo.focus();
    } else if (!evento.shiftKey && document.activeElement === ultimo) {
        evento.preventDefault();
        primeiro.focus();
    }
}

/**
 * Abre o modal com a Política de Privacidade ou com os Termos de Uso.
 *
 * @param {string} chave "politica" ou "termos"
 * @param {HTMLElement} origem botão que abriu o documento, usado para devolver o foco
 * @returns {void}
 */
function abrirDocumento(chave, origem) {
    const documento = window.DOCUMENTOS_LEGAIS[chave];
    const overlay = document.getElementById("legal-overlay");
    const dialog = document.getElementById("legal-dialog");
    document.getElementById("legal-titulo").textContent = documento.titulo;
    document.getElementById("legal-versao").textContent = documento.versao;
    preencherDocumento(document.getElementById("legal-body"), documento);
    origemDoModal = origem;
    document.querySelector(".page").inert = true;
    document.body.style.overflow = "hidden";
    overlay.hidden = false;
    dialog.focus();
}

/**
 * Fecha o modal e, se a ação for de aceite ou recusa, atualiza o checkbox único.
 *
 * @param {"manter"|"aceitar"|"negar"} acao manter preserva o checkbox; aceitar marca; negar desmarca
 * @returns {void}
 */
function fecharDocumento(acao) {
    const overlay = document.getElementById("legal-overlay");
    if (overlay.hidden) {
        return;
    }
    const checkbox = document.getElementById("aceitouTermos");
    if (acao === "aceitar") {
        checkbox.checked = true;
        checkbox.removeAttribute("aria-invalid");
        document.getElementById("termos-erro").textContent = "";
    } else if (acao === "negar") {
        checkbox.checked = false;
    }
    overlay.hidden = true;
    document.querySelector(".page").inert = false;
    document.body.style.overflow = "";
    if (origemDoModal) {
        origemDoModal.focus();
    }
}

/**
 * Liga a abertura dos documentos e as ações de fechar, aceitar e negar.
 *
 * @returns {void}
 */
function iniciarModal() {
    document.querySelectorAll(".legal").forEach((botao) => {
        botao.addEventListener("click", (evento) => {
            evento.preventDefault();
            evento.stopPropagation();
            abrirDocumento(botao.dataset.documento, botao);
        });
    });
    document.getElementById("legal-aceitar").addEventListener("click", () => fecharDocumento("aceitar"));
    document.getElementById("legal-negar").addEventListener("click", () => fecharDocumento("negar"));
    document.getElementById("legal-fechar").addEventListener("click", () => fecharDocumento("manter"));
    document.getElementById("legal-overlay").addEventListener("click", (evento) => {
        if (evento.target.id === "legal-overlay") {
            fecharDocumento("manter");
        }
    });
    document.addEventListener("keydown", (evento) => {
        const aberto = !document.getElementById("legal-overlay").hidden;
        if (!aberto) {
            return;
        }
        if (evento.key === "Escape") {
            evento.preventDefault();
            fecharDocumento("manter");
            return;
        }
        prenderFoco(evento);
    });
}

/**
 * Liga o envio do formulário, a senha e o modal dos documentos.
 *
 * @returns {void}
 */
function iniciar() {
    const formulario = document.getElementById("cadastro-form");
    formulario.addEventListener("submit", enviarCadastro);
    formulario.querySelector(".password-toggle").addEventListener("click", (evento) => {
        alternarSenha(evento.currentTarget);
    });
    iniciarModal();
}

iniciar();
