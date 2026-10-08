/**
 * Origem do backend local. O Spring Boot sobe em http://localhost:8080
 * quando nenhuma porta é configurada. Uma página pode definir
 * window.BUSSOLA_API_BASE antes deste script para apontar outro endereço.
 */
const ORIGEM_API = window.BUSSOLA_API_BASE || "http://localhost:8080";

/**
 * Chave do sessionStorage com a sessão devolvida por POST /auth/login.
 * O valor é JSON. Não contém senha, hash nem segredo do backend.
 */
const CHAVE_SESSAO = "bussola.sessao";

let enviando = false;

/**
 * Remove espaços das pontas de um valor de formulário.
 *
 * @param {HTMLFormElement} formulario formulário de login
 * @param {string} nome atributo name do campo
 * @returns {string} texto sem espaços nas pontas; string vazia se o campo não existir
 */
function textoDoCampo(formulario, nome) {
    const campo = formulario.elements.namedItem(nome);
    return campo ? String(campo.value).trim() : "";
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
 * @param {HTMLFormElement} formulario formulário de login
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
 * @param {HTMLFormElement} formulario formulário de login
 * @param {string} mensagem texto apresentado ao usuário
 * @returns {void}
 */
function mostrarMensagemGlobal(formulario, mensagem) {
    formulario.querySelector("#form-message").textContent = mensagem;
}

/**
 * Valida e-mail e senha antes do envio, sem regras extras de senha.
 *
 * @param {HTMLFormElement} formulario formulário de login
 * @returns {boolean} true quando os dois campos atendem ao contrato de POST /auth/login
 */
function validarFormulario(formulario) {
    limparErros(formulario);
    let valido = true;
    const email = textoDoCampo(formulario, "email");
    const senha = formulario.elements.namedItem("senha").value;

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

    return valido;
}

/**
 * Monta o JSON aceito por POST /auth/login. Os nomes dos campos são email e senha.
 *
 * @param {HTMLFormElement} formulario formulário já validado
 * @returns {{email: string, senha: string}} payload da API, sem campos extras
 */
function montarPayload(formulario) {
    return {
        email: textoDoCampo(formulario, "email"),
        senha: formulario.elements.namedItem("senha").value
    };
}

/**
 * Liga ou desliga o estado visual de envio do botão principal.
 *
 * @param {HTMLFormElement} formulario formulário de login
 * @param {boolean} ocupado true enquanto o POST está em andamento
 * @returns {void}
 */
function definirEnviando(formulario, ocupado) {
    const botao = formulario.querySelector(".submit");
    formulario.setAttribute("aria-busy", ocupado ? "true" : "false");
    botao.disabled = ocupado;
    botao.textContent = ocupado ? "Entrando..." : "Acessar minha conta →";
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
 * Guarda no sessionStorage somente o token e os dados públicos do usuário.
 * A senha digitada não entra nesse objeto. A chave some quando a aba fecha.
 *
 * @param {object} corpo corpo 200 de POST /auth/login
 * @returns {boolean} true quando accessToken e tokenType existem e foram gravados
 */
function guardarSessao(corpo) {
    if (typeof corpo.accessToken !== "string" || corpo.accessToken.length === 0) {
        return false;
    }
    if (corpo.tokenType !== "Bearer") {
        return false;
    }
    const sessao = {
        accessToken: corpo.accessToken,
        tokenType: corpo.tokenType,
        inatividadeMaximaSegundos: corpo.inatividadeMaximaSegundos
    };
    const usuario = corpo.usuario;
    if (usuario && typeof usuario === "object") {
        sessao.usuario = {
            id: usuario.id,
            nomeCompleto: usuario.nomeCompleto,
            email: usuario.email
        };
    }
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
    return true;
}

/**
 * Mostra o erro da API sem indicar se o e-mail existe.
 *
 * @param {HTMLFormElement} formulario formulário de login
 * @param {number} status código HTTP da resposta
 * @param {object} corpo corpo JSON já interpretado
 * @returns {void}
 */
function tratarErroHttp(formulario, status, corpo) {
    const erro = typeof corpo.erro === "string" ? corpo.erro : "";
    if (status === 401) {
        mostrarMensagemGlobal(formulario, erro || "E-mail ou senha inválidos.");
        return;
    }
    if (status === 400 && erro) {
        mostrarMensagemGlobal(formulario, erro);
        return;
    }
    mostrarMensagemGlobal(formulario, "Não foi possível entrar. Tente novamente.");
}

/**
 * Abre a Home autenticada depois que a API confirma o login.
 * home.html fica na mesma pasta que login.html.
 *
 * @returns {void}
 */
function irParaHome() {
    window.location.href = "home.html";
}

/**
 * Envia o login para POST /auth/login e trata sucesso, validação e falha de rede.
 * Só grava o token e muda de página quando a resposta é 200 e o corpo traz o token.
 *
 * @param {SubmitEvent} evento envio do formulário
 * @returns {Promise<void>}
 */
async function enviarLogin(evento) {
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
    let autenticado = false;
    try {
        const resposta = await fetch(`${ORIGEM_API}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify(payload)
        });
        const corpo = await lerCorpo(resposta);
        if (resposta.status === 200 && guardarSessao(corpo)) {
            autenticado = true;
            irParaHome();
            return;
        }
        if (resposta.status === 200) {
            mostrarMensagemGlobal(formulario, "Não foi possível entrar. Tente novamente.");
            return;
        }
        tratarErroHttp(formulario, resposta.status, corpo);
    } catch (erro) {
        mostrarMensagemGlobal(formulario, "Não foi possível entrar. Tente novamente.");
    } finally {
        enviando = false;
        if (!autenticado) {
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

/**
 * Avisa que a recuperação de senha ainda não tem tela nem endpoint.
 * Não chama a API e não sai de login.html.
 *
 * @param {HTMLFormElement} formulario formulário de login
 * @returns {void}
 */
function avisarRecuperacaoIndisponivel(formulario) {
    mostrarMensagemGlobal(formulario, "A recuperação de senha ainda não está disponível.");
}

/**
 * Liga o envio do formulário, a senha e o aviso de recuperação.
 *
 * @returns {void}
 */
function iniciar() {
    const formulario = document.getElementById("login-form");
    const parametros = new URLSearchParams(window.location.search);
    if (parametros.get("cadastro") === "sucesso") {
        mostrarMensagemGlobal(formulario, "Conta criada. Entre para acessar sua Home.");
    } else if (parametros.get("motivo") === "sessao-expirada") {
        mostrarMensagemGlobal(formulario, "Sua sessão expirou ou foi encerrada. Entre novamente.");
    }
    formulario.addEventListener("submit", enviarLogin);
    formulario.querySelector(".password-toggle").addEventListener("click", (evento) => {
        alternarSenha(evento.currentTarget);
    });
    document.getElementById("esqueci-senha").addEventListener("click", () => {
        avisarRecuperacaoIndisponivel(formulario);
    });
}

iniciar();
