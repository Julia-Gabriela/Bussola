import { criarApi } from "../shared/api.js";
import { configurarConta } from "../shared/conta.js?v=2";
const el = id => document.getElementById(id);
let ocupado = false, encerrada = false, resposta = null, selecionada = null, abridorExclusao = null, pagina = 0;
const parametros = new URLSearchParams(location.search);
el("busca").value = (parametros.get("busca") || "").slice(0,200);
el("status").value = ["CONCLUIDA","EM_ANDAMENTO"].includes(parametros.get("status")) ? parametros.get("status") : "";
el("ordem").value = ["recentes","antigas","titulo"].includes(parametros.get("ordem")) ? parametros.get("ordem") : "recentes";
if (/^\d{1,6}$/.test(parametros.get("pagina") || "")) pagina = Number(parametros.get("pagina"));
const api = criarApi(() => { encerrada = true; location.replace("login.html?motivo=sessao-expirada"); });
function aviso(titulo,texto) { el("aviso-titulo").textContent=titulo; el("aviso-texto").textContent=texto; if(!el("aviso").open) el("aviso").showModal(); }
function estado(valor) {
    ocupado=valor; el("conteudo").setAttribute("aria-busy",String(valor)); el("campos-filtros").disabled=valor;
    el("anterior").disabled=valor || !resposta || pagina===0;
    el("proxima").disabled=valor || !resposta || pagina+1>=resposta.totalPaginas;
    el("excluir").disabled=valor; el("recarregar").disabled=valor;
    document.querySelectorAll(".historico-acoes button").forEach(b=>{b.disabled=valor;});
}
window.BussolaAlteracoes.registrar({sujo:()=>false,ocupado:()=>ocupado});
const preencherConta=configurarConta({requisitar:api.requisitar,aviso,podeSair:()=>!ocupado,aoSair:()=>{},ocupado:estado,rotas:{historico:"historico.html"}});
el("historico").setAttribute("aria-current","page"); el("historico").classList.add("is-current");
function criar(tag,texto) { const no=document.createElement(tag); no.textContent=texto; return no; }
function abrir(item) {
    if(ocupado) return;
    let rota;
    if(item.status==="CONCLUIDA") {
        const evento=new CustomEvent("bussola:ver-resultado",{cancelable:true,detail:{decisaoId:item.id,status:item.status}});
        if(!window.dispatchEvent(evento)) return;
        rota=window.BUSSOLA_HISTORICO_ROTAS?.resultadoDecisao;
        if(!rota) { aviso("Análise concluída","Sua decisão está registrada. A tela do resultado consolidado estará disponível em uma próxima entrega."); return; }
    } else rota=item.resultadoAlcancado?"resultado.html":item.etapaAtual===6?"analise.html":item.etapaAtual===5?"pesos.html"
        :item.etapaAtual>=4?"criterios.html":item.etapaAtual===3?"opcoes.html":item.etapaAtual===2?"brainstorming.html":"nova-decisao.html";
    const url=new URL(rota,location.href);
    if(url.origin!==location.origin || !["http:","https:"].includes(url.protocol)) { aviso("Página indisponível","Não foi possível abrir esta página."); return; }
    url.searchParams.set("decisaoId",item.id); location.assign(url.href);
}
function renderizar() {
    el("totais").textContent=`${resposta.concluidas} concluídas · ${resposta.emAndamento} em andamento`;
    el("decisoes").replaceChildren(...resposta.decisoes.map(item=>{
        const li=document.createElement("li");li.className="historico-item"+(item.status==="CONCLUIDA"?" concluida":"");
        const texto=document.createElement("div");texto.className="historico-texto";
        texto.append(criar("h2",item.titulo));const status=criar("span",item.status==="CONCLUIDA"?"Concluída":"Em andamento");status.className="historico-status";texto.append(status);
        if(item.status!=="CONCLUIDA") { const progresso=document.createElement("progress");progresso.max=100;progresso.value=item.progressoPercentual;progresso.setAttribute("aria-label","Progresso da decisão");const etapa=criar("p",window.BussolaProgresso.descrever(item));etapa.className="historico-etapa";texto.append(etapa,progresso,criar("p",`Progresso do caminho: ${item.progressoPercentual}%`)); }
        const concluida=item.status==="CONCLUIDA";
        const data=new Date(concluida&&item.finalizadaEm?item.finalizadaEm:item.dataAtualizacao);texto.append(criar("p",(concluida?"Finalizada em ":"Atualizada em ")+(Number.isNaN(data.getTime())?"data indisponível":new Intl.DateTimeFormat("pt-BR",{dateStyle:"medium"}).format(data))));
        const acoes=document.createElement("div");acoes.className="historico-acoes";
        const continuar=criar("button",concluida?"Visualizar resultado":"Continuar decisão");continuar.type="button";continuar.className="fluxo-proxima";continuar.addEventListener("click",()=>abrir(item));
        const excluir=criar("button",concluida?"Excluir decisão":"Excluir");excluir.type="button";excluir.className="fluxo-salvar botao-perigo";excluir.setAttribute("aria-label","Excluir decisão: "+item.titulo);
        excluir.addEventListener("click",()=>{if(ocupado)return;selecionada=item;abridorExclusao=excluir;el("exclusao-nome").textContent=item.titulo;el("exclusao").showModal();el("cancelar-exclusao").focus();});
        acoes.append(continuar,excluir);li.append(texto,acoes);return li;
    }));
    el("pagina").textContent=resposta.totalRegistros ? `Página ${pagina+1} de ${resposta.totalPaginas} · ${resposta.totalRegistros} decisões` : "0 decisões";
    el("mensagem").textContent=resposta.decisoes.length?"":resposta.concluidas+resposta.emAndamento===0?"Você ainda não tem decisões. Comece em Nova decisão.":"Nenhuma decisão encontrada com esses filtros.";
}
async function consultar() {
    const params=new URLSearchParams({busca:el("busca").value.trim(),ordem:el("ordem").value,pagina:String(pagina),tamanho:"10"});
    if(el("status").value)params.set("status",el("status").value);
    resposta=await api.requisitar("/decisoes?"+params);
    if(!Array.isArray(resposta.decisoes)||!Number.isInteger(resposta.totalPaginas))throw new Error("Não foi possível interpretar o histórico.");
    if(pagina>0 && pagina>=resposta.totalPaginas) { pagina=Math.max(0,resposta.totalPaginas-1);return consultar(); }
    history.replaceState(null,"","?"+params);renderizar();
}
async function carregar() {
    if(ocupado||encerrada||!api.autenticada)return;
    estado(true);el("recarregar").hidden=true;
    try {preencherConta(await api.requisitar("/auth/me"));await consultar();}
    catch(erro) {resposta=null;el("decisoes").replaceChildren();el("pagina").textContent="";el("mensagem").textContent=erro.message;el("recarregar").hidden=false;}
    finally {estado(false);}
}
el("filtros").addEventListener("submit",evento=>{evento.preventDefault();if(ocupado)return;pagina=0;carregar();});
el("limpar").addEventListener("click",()=>{if(ocupado)return;el("filtros").reset();pagina=0;carregar();});
for(const campo of ["status","ordem"])el(campo).addEventListener("change",()=>{pagina=0;carregar();});
el("anterior").addEventListener("click",()=>{if(!ocupado&&pagina>0){pagina--;carregar();}});
el("proxima").addEventListener("click",()=>{if(!ocupado&&resposta&&pagina+1<resposta.totalPaginas){pagina++;carregar();}});
el("recarregar").addEventListener("click",carregar);
el("cancelar-exclusao").addEventListener("click",()=>el("exclusao").close());
el("exclusao").addEventListener("close",()=>{selecionada=null;if(abridorExclusao?.isConnected)abridorExclusao.focus();abridorExclusao=null;});
el("excluir").addEventListener("click",async()=>{
    if(ocupado||!selecionada)return;
    const id=selecionada.id;el("exclusao").close();estado(true);let excluida=false;
    try {await api.requisitar(`/decisoes/${id}?confirmar=true`,"DELETE");excluida=true;await consultar();el("mensagem").textContent="Decisão excluída.";}
    catch(erro) {resposta=null;el("decisoes").replaceChildren();el("pagina").textContent="";el("recarregar").hidden=false;el("mensagem").textContent=excluida?"Decisão excluída. Recarregue para atualizar a lista.":erro.message+" Recarregue o histórico para conferir antes de tentar novamente.";}
    finally {estado(false);}
});
window.addEventListener("pageshow",evento=>{if(evento.persisted)location.reload();});
carregar();
