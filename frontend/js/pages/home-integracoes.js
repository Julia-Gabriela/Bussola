/**
 * Preencher quando as telas dos outros integrantes forem entregues.
 * Caminhos relativos a frontend/pages/, por exemplo: historico: "historico.html".
 * A Home acrescenta decisaoId e etapa ao continuar, ou decisaoId ao ver o resultado.
 * null mantém a pessoa na Home com um aviso, sem criar links para arquivos ausentes.
 * Alternativa: escutar o evento cancelável bussola:navegar e chamar preventDefault().
 */
window.BUSSOLA_HOME_ROTAS = {
    historico: null,
    novaDecisao: null,
    continuarDecisao: null,
    resultadoDecisao: null,
    perfil: null,
    ...window.BUSSOLA_HOME_ROTAS
};
