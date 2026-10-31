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
     * Distingue quatro situações sem inventar estado: decisão finalizada
     * (`status` CONCLUIDA), Resultado já aberto (`resultadoAlcancado === true`,
     * registrado pelo backend quando o Resultado é exibido), análise em
     * andamento (etapa 6 com `resultadoAlcancado === false`) e etapa alcançada
     * nas demais. Estar na etapa 6 não basta para dizer "Resultado disponível":
     * se o campo `resultadoAlcancado` não vier na resposta, o texto fica neutro
     * ("Etapa 6 de 6 · Análise").
     *
     * @param {{status: string, etapaAtual: number, totalEtapas?: number, resultadoAlcancado?: boolean}} decisao
     *        resumo devolvido pela API (Home ou Histórico)
     * @returns {string} "Concluída · decisão finalizada"; "Resultado disponível · falta finalizar";
     *          "Etapa 6 de 6 · Análise em andamento"; ou "Etapa N de 6 · Nome da etapa"
     *          (o nome é omitido se a etapa estiver fora de 1 a 6)
     */
    function descrever(decisao) {
        if (decisao.status === "CONCLUIDA") return "Concluída · decisão finalizada";
        const total = decisao.totalEtapas || NOMES.length;
        if (decisao.resultadoAlcancado === true) return "Resultado disponível · falta finalizar";
        const nome = NOMES[decisao.etapaAtual - 1];
        if (decisao.etapaAtual === total && decisao.resultadoAlcancado === false) {
            return `Etapa ${total} de ${total} · Análise em andamento`;
        }
        return `Etapa ${decisao.etapaAtual} de ${total}` + (nome ? ` · ${nome}` : "");
    }

    window.BussolaProgresso = { NOMES, descrever };
})();
