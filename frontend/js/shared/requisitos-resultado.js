/**
 * Descreve o que falta em Prós e Contras e na Matriz GUT para liberar o
 * resultado comparativo, com as mesmas condições mínimas que o backend exige:
 * ao menos um pró e um contra em cada alternativa e ao menos um item
 * classificado na Matriz GUT.
 *
 * Serve apenas para orientar. A liberação final continua sendo decidida pelo
 * backend em GET /decisoes/{id}/resultado.
 *
 * @param {{opcoes: Array<{prosContras: Array<{tipo: string}>}>, fatoresGut: Array<object>}} analise
 *        Resposta de GET /decisoes/{id}/analise; a primeira opção é A e a segunda é B.
 * @returns {{pros: {pronto: boolean, texto: string}, gut: {pronto: boolean, texto: string}}}
 *          Situação de cada técnica com um texto curto, pronto para exibir após o nome dela.
 */
export function requisitosAnalise(analise) {
    const faltas = analise.opcoes.flatMap((opcao, i) => {
        const letra = i === 0 ? "A" : "B";
        const tipos = opcao.prosContras.map(item => item.tipo);
        return [["PRO", "um pró"], ["CONTRA", "um contra"]]
            .filter(([tipo]) => !tipos.includes(tipo)).map(([, nome]) => nome + " em " + letra);
    });
    const lista = faltas.length > 1 ? faltas.slice(0, -1).join(", ") + " e " + faltas.at(-1) : faltas[0];
    const total = analise.fatoresGut.length;
    return {
        pros: { pronto: !faltas.length, texto: faltas.length
            ? "falta registrar ao menos " + lista + "."
            : "pronto, há prós e contras nas duas alternativas." },
        gut: { pronto: total > 0, texto: total
            ? "pronto, " + total + (total === 1 ? " item classificado." : " itens classificados.")
            : "falta classificar ao menos um item." }
    };
}

/**
 * Descreve as avaliações de critérios que ainda precisam ser confirmadas.
 *
 * Usa os campos `avaliacaoPendenteA` e `avaliacaoPendenteB` de
 * GET /decisoes/{id}/criterios, que vêm da mesma coluna
 * (`opcoes.avaliacao_pendente`) que o backend consulta para recusar o
 * resultado. Não confunde com `resultados.requer_atualizacao`: esse sinal só
 * indica que uma pontuação antiga será recalculada e não bloqueia o resultado.
 *
 * @param {{avaliacaoPendenteA: boolean, avaliacaoPendenteB: boolean,
 *          opcaoA: {nome: string}, opcaoB: {nome: string}}} criterios Resposta de GET /criterios.
 * @returns {{pronto: boolean, alternativas: string[], texto: string}}
 *          `pronto` é true quando nenhuma alternativa está pendente; `alternativas`
 *          lista as pendentes no formato "A (nome)"; `texto` é a frase para exibir
 *          depois do rótulo "Avaliações dos critérios:".
 */
export function pendenciasAvaliacao(criterios) {
    const alternativas = [];
    if (criterios.avaliacaoPendenteA === true) alternativas.push("A (" + criterios.opcaoA.nome + ")");
    if (criterios.avaliacaoPendenteB === true) alternativas.push("B (" + criterios.opcaoB.nome + ")");
    return {
        pronto: !alternativas.length,
        alternativas,
        texto: alternativas.length
            ? "confirme as notas " + (alternativas.length > 1 ? "das alternativas " : "da alternativa ") + alternativas.join(" e ") + " em Critérios."
            : "pronto, notas confirmadas."
    };
}

/**
 * Monta o endereço de Critérios em modo de revisão para confirmar notas.
 *
 * O parâmetro `retorno` faz Critérios oferecer o botão de volta à tela de
 * origem depois da confirmação.
 *
 * @param {string} decisaoId ID da decisão (já validado pela página).
 * @param {"inversao"|"resultado"} retorno Tela para a qual a pessoa deve voltar.
 * @returns {string} URL relativa de `criterios.html` com `revisao=1`.
 */
export function urlRevisarAvaliacoes(decisaoId, retorno) {
    return "criterios.html?decisaoId=" + encodeURIComponent(decisaoId) + "&revisao=1&retorno=" + retorno;
}

/**
 * Marca um item da lista "Para liberar o resultado comparativo".
 *
 * O item precisa ter o atributo data-requisito e conter um elemento
 * `.pendencia-texto`; a classe `pronto` pinta a marca de verde.
 *
 * @param {ParentNode} lista Elemento que contém os itens (por exemplo, o `<ul>`).
 * @param {string} requisito Valor de data-requisito do item a atualizar.
 * @param {boolean} pronto true quando a condição já foi atendida.
 * @param {string} texto Situação exibida ao lado do nome da técnica.
 * @returns {void}
 */
export function marcarRequisito(lista, requisito, pronto, texto) {
    const item = lista.querySelector(`[data-requisito="${requisito}"]`);
    if (!item) return;
    item.classList.toggle("pronto", pronto);
    item.querySelector(".pendencia-texto").textContent = texto;
}
