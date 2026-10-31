import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js?v=2";

const el = id => document.getElementById(id);
let dados, referencia = "", ocupado = false, bloqueado = false, encerrada = false;
const foto = () => JSON.stringify([el("nome").value, el("nascimento").value, el("email").value]);
const dadosSujos = () => referencia !== "" && (foto() !== referencia || !!el("senha-email").value);
const senhasSujas = () => ["senha-atual", "nova-senha", "confirmacao-senha"].some(id => el(id).value);
const sujo = () => !encerrada && (dadosSujos() || senhasSujas() || !!el("senha-exclusao").value);
window.BussolaAlteracoes.registrar({
    sujo,
    ocupado: () => ocupado,
    descartavel: true,
    mensagem: "Você fez alterações no seu perfil que ainda não foram salvas. Deseja continuar editando ou descartá-las?"
});
function sairParaLogin(motivo) {
    encerrada = true; ocupado = false; referencia = "";
    document.querySelectorAll('input[type="password"], #nova-senha, #senha-atual, #confirmacao-senha').forEach(c => { c.value = ""; });
    sessionStorage.removeItem("bussola.sessao");
    location.replace("login.html?motivo=" + encodeURIComponent(motivo));
}
const api = criarApi(() => sairParaLogin("sessao-expirada"));
function aviso(titulo, texto) {
    el("aviso-titulo").textContent = titulo; el("aviso-texto").textContent = texto;
    if (!el("aviso").open) el("aviso").showModal();
}
function estado(valor) {
    ocupado = valor;
    el("conteudo").setAttribute("aria-busy", String(valor));
    for (const id of ["dados-campos", "senha-campos", "exclusao-campos"])
        el(id).disabled = valor || bloqueado || !dados || encerrada;
    el("abrir-exclusao").disabled = valor || bloqueado || !dados || encerrada;
    el("recarregar").disabled = valor;
}
const preencherConta = configurarConta({
    requisitar: api.requisitar, aviso, podeSair: () => !ocupado,
    aoSair: () => { encerrada = true; referencia = ""; }, ocupado: estado,
    rotas: { historico: "historico.html", perfil: "perfil.html" }
});
function conferirEmail() {
    const mudou = dados && el("email").value.trim().toLowerCase() !== dados.email.toLowerCase();
    el("confirmar-email").hidden = !mudou;
    el("senha-email").required = !!mudou;
    if (!mudou) el("senha-email").value = "";
}
function renderizar(resposta) {
    if (!resposta?.id || typeof resposta.nomeCompleto !== "string" || typeof resposta.email !== "string"
            || !/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/.test(resposta.dataNascimento))
        throw new Error("Não foi possível interpretar seu perfil. Recarregue os dados.");
    dados = resposta;
    el("nome").value = dados.nomeCompleto;
    el("nascimento").value = dados.dataNascimento;
    el("email").value = dados.email;
    el("senha-email").value = "";
    referencia = foto(); conferirEmail(); preencherConta(dados);
    try {
        const sessao = JSON.parse(sessionStorage.getItem("bussola.sessao"));
        if (sessao) { sessao.usuario = { id:dados.id, nomeCompleto:dados.nomeCompleto, email:dados.email }; sessionStorage.setItem("bussola.sessao",JSON.stringify(sessao)); }
    } catch { /* /auth/me continua sendo a fonte da sessão. */ }
    el("perfil-conteudo").hidden = false;
}
async function carregar() {
    if (ocupado || encerrada || !api.autenticada) return;
    estado(true);
    try {
        renderizar(await api.requisitar("/perfil")); bloqueado = false;
        el("mensagem").textContent = ""; el("recarregar").hidden = true;
    } catch (erro) {
        bloqueado = true; el("mensagem").textContent = erro.message;
        el("recarregar").hidden = encerrada;
    } finally { estado(false); }
}
function limparSenhas() {
    el("senha-form").reset();
    for (const id of ["senha-atual","nova-senha","confirmacao-senha"]) { el(id).type = "password"; el(id).setCustomValidity(""); }
    el("senha-mensagem").textContent = "";
}
function podeGravar() { return !ocupado && !bloqueado && !encerrada && !!dados; }
el("dados-form").addEventListener("submit", async evento => {
    evento.preventDefault(); if (!podeGravar()) return;
    const nome = el("nome").value.trim();
    el("nome").setCustomValidity(nome ? "" : "Informe seu nome completo.");
    if (!el("dados-form").reportValidity()) return;
    estado(true); el("dados-mensagem").textContent = "Salvando seus dados…";
    let confirmado = false;
    try {
        const resposta = await api.requisitar("/perfil", "PUT", {
            nomeCompleto:nome, dataNascimento:el("nascimento").value, email:el("email").value.trim(),
            senhaAtual:el("senha-email").value || null
        });
        confirmado = true; renderizar(resposta); el("dados-mensagem").textContent = "Dados atualizados.";
    } catch (erro) {
        bloqueado = confirmado || erro.incerto || erro.status >= 500;
        el("dados-mensagem").textContent = erro.message;
        el("recarregar").hidden = !bloqueado || encerrada;
    } finally { el("senha-email").value = ""; estado(false); }
});
el("email").addEventListener("input", conferirEmail);
el("nome").addEventListener("input", () => el("nome").setCustomValidity(""));
el("cancelar-dados").addEventListener("click", () => {
    if (podeGravar() && (!dadosSujos() || window.confirm("Descartar as alterações dos dados pessoais?"))) {
        renderizar(dados); el("dados-mensagem").textContent = "";
    }
});
el("limpar-senhas").addEventListener("click", limparSenhas);
el("mostrar-senhas").addEventListener("change", () => {
    for (const id of ["senha-atual","nova-senha","confirmacao-senha"]) el(id).type = el("mostrar-senhas").checked ? "text" : "password";
});
for (const id of ["nova-senha","confirmacao-senha"])
    el(id).addEventListener("input", () => { el("nova-senha").setCustomValidity(""); el("confirmacao-senha").setCustomValidity(""); });
el("senha-form").addEventListener("submit", async evento => {
    evento.preventDefault(); if (!podeGravar()) return;
    if (dadosSujos()) { aviso("Dados pessoais não salvos", "Salve ou descarte as alterações dos dados pessoais antes de trocar a senha."); return; }
    const nova = el("nova-senha").value;
    el("nova-senha").setCustomValidity(new TextEncoder().encode(nova).length > 72 ? "A senha excede 72 bytes. Use uma senha um pouco menor." : "");
    el("confirmacao-senha").setCustomValidity(nova !== el("confirmacao-senha").value ? "As senhas não correspondem." : "");
    if (!el("senha-form").reportValidity()) return;
    estado(true); el("senha-mensagem").textContent = "Alterando sua senha…";
    try {
        await api.requisitar("/perfil/senha", "PUT", {
            senhaAtual:el("senha-atual").value, novaSenha:nova, confirmacaoSenha:el("confirmacao-senha").value
        });
        sairParaLogin("senha-alterada");
    } catch (erro) {
        if (erro.incerto || erro.status >= 500) sairParaLogin("seguranca-incerta");
        else { el("senha-mensagem").textContent = erro.message; el("senha-atual").value = ""; }
    } finally { estado(false); }
});
el("abrir-exclusao").addEventListener("click", () => {
    if (!podeGravar()) return;
    if (dadosSujos() || senhasSujas()) { aviso("Alterações não salvas", "Salve ou descarte os dados pessoais e limpe os campos de senha antes de continuar."); return; }
    el("exclusao-form").reset(); el("exclusao-mensagem").textContent = ""; el("exclusao").showModal();
});
el("cancelar-exclusao").addEventListener("click", () => el("exclusao").close());
el("exclusao").addEventListener("cancel", evento => { if (ocupado) evento.preventDefault(); });
el("exclusao").addEventListener("close", () => { el("exclusao-form").reset(); el("exclusao-mensagem").textContent = ""; });
el("exclusao-form").addEventListener("submit", async evento => {
    evento.preventDefault(); if (!podeGravar() || !el("exclusao-form").reportValidity()) return;
    estado(true); el("exclusao-mensagem").textContent = "Excluindo a conta…";
    try {
        await api.requisitar("/perfil", "DELETE", { senhaAtual:el("senha-exclusao").value, confirmar:el("confirmar-exclusao").checked });
        sairParaLogin("conta-excluida");
    } catch (erro) {
        if (erro.incerto || erro.status >= 500) sairParaLogin("exclusao-incerta");
        else { el("exclusao-mensagem").textContent = erro.message; el("senha-exclusao").value = ""; }
    } finally { estado(false); }
});
el("recarregar").addEventListener("click", () => {
    if (ocupado) return;
    if (!sujo() || window.confirm("Recarregar descarta o que não foi salvo. Deseja continuar?")) {
        limparSenhas(); el("senha-exclusao").value = ""; carregar();
    }
});
document.querySelectorAll("[data-documento]").forEach(botao => botao.addEventListener("click", () => {
    const documento = window.DOCUMENTOS_LEGAIS[botao.dataset.documento];
    el("documento-titulo").textContent = documento.titulo;
    el("documento-versao").textContent = documento.versao;
    const corpo = el("documento-corpo"); corpo.replaceChildren(); let lista;
    for (const bloco of documento.blocos) {
        if (bloco.tipo !== "item") lista = null;
        const no = document.createElement(bloco.tipo === "secao" ? "h3" : bloco.tipo === "subsecao" ? "h4" : bloco.tipo === "item" ? "li" : "p");
        no.textContent = bloco.texto;
        if (bloco.tipo === "item") { if (!lista) { lista = document.createElement("ul"); corpo.append(lista); } lista.append(no); }
        else corpo.append(no);
    }
    el("documento").showModal(); el("documento").scrollTop = 0;
}));
const hoje = new Date();
const limite = new Date(hoje.getFullYear() - 16, hoje.getMonth(), hoje.getDate());
el("nascimento").max = [limite.getFullYear(), String(limite.getMonth()+1).padStart(2,"0"), String(limite.getDate()).padStart(2,"0")].join("-");
window.addEventListener("pageshow", evento => { if (evento.persisted) location.reload(); });
carregar();
