/**
 * Nomenclatura única das seis etapas da decisão, igual ao `etapaAtual` do backend
 * (1 Contextualização … 6 Análise). Usada pela Home e pelo Histórico para que o
 * texto do progresso seja o mesmo dos selos "Etapa N de 6" das telas do fluxo.
 */
(() => {
    "use strict";

    const NOMES = ["Contextualização", "Brainstorming", "Opções", "Critérios", "Pesos", "Análise"];

    /**
     * Descreve em que ponto a decisão está, no formato padrão das telas.
     *
     * @param {{status: string, etapaAtual: number, totalEtapas?: number, resultadoAlcancado?: boolean}} decisao
     *        resumo devolvido pela API (Home ou Histórico)
     * @returns {string} "Concluída" para decisões finalizadas; "Etapa 6 de 6 · Resultado disponível ·
     *          falta finalizar" quando o resultado comparativo já foi aberto; nos demais casos
     *          "Etapa N de 6 · Nome da etapa" (o nome é omitido se a etapa estiver fora de 1 a 6)
     */
    function descrever(decisao) {
        if (decisao.status === "CONCLUIDA") return "Concluída";
        const total = decisao.totalEtapas || NOMES.length;
        if (decisao.resultadoAlcancado) return `Etapa ${total} de ${total} · Resultado disponível · falta finalizar`;
        const nome = NOMES[decisao.etapaAtual - 1];
        return `Etapa ${decisao.etapaAtual} de ${total}` + (nome ? ` · ${nome}` : "");
    }

    window.BussolaProgresso = { NOMES, descrever };
})();
