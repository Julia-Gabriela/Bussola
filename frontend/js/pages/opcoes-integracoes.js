/** Critérios já está integrada; a navegação usa o ID da decisão salva.
 * A navegação acrescenta decisaoId. null mantém a confirmação nesta tela.
 * Também é possível interceptar o evento cancelável bussola:opcoes-concluidas.
 */
window.BUSSOLA_OPCOES_ROTAS = {
    perfil: "perfil.html",
    criterios: "criterios.html",
    historico: "historico.html",
    ...window.BUSSOLA_OPCOES_ROTAS
};
