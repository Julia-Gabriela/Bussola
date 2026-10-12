/** Critérios já está integrada; a navegação usa o ID da decisão salva.
 * A navegação acrescenta decisaoId. null mantém a confirmação nesta tela.
 * Também é possível interceptar o evento cancelável bussola:opcoes-concluidas.
 */
window.BUSSOLA_OPCOES_ROTAS = {
    criterios: "criterios.html",
    historico: null,
    ...window.BUSSOLA_OPCOES_ROTAS
};
