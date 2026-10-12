(() => {
    "use strict";
    let dialogo;
    let pendente = false;

    function criarDialogo() {
        dialogo = document.createElement("dialog");
        dialogo.id = "confirmar-avanco";
        dialogo.className = "confirmacao-etapa";
        dialogo.setAttribute("aria-labelledby", "confirmacao-etapa-titulo");
        dialogo.setAttribute("aria-describedby", "confirmacao-etapa-texto");
        dialogo.innerHTML = `
            <h2 id="confirmacao-etapa-titulo">Concluir esta etapa?</h2>
            <p id="confirmacao-etapa-texto">Ao avançar, esta etapa será concluída e ficará disponível apenas para consulta. Você não poderá editar as informações desta etapa. Deseja continuar?</p>
            <form method="dialog" class="confirmacao-etapa-acoes">
                <button type="submit" value="cancelar" autofocus>Continuar revisando</button>
                <button type="submit" value="confirmar" class="confirmacao-etapa-avancar">Confirmar e avançar</button>
            </form>
        `;
        document.body.append(dialogo);
    }

    function confirmarAvanco() {
        // Uma segunda tentativa enquanto o diálogo está aberto nunca autoriza outro envio.
        if (pendente) return Promise.resolve(false);
        if (!dialogo) criarDialogo();
        pendente = true;
        dialogo.returnValue = "";
        return new Promise(resolve => {
            dialogo.addEventListener("close", () => {
                pendente = false;
                resolve(dialogo.returnValue === "confirmar");
            }, { once: true });
            dialogo.showModal();
        });
    }

    window.BussolaEtapas = Object.freeze({
        confirmarAvanco,
        get aberta() { return pendente; }
    });
})();
