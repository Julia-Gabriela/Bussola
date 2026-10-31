import { requisitosAnalise } from "./requisitos-resultado.js";

/**
 * Estado das quatro técnicas da Análise, usado só para orientar a navegação.
 *
 * Os dados vêm de leituras que já existem e são liberadas na etapa 6:
 * GET /analise (Prós e Contras e Matriz GUT, com as mesmas condições mínimas
 * de `requisitosAnalise`), GET /reflexoes-101010 e GET /inversao (campo
 * `concluida`). Nada é gravado e nenhuma condição nova é criada para o
 * Resultado, que continua sendo liberado apenas pelo backend.
 *
 * @typedef {{pros: (boolean|null), gut: (boolean|null), "101010": (boolean|null), inversao: (boolean|null)}} EstadoTecnicas
 *          true = concluída, false = pendente, null = não foi possível verificar.
 */

/**
 * Consulta o estado real das técnicas da Análise de uma decisão.
 *
 * Cada leitura é independente: se uma falhar, só a técnica correspondente
 * fica como null ("não foi possível verificar"), sem afetar as outras.
 *
 * @param {{requisitar: (caminho: string) => Promise<any>}} api cliente autenticado criado por `criarApi`
 * @param {string} decisaoId ID da decisão (já validado pela página)
 * @param {object} [analise] resposta de GET /analise já carregada pela página; evita repetir a leitura
 * @returns {Promise<EstadoTecnicas>} situação de cada técnica
 */
export async function estadoTecnicas(api, decisaoId, analise) {
    const base = "/decisoes/" + decisaoId;
    const [dadosAnalise, reflexoes, inversao] = await Promise.all([
        analise ? Promise.resolve(analise) : api.requisitar(base + "/analise").catch(() => null),
        api.requisitar(base + "/reflexoes-101010").catch(() => null),
        api.requisitar(base + "/inversao").catch(() => null)
    ]);
    const requisitos = dadosAnalise ? requisitosAnalise(dadosAnalise) : null;
    return {
        pros: requisitos ? requisitos.pros.pronto : null,
        gut: requisitos ? requisitos.gut.pronto : null,
        "101010": reflexoes ? reflexoes.concluida === true : null,
        inversao: inversao ? inversao.concluida === true : null
    };
}

/**
 * Marca na barra de técnicas quais já foram concluídas.
 *
 * Cada item da barra precisa do atributo `data-tecnica` ("pros", "gut",
 * "101010" ou "inversao"). Itens concluídos recebem a classe `concluida`, um
 * "✓" visual e o texto " — concluída" para leitores de tela. Itens pendentes
 * ou não verificados voltam ao estado normal.
 *
 * @param {(ParentNode|null)} nav elemento `.tecnicas-nav` da página
 * @param {Partial<EstadoTecnicas>} estado situação de cada técnica (chaves ausentes não mudam)
 * @returns {void}
 */
export function marcarNavegacao(nav, estado) {
    if (!nav) return;
    for (const [tecnica, concluida] of Object.entries(estado)) {
        const item = nav.querySelector(`[data-tecnica="${tecnica}"]`);
        if (!item) continue;
        item.classList.toggle("concluida", concluida === true);
        item.querySelector(".tecnica-status")?.remove();
        if (concluida !== true) continue;
        const status = document.createElement("span");
        status.className = "tecnica-status";
        const marca = document.createElement("span");
        marca.setAttribute("aria-hidden", "true");
        marca.textContent = "✓ Concluída";
        const leitor = document.createElement("span");
        leitor.className = "fluxo-sr-only";
        leitor.textContent = " — concluída";
        status.append(marca, leitor);
        item.append(status);
    }
}
