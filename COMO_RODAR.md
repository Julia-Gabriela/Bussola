# Como rodar o Bússola

## Para ligar no dia a dia

Se você já configurou o projeto neste computador, faça só estes passos:

1. **MySQL:** precisa estar ligado. No PowerShell, `Get-Service *mysql*` deve mostrar `Running`. Se estiver parado, inicie o serviço pelo aplicativo Serviços do Windows.
2. **Backend no IntelliJ:** abra o projeto, selecione **BussolaApplication** no topo e clique em **▶**. Após atualizar o código, sincronize **pom.xml → Maven → Sync Project** e reinicie a aplicação. Aguarde **Started BussolaApplication** e a porta **8080**. Deixe rodando.
3. **Frontend:** abra um PowerShell na pasta raiz do projeto e execute:

```powershell
.\.\rodar-frontend.ps1
```

4. Deixe esse terminal aberto e acesse **http://127.0.0.1:8765/pages/login.html**. Entre com um usuário cadastrado no Bússola, não com a senha do MySQL.

Na máquina da Júlia, para entrar na pasta raiz:

```powershell
cd "D:\Faculdade\7_semestre\Solucoes_Computacionais\Bussola"
```

Se o PowerShell bloquear o script, use este comando na raiz:

```powershell
python -m http.server 8765 --directory frontend
```

**Para parar:** botão ■ no IntelliJ e **Ctrl + C** no terminal do frontend. Fechar a aba do navegador não desliga os servidores.

## Links úteis

Após atualizar o frontend, pressione **Ctrl + F5** no navegador. Na Home, **Continuar** abre diretamente a tela disponível correspondente à etapa salva. Ao revisar Brainstorming, Opções ou Critérios pelo indicador superior, use **Próxima etapa** para seguir: salve ou cancele o rascunho de ideia; em Opções e Critérios, o botão salva as alterações antes de navegar, sem reiniciar o progresso. Notas pendentes ainda precisam de confirmação. A tela de Pesos continua aguardando implementação.

Para conferir o layout, teste uma janela de computador e uma tela estreita. Os campos e espaços estão mais compactos; listas longas e telas de celular mantêm rolagem para não cortar conteúdo.

| O que abrir | Endereço |
| --- | --- |
| Login | http://127.0.0.1:8765/pages/login.html |
| Cadastro | http://127.0.0.1:8765/pages/cadastro.html |
| Home autenticada | http://127.0.0.1:8765/pages/home.html |
| Histórico | http://127.0.0.1:8765/pages/historico.html |
| Nova decisão | http://127.0.0.1:8765/pages/nova-decisao.html |
| Brainstorming | `http://127.0.0.1:8765/pages/brainstorming.html?decisaoId=ID_DA_DECISAO` (após concluir a contextualização) |
| Avaliação por critérios | `http://127.0.0.1:8765/pages/criterios.html?decisaoId=ID_DA_DECISAO` (após concluir Opções) |
| Definição das opções | `http://127.0.0.1:8765/pages/opcoes.html?decisaoId=ID_DA_DECISAO` (use uma decisão sua na etapa 3 ou posterior) |
| Sobre o projeto | http://127.0.0.1:8765/pages/sobre.html |
| Swagger: testar a API | http://localhost:8080/swagger-ui/index.html |

Para testar pelo **Swagger**, basta MySQL e backend. Para usar login, cadastro e Home pelas **telas**, ligue também o frontend. Abrir o HTML por `file:///` não substitui o servidor da porta 8765.

## Primeira configuração neste computador

Só precisa fazer esta parte se o projeto ainda não estiver configurado.

1. Instale **JDK 25**, **MySQL 8**, **Python 3** e Git. Confira `java -version` e `python --version` no PowerShell.
2. Em uma ferramenta conectada ao MySQL, como o Workbench, crie o banco se ele ainda não existir:

```sql
CREATE DATABASE bussola CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

SQL deve ser executado no Workbench, não no PowerShell. As tabelas são criadas pelas migrations do Flyway ao iniciar o backend. Não altere migrations já aplicadas nem recrie um banco existente.

3. Gere a chave JWT uma vez, em um PowerShell normal. Este bloco salva a chave no usuário do Windows sem exibi-la:

```powershell
$env:JWT_SECRET = [Environment]::GetEnvironmentVariable("JWT_SECRET", "User")
if (-not $env:JWT_SECRET) {
    $bytesJwt = New-Object byte[] 32
    $geradorJwt = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $geradorJwt.GetBytes($bytesJwt)
    $geradorJwt.Dispose()
    $env:JWT_SECRET = [Convert]::ToBase64String($bytesJwt)
    [Environment]::SetEnvironmentVariable("JWT_SECRET", $env:JWT_SECRET, "User")
}
"JWT_SECRET configurada"
```

4. Feche completamente e abra novamente o IntelliJ para reconhecer a variável. Configure o projeto e o Maven com **JDK 25**.
5. Em **Run → Edit Configurations**, selecione ou crie uma configuração **Application**:
   - Nome: `BussolaApplication`.
   - Main class: `br.com.bussola.BussolaApplication`.
   - Módulo: `bussola`; JDK: **25**.
   - **Environment variables:** adicione `DB_PASSWORD` com a senha local do MySQL.
   - Mantenha **Include system environment variables** habilitado e **Store as project file** desmarcado.

Os padrões são banco `bussola` em `localhost:3306` e usuário `root`. Se forem diferentes, configure também `DB_URL` e `DB_USERNAME`. Não coloque senhas ou tokens no código, Git ou prints. A chave JWT não é a senha do banco; trocá-la invalida os tokens anteriores.

Depois, siga **Para ligar no dia a dia**, no início deste arquivo.

## Testar nova decisão no Swagger

1. Inicie o backend atualizado e abra o Swagger.
2. Se não tiver usuário, execute **POST /auth/cadastro → Try it out → Execute**. Espere **201**.
3. Execute **POST /auth/login** com o e-mail e a senha cadastrados. Espere **200**.
4. Copie o valor de `accessToken`, clique em **Authorize** e cole sem aspas e sem escrever `Bearer` antes.
5. Abra **POST /decisoes → Try it out** e envie:

```json
{
  "titulo": "Devo mudar de emprego?",
  "contexto": "Recebi uma proposta em outra cidade."
}
```

6. Espere **201** com um `id`, status `EM_ANDAMENTO` e `etapaAtual: 1`. O título é obrigatório (até 200 caracteres). O contexto é opcional (até 16.000 caracteres).
7. Use esse `id` em **GET /decisoes/{id}**: deve retornar **200**. Uma decisão inexistente ou de outro usuário retorna **404**. Sem autenticação, as rotas retornam **401**.
8. Execute **GET /home** ou atualize a Home na aba em que fez login para ver o resumo atualizado.

O dono da decisão vem do token; não envie `usuarioId`, status ou etapa. A Home já consulta as decisões do próprio usuário. O cálculo de progresso da Home continua com seu contrato existente de 6 etapas; criar uma decisão não conclui a contextualização nem libera etapas futuras.

## Confirmar o avanço entre etapas

Nas telas de Contextualização, Brainstorming, Opções e Critérios, **Salvar e continuar** ou **Próxima etapa** abre a pergunta **Concluir esta etapa?**, depois da validação dos campos. A mensagem informa que a etapa será concluída e que, enquanto a decisão estiver em andamento, ela poderá ser revisada depois, com uma confirmação.

Para testar no navegador, mantenha **MySQL + backend + frontend** ligados e use uma decisão de teste:

1. Preencha os campos obrigatórios e clique para avançar. Antes de confirmar, nenhum salvamento ou avanço é enviado por esse clique.
2. Clique em **Continuar revisando** ou pressione **Esc**. A tela deve manter os dados digitados, a etapa atual e o foco no botão que abriu a confirmação.
3. Abra novamente e clique em **Confirmar e avançar**. Só então a tela envia a conclusão ao backend e segue o fluxo. Em Critérios, Pesos continua pendente e aparece o aviso de avaliações salvas.
4. **Salvar para depois** continua salvando sem abrir essa confirmação e sem encerrar a etapa. No Brainstorming, salvar ideias também não conclui a etapa.

Essa confirmação pertence à interface. No Swagger, as rotas de conclusão continuam avançando diretamente quando a requisição é válida. Não houve mudança nos comandos de inicialização; após atualizar, reinicie o backend e use **Ctrl + F5** nas páginas.

## Revisar uma etapa anterior

Enquanto a decisão está **em andamento**, uma etapa já concluída do indicador (Brainstorm, Opções ou Critérios) pode ser reaberta. Pesos e Análise ainda não têm página: aparecem no indicador, mas não abrem tela. A contextualização continua editável só na etapa 1. Uma decisão com status **concluída** permanece só para consulta.

Mantenha MySQL, backend e frontend ligados. Use uma decisão sua que já tenha passado da etapa que você quer revisar.

1. Na etapa atual, clique no nome de uma etapa anterior no indicador. A página não muda. Abre o aviso **Revisar esta etapa?**, com o texto de que as respostas podem ser alteradas e de que informações e resultados seguintes poderão ser atualizados.
2. Clique em **Cancelar** ou pressione **Esc**. O aviso fecha, o endereço continua o mesmo e a decisão não é gravada de novo. O foco volta ao passo clicado.
3. Abra de novo e clique em **Continuar revisão**. A etapa escolhida abre com `revisao=1` na URL e mostra o aviso **Suas alterações podem atualizar o resultado desta análise.** No indicador, essa página fica em destaque. A etapa já salva no backend continua marcada como alcançada, com o círculo só contornado, sem o mesmo preenchimento da página aberta. O botão de concluir a etapa fica oculto. Em Opções e Critérios, o salvamento aparece como **Salvar alterações**. Em Critérios, se a decisão já chegou em Pesos, **Continuar para pesos** fica à esquerda e **Salvar alterações** à direita, na mesma linha.
4. Altere uma resposta e salve. Recarregue a página: a alteração continua, os identificadores das opções, critérios ou ideias já existentes continuam os mesmos e `etapaAtual` não volta nem avança. Os dados das etapas seguintes não são apagados.
5. No indicador, o passo da etapa atual (quando essa etapa tem página) volta para ela sem o aviso de revisão. Pesos e Análise, e qualquer etapa ainda não alcançada, não são clicáveis. Abrir Critérios ou Opções direto na URL antes da hora continua respondendo **409** e a tela não libera a edição.
6. Sem `revisao=1`, uma etapa já passada abre só para consulta, sem o aviso de revisão. Se a decisão já passou de Critérios e a tela aberta é a própria avaliação, clique de novo em **Critérios** no indicador: o popup **Revisar esta etapa?** aparece e **Continuar revisão** acrescenta `revisao=1`. A decisão de outra conta responde **404**.

## Sair sem perder o que foi digitado

Nas telas de Contextualização, Brainstorming, Opções e Critérios, a página compara os campos com o último dado carregado ou salvo. Digitar e depois apagar até voltar ao mesmo texto libera a saída. Carregar a página não conta como alteração.

Se houver diferença e você clicar no indicador, em Início, em Sair ou em outro link interno, abre o aviso **Alterações não salvas**, com o botão **Continuar editando**. Esc fecha o aviso e mantém o que foi digitado. Não há botão para descartar. **Salvar para depois** ou **Salvar alterações** grava e, se der certo, libera a navegação. Se o salvamento falhar, o texto permanece e a saída continua bloqueada.

No Brainstorming, cada ideia adicionada, editada ou excluída já é gravada na hora. O bloqueio vale para o texto ainda no formulário e para uma exclusão que ainda não foi confirmada. Em Critérios, incluir ou remover um critério, ou mudar uma nota, bloqueia a saída até salvar. Confirmar notas pendentes não grava essas mudanças: a tela pede para salvar antes.

Atualizar ou fechar a aba usa o aviso do próprio navegador. O botão Voltar também é interceptado quando a página consegue. O navegador pode ignorar essa proteção em alguns fechamentos.

Abrir ou cancelar o aviso não grava nada. Não existe, por enquanto, recálculo automático de pesos ou do resultado da análise quando uma resposta anterior muda. Quando o nome de uma alternativa já avaliada muda, as notas ficam pendentes de confirmação, como explica a seção seguinte.

## Confirmar avaliações pendentes

Mudar ideias no brainstorming não cria pendência e não apaga opções, critérios nem notas. A pendência aparece só quando o **nome** de uma alternativa que já tem nota é alterado de verdade. Espaços nas pontas do nome não contam. A e B são independentes: alterar só A deixa B como estava.

Mudar apenas a descrição não cria pendência obrigatória. Depois de salvar, a tela de Opções pode mostrar a recomendação **A descrição de … foi alterada. Se quiser, confira as notas dessa alternativa. Essa revisão não é obrigatória.** Essa frase não fica gravada: ao recarregar a página, ela some. Se o nome e a descrição mudam juntos, vale a regra do nome.

A pendência fica na coluna `avaliacao_pendente` de cada opção, no MySQL. Continua depois de atualizar a página, sair, entrar de novo ou reiniciar o backend. A decisão também guarda `versao_avaliacao`, usada para impedir que uma tela antiga confirme notas que já mudaram.

Para testar, mantenha MySQL, backend e frontend ligados. Use uma decisão sua que já tenha notas nas duas alternativas e ainda esteja em andamento. Depois de atualizar o projeto, reinicie o backend para aplicar a nova migration e use **Ctrl + F5**.

1. Revise Opções e troque o nome só de A. Salve. Volte a Critérios. O aviso **Revise sua avaliação** deve citar a alternativa A e manter as notas visíveis. B não entra no aviso.
2. Clique em **Salvar para depois** ou **Salvar alterações** sem mudar as notas. A pendência continua.
3. Clique em **Confirmar notas de …** da alternativa A. A pendência de A some. Se B também estiver pendente, o aviso de B permanece.
4. Confirme sem alterar nenhuma nota: isso é permitido. A confirmação é o clique no botão, não o ato de abrir a página.
5. Troque o nome de A de novo e salve. A pendência de A volta.
6. Troque só a descrição, salve e recarregue Opções: a recomendação some e Critérios não exige confirmação.
7. Com pendência aberta, **Próxima etapa** fica oculta. O backend também recusa concluir Critérios enquanto A ou B estiver pendente (**409**). `etapaAtual` não muda.
8. Se a mensagem pedir para recarregar antes de confirmar, use **Recarregar dados**. Uma confirmação enviada com a versão antiga não apaga a pendência.

Enquanto existir pendência, um resultado futuro não deve ser tratado como válido. Se já houver uma linha em `resultados` para aquela alternativa, ela permanece e fica marcada para atualização. Pesos e Análise ainda não têm tela nem cálculo.

## Testar contextualização e brainstorming

A integração agora inclui a página de Brainstorming, o cliente `decisao-api.js`, as rotas de contexto/ideias e `FluxoDecisaoControllerTest`. O fluxo disponível é **Home → Contextualização → Brainstorming → Opções → Critérios**. Pesos e Análise permanecem pendentes.

Depois de atualizar o projeto, **reinicie o backend no IntelliJ** e atualize o navegador com **Ctrl + F5**. Não precisa criar tabelas nem mudar variáveis ou portas.

1. Faça login e clique em **Iniciar decisão** na Home.
2. Informe o título e, se quiser, o contexto. **Salvar para depois** mantém a decisão na etapa 1.
3. Reabra pela Home, altere o contexto e clique em **Salvar e continuar** e depois em **Confirmar e avançar**. Deve abrir o brainstorming (etapa 2 de 6).
4. Selecione **Favorável**, **Desfavorável** ou **Dúvida**, escreva uma ideia e clique em **Adicionar**.
5. Use **Editar** para mudar texto ou categoria. **Excluir** pede confirmação. Cada ação é salva no banco.
6. Atualize a página ou retorne pela Home: as ideias devem continuar lá. Tanto o cartão da decisão quanto **Continuar** retomam o fluxo salvo.
7. Clique em **Próxima etapa** depois de salvar ou cancelar o texto em edição e escolha **Confirmar e avançar**. A decisão passa para **Opções (etapa 3)**, preservando as ideias e o status **Em andamento**.

A tela de Opções já está implementada em `opcoes.html`. A conclusão do brainstorming abre `opcoes.html?decisaoId=ID_DA_DECISAO` após salvar `etapaAtual: 3`. Sem pedir revisão, as etapas anteriores ficam para consulta. A revisão com confirmação está descrita em **Revisar uma etapa anterior**. Não há quantidade mínima de ideias exigida pela documentação. Texto de ideia é obrigatório ao adicionar/editar; limite técnico: 16.000 caracteres, compatível com a coluna TEXT.

Se o envio falhar sem confirmação, recarregue os dados antes de reenviar. No brainstorming, use **Recarregar dados**. Na criação, confira a Home para evitar duplicação. O que ainda não foi salvo no formulário não é recuperado ao sair.

### Rotas para testar no Swagger

Use o ID de uma decisão sua e autorize com o token do login:

| Operação | Rota | Resultado |
| --- | --- | --- |
| Salvar título/contexto | PUT /decisoes/{id}/contexto | 200; somente etapa 1; corpo igual ao da criação |
| Avançar ao brainstorming | POST /decisoes/{id}/contexto/concluir | 200; etapa 2 |
| Consultar ideias | GET /decisoes/{id}/ideias | 200; lista ordenada por ID |
| Adicionar ideia | POST /decisoes/{id}/ideias | 201 |
| Editar texto/categoria | PUT /decisoes/{id}/ideias/{ideiaId} | 200 |
| Excluir ideia | DELETE /decisoes/{id}/ideias/{ideiaId} | 204 |
| Concluir brainstorming | POST /decisoes/{id}/brainstorming/concluir | 200; etapa 3, ainda Em andamento |

Corpo de adicionar/editar ideia:

```json
{"conteudo": "Ganhar experiência profissional", "categoria": "FAVORAVEL"}
```

Categorias aceitas: `FAVORAVEL`, `DESFAVORAVEL`, `DUVIDA`. Sem login: **401**. Dados inválidos: **400**. Decisão de outra pessoa ou ideia fora da decisão informada: **404**. Incluir, editar ou excluir ideia em uma decisão em andamento que já passou do brainstorming responde **201**, **200** ou **204** e não altera `etapaAtual`. A contextualização e a conclusão do brainstorming fora da etapa exata, o acesso antes do brainstorming e qualquer alteração em decisão concluída respondem **409**. Repetir a conclusão quando a decisão já está na etapa 3 não avança uma segunda vez.



O login expira após **60 minutos sem requisições autenticadas**. Quando receber 401 por expiração, faça login de novo. O botão **Sair** da Home chama `POST /auth/logout` e remove a sessão local; fechar Authorize no Swagger apenas remove o token da interface.

## Testar a definição das opções

A tela **Defina suas opções** está em `frontend/pages/opcoes.html`. Ela exibe o nome da decisão apenas para consulta, permite salvar duas alternativas (A e B) com descrições e avançar para Critérios. O nome é definido na contextualização e não pode ser alterado em Opções, nem pela API. Os dados pertencem exclusivamente à conta autenticada.

**A forma de ligar o projeto continua a mesma.** Reinicie o backend com os comandos/configurações que você já usa e aguarde **Started BussolaApplication**. Mantenha MySQL, backend e frontend ligados para testar o salvamento. Atualize a página com **Ctrl + F5**. Não há nova dependência, variável, porta ou migration; a API usa as tabelas `decisoes` e `opcoes` existentes.

### Pré-requisito: decisão na etapa de opções

Opções corresponde a `etapa_atual = 3` no banco, depois da contextualização (1) e do brainstorming (2). O indicador do Figma mostra **Etapa 2 de 5** porque conta as cinco etapas da análise, sem a contextualização. A Home continua contando o fluxo completo de seis etapas.

O backend bloqueia o acesso antecipado com **409**. Criar uma decisão pela Home ainda a deixa na etapa 1; mudar o número na URL não libera etapas. Para chegar à etapa 3 pela interface, conclua a contextualização e o brainstorming conforme a seção anterior.

**Opcional: testar Opções isoladamente com uma decisão fictícia:**

1. Cadastre uma **conta de teste** pelo frontend e faça login nela.
2. Abra o MySQL Workbench e conecte ao mesmo banco local usado pelo backend. Se seu MySQL está na porta **3307**, use essa porta na conexão; o padrão do guia é 3306.
3. No Workbench, abra **File → Open SQL Script** e selecione `backend/src/test/resources/opcoes-desenvolvimento.sql` dentro do projeto.
4. No editor do Workbench, substitua `SEU_EMAIL_DE_TESTE` pelo e-mail da conta de teste. Não precisa colocar senha no arquivo; execute o SQL completo pelo botão de raio.
5. A consulta final mostra `decisao_id` e uma URL. Se aparecer “Nenhuma decisão criada”, confira o e-mail e se a conta foi cadastrada nesse banco.
6. Abra a URL retornada **na mesma aba em que fez login**, ou atualize a Home e clique em **Continuar** na decisão com prefixo `[TESTE]`. A sessão fica no armazenamento da aba; se abrir outra, faça login nela também.

Esse SQL é uma fixture manual para desenvolvimento: cria somente uma nova decisão fictícia na etapa 3 e não modifica decisões existentes. Cada execução válida cria outra decisão de teste. **Não é migration, não é executado automaticamente e não substitui a conclusão do brainstorming no fluxo normal.** O servidor continua exigindo a etapa correta. Não salve o e-mail pessoal alterado no Git.

### Teste funcional pela tela

1. Abra a decisão de teste. O título deve vir da API e aparecer como texto, sem campo de edição. Os campos A e B ficam disponíveis.
2. Tente avançar sem preencher os nomes: a tela deve indicar os campos obrigatórios sem enviar a requisição.
3. Preencha nomes diferentes para A e B e, opcionalmente, as descrições. Os nomes das alternativas aceitam até **200 caracteres** cada; as descrições aceitam até **16.000 caracteres** cada.
4. Clique em **Salvar para depois**. Aguarde a confirmação. Recarregue a página: os valores devem continuar preenchidos e a decisão deve permanecer na etapa 3. Você pode editar e salvar novamente sem criar outro par de opções.
5. Clique em **Próxima etapa** e teste **Continuar revisando**: os valores devem permanecer na tela sem avançar. Abra novamente e escolha **Confirmar e avançar**. O backend salva os dados e avança para a etapa 4 na mesma transação, mantendo o status **EM_ANDAMENTO**. O título original da decisão é preservado.
6. A página `criterios.html?decisaoId=ID` abre com as duas opções salvas. Ao retomar pela Home, a decisão na etapa 4 ou posterior abre Critérios; as opções anteriores continuam disponíveis para consulta.
7. Para alterar as opções depois do avanço, use o indicador conforme **Revisar uma etapa anterior**. Sem essa confirmação, a tela fica para consulta. Uma decisão concluída fica somente para consulta.

Se uma falha de rede impedir a confirmação do salvamento, a tela preserva os valores digitados e bloqueia novos envios. Use **Recarregar dados** para consultar o que o servidor salvou; a tela pede confirmação antes de substituir o formulário. Alterações ainda não salvas não são recuperadas depois de fechar a aba. A página avisa ao tentar sair com alterações pendentes.

**Teste visual:** iniciar apenas o frontend permite conferir a estrutura responsiva. Para ver os campos preenchidos com dados reais, validar a sessão, salvar ou avançar, também são necessários backend e MySQL. Sem sessão, a página encaminha ao login; sem ID, orienta abrir uma decisão pela Home.

### Teste da API pelo Swagger

Com MySQL e backend ligados, abra **http://localhost:8080/swagger-ui/index.html**, faça login em **POST /auth/login** e informe o `accessToken` em **Authorize**, conforme as instruções anteriores. Use o ID da decisão de teste.

| Operação | Rota | Resultado esperado |
| --- | --- | --- |
| Carregar | GET /decisoes/{decisaoId}/opcoes | 200; título, etapa, status e opções; A/B são null quando ainda não foram salvas |
| Salvar sem avançar | PUT /decisoes/{decisaoId}/opcoes | 200; na etapa 3 mantém a etapa e os IDs; numa etapa posterior em andamento também salva sem mudar `etapaAtual` nem duplicar opções |
| Salvar e avançar | POST /decisoes/{decisaoId}/opcoes/concluir | 200; etapa 4, ainda EM_ANDAMENTO |

Use este corpo no PUT e no POST, copiando em `titulo` o nome exato retornado pelo GET. Esse campo permanece no contrato da API para compatibilidade; não permite renomear a decisão. Enviar outro título retorna **409**, sem salvar as opções nem avançar:

```json
{
  "titulo": "Aceitar estágio ou focar na faculdade?",
  "opcaoA": {
    "nome": "Aceitar o estágio",
    "descricao": "Ganhar experiência e conciliar os horários."
  },
  "opcaoB": {
    "nome": "Focar na faculdade",
    "descricao": "Dedicar mais tempo às disciplinas e projetos."
  }
}
```

A resposta contém `decisaoId`, `titulo`, `etapaAtual`, `status`, `opcaoA` e `opcaoB`. Cada opção salva tem `id`, `nome` e `descricao`. Descrição vazia é armazenada como null. Para manter A/B estáveis, A corresponde ao menor ID e B ao próximo ID daquela decisão; as edições preservam esses IDs.

Sem autenticação: **401**. Decisão inexistente ou de outra conta: **404**. Campos obrigatórios ausentes, limites excedidos ou nomes iguais (desconsiderando caixa e espaços nas pontas): **400**. Salvar ou concluir antes da etapa 3, concluir com dados diferentes depois do avanço, ou alterar uma decisão concluída: **409**. O PUT de revisão, com a decisão ainda em andamento e já na etapa 4 ou posterior, salva sem avançar. Reenviar a mesma conclusão na etapa 4 retorna os dados salvos sem duplicar opções nem avançar novamente. Se a alternativa já tiver nota e o nome mudar, a resposta traz `avaliacaoPendenteA` ou `avaliacaoPendenteB` como verdadeiro e incrementa `versaoAvaliacao`. Reenviar o mesmo nome não liga a pendência. Mudar só a descrição pode trazer `recomendacaoRevisarA` ou `recomendacaoRevisarB` nessa resposta; o GET seguinte volta esses dois campos como falso.

**Integração entre as etapas:** `frontend/js/pages/opcoes-integracoes.js` já aponta para `criterios.html`; a navegação acrescenta `decisaoId`. O evento cancelável `bussola:opcoes-concluidas` continua disponível. Brainstorming abre `opcoes.html?decisaoId=ID` depois de concluir sua etapa. A retomada da Home passa por `nova-decisao.html`, consulta o estado real da decisão e encaminha a etapa 3 para Opções e as etapas 4 ou posteriores para Critérios. Pesos ainda depende da integração descrita abaixo.

### Testes automatizados de Opções

No PowerShell, dentro de `backend`, execute:

```powershell
.\mvnw.cmd "-Dtest=OpcoesServiceTest,OpcoesControllerTest" test
```

Espere **BUILD SUCCESS**. Os testes não usam MySQL: verificam regras de etapa, titularidade, preservação dos IDs, reenvio da conclusão, campos obrigatórios, limites, autenticação e CORS com repositórios simulados. Eles não substituem o teste funcional de persistência no MySQL descrito acima.

## Testar a avaliação por critérios

A página está em `frontend/pages/criterios.html`. **Continue usando os mesmos comandos e terminais para ligar o projeto.** Não há nova porta, variável, dependência ou migration. Reinicie o backend atualizado, espere **Started BussolaApplication**, mantenha MySQL e frontend ligados e atualize o navegador com **Ctrl + F5**.

### Abrir e testar pela tela

1. Faça login. Abra uma decisão que já passou por Opções e clique em **Próxima etapa**. A URL será `http://localhost:8765/pages/criterios.html?decisaoId=ID_DA_DECISAO`, com o número real da sua decisão.
2. Para testar o fluxo completo, crie uma decisão pela Home, clique em **Salvar e continuar**, registre ideias no Brainstorming e avance para Opções. Preencha A/B e conclua Opções. A fixture `opcoes-desenvolvimento.sql` continua sendo uma alternativa opcional para testes isolados; não é necessária para percorrer as telas.
3. Confira título e nomes das alternativas. A tela tem cinco critérios fixos e começa sem notas inventadas: **Segurança, Crescimento, Risco, Impacto emocional e Alinhamento a valores**.
4. Selecione notas inteiras de **1 a 5**, usando os botões ou as setas do teclado dentro de cada grupo. Nos critérios favoráveis, 5 é mais favorável. **Em Risco, 1 significa menor risco e 5 significa maior risco.** Essa nota é salva como escolhida, sem inversão no banco.
5. Preencha apenas algumas notas e clique em **Salvar para depois**. Espere a confirmação, recarregue e confira que as notas continuam lá. A decisão permanece em `etapaAtual: 4`.
6. Use **Adicionar critério** para um aspecto pessoal. Nome obrigatório (até 200 caracteres), descrição opcional (até 1.000). Os nomes não podem se repetir; o limite é de **20 critérios no total** (5 fixos + até 15 personalizados). Formule o personalizado de modo que 5 seja mais favorável. Ele começa com peso 1, como os fixos. Adicionar e remover só são persistidos quando você salva ou avança. Remover pede confirmação e exclui também as notas daquele personalizado ao salvar. Os cinco fixos não podem ser removidos.
7. Tente **Próxima etapa** com uma nota faltando: a tela deve focar o primeiro grupo incompleto e continuar na etapa atual. Depois, preencha A/B em todos os critérios, clique para avançar e escolha **Confirmar e avançar**. O backend salva e passa de **4 para 5**, mantendo a decisão **EM_ANDAMENTO**.
8. Como **Pesos ainda não está implementada**, aparece a confirmação de que a avaliação foi salva. Nenhuma página inexistente é aberta. Ao retomar pela Home, Critérios abre para consulta. Para editar as notas de novo, siga **Revisar uma etapa anterior**: **Salvar alterações** grava sem voltar a etapa nem apagar as notas das outras etapas. Se o aviso **Revise sua avaliação** estiver visível, confirme cada alternativa citada antes de tentar avançar, como em **Confirmar avaliações pendentes**.

O indicador **Etapa 3 de 5** segue o Figma e conta as etapas da análise; o banco/Home contam também a contextualização, por isso Critérios é a etapa 4 de 6.

**Totais:** são somas brutas sem pesos, com indicação de parcial quando há notas faltantes. Não são uma recomendação: incluem a nota de Risco no sentido escolhido pelo usuário. A futura análise deve interpretar `sentido: MENOR_MELHOR` antes de comparar alternativas. Os pesos existentes não são redefinidos ao salvar notas.

Se a conexão falhar sem confirmar a gravação, o formulário preserva as notas e bloqueia reenvios. Use **Recarregar dados** para conferir o que foi salvo; haverá confirmação antes de substituir alterações locais. A página avisa ao sair com alterações não salvas, mas não as recupera depois de fechar a aba.

**Teste visual e funcional:** só o frontend permite verificar o cabeçalho e os estados sem dados. A tabela é montada com os critérios e as opções retornados pela API; para ver dados reais, validar a sessão, salvar e avançar, mantenha **MySQL + backend + frontend** ligados. Sem sessão, a tela encaminha ao login; sem ID válido, orienta abrir a decisão pela Home. Uma decisão de outra conta não pode ser consultada.

### Testar Critérios no Swagger

No navegador, abra **http://localhost:8080/swagger-ui/index.html**, faça login e informe o token em **Authorize** como explicado anteriormente. Use o ID de uma decisão sua que esteja na etapa 4 (conclua Opções antes).

| Operação | Rota | Resultado esperado |
| --- | --- | --- |
| Consultar | GET /decisoes/{decisaoId}/criterios | 200; opções A/B, critérios, notas e sentido da escala; não grava nada |
| Salvar parcial | PUT /decisoes/{decisaoId}/criterios | 200; mantém etapa 4; notas não preenchidas são null |
| Concluir | POST /decisoes/{decisaoId}/criterios/concluir | 200; todas as notas preenchidas, etapa 5, EM_ANDAMENTO |

Exemplo de corpo completo para PUT e POST:

```json
{
  "criterios": [
    { "nome": "Segurança", "notaA": 3, "notaB": 4 },
    { "nome": "Crescimento", "notaA": 5, "notaB": 3 },
    { "nome": "Risco", "notaA": 3, "notaB": 2 },
    { "nome": "Impacto emocional", "notaA": 2, "notaB": 4 },
    { "nome": "Alinhamento a valores", "notaA": 4, "notaB": 4 }
  ]
}
```

No PUT, pode trocar uma nota por `null` para testar o salvamento parcial. Envie sempre a **lista completa**: omitir um personalizado o remove com suas notas; omitir um fixo é inválido. Para adicionar, inclua outro objeto com `nome`, `descricao` opcional, `notaA` e `notaB`. Os nomes identificam os critérios dentro da decisão; reenvios preservam IDs. A interface não oferece renomeação de critérios.

Sem autenticação: **401**. Decisão inexistente/de outra conta: **404**. Nota fora de 1–5, fracionária ou em texto, nome repetido, fixo ausente ou conclusão incompleta: **400**. Salvar ou concluir antes da etapa 4, concluir com dados diferentes depois do avanço, concluir com avaliação pendente, confirmar com `versaoAvaliacao` antiga, ou alterar uma decisão concluída: **409**. O PUT de revisão, com a decisão em andamento e já na etapa 5 ou posterior, salva sem mudar `etapaAtual` e sem confirmar a pendência. Repetir a mesma conclusão na etapa 5 retorna os dados salvos sem avançar novamente. Para evidências, capture a rota, o código de resposta e o corpo, ocultando o token do trecho Curl.

Confirmar uma alternativa: `POST /decisoes/{decisaoId}/criterios/confirmacao`.

```json
{"alternativa": "A", "versaoAvaliacao": 1}
```

`alternativa` aceita somente `A` ou `B`. Use o `versaoAvaliacao` do último GET ou salvamento. A resposta **200** desliga só a pendência indicada, preserva notas, pesos, IDs e `etapaAtual`. Salvar com PUT não faz isso.

### Integração com as próximas telas

- Brainstorming está integrada: sua conclusão persiste a etapa 3 e abre `opcoes.html?decisaoId=ID`. Ao consultar ideias de uma etapa já concluída, **Continuar decisão** retoma o estágio salvo pela API.
- Opções já abre Critérios. Os IDs de A/B são preservados: A é a opção de menor ID, B é a seguinte.
- Quando Pesos existir, configure `pesos` em `frontend/js/pages/criterios-integracoes.js`. A navegação acrescenta `decisaoId`. Outra integração pode interceptar o evento cancelável `bussola:criterios-concluidos`, com `detail: { decisaoId, etapa: 5 }`.
- A API retorna `criterios[].id`, `codigo`, `nome`, `descricao`, `personalizado`, `peso`, `sentido`, `notaA` e `notaB`. IDs são null somente para os cinco critérios ainda não gravados; depois de salvar, são os IDs reais para Pesos/Avaliações. O GET não cria registros.
- `sentido` é `MENOR_MELHOR` em Risco e `MAIOR_MELHOR` nos demais. Na futura comparação em que uma pontuação maior significa preferência, normalize Risco como `6 - nota` antes de aplicar seu peso. O total bruto desta tela não deve ser reutilizado como resultado final.
- Enquanto Pesos não existe, a retomada da Home permanece em Critérios para consulta a partir da etapa 5. Ao entregar Pesos/Análise, atualize também o encaminhamento em `nova-decisao.js` para o estágio real do backend.

### Testes automatizados de Critérios

Em um PowerShell, **dentro da pasta `backend`**, execute:

```powershell
.\mvnw.cmd "-Dtest=CriteriosServiceTest,CriteriosControllerTest" test
```

Espere **BUILD SUCCESS**. Esses testes usam repositórios simulados, sem MySQL: verificam notas parciais, escala de Risco, validação, titularidade, IDs/pesos preservados, remoção de personalizados, conclusão idempotente, autenticação e CORS. Eles não substituem a conferência da persistência real pelo roteiro da tela e do Swagger acima.

## Testes automatizados

No PowerShell, entre em `backend` a partir da raiz e execute os testes sem banco:

```powershell
cd backend
.\mvnw.cmd "-Dtest=*Test" test
```

Espere **BUILD SUCCESS**. Esse comando seleciona as classes terminadas em `Test` presentes no repositório, incluindo cadastro, nova decisão, definição das opções, avaliação por critérios, autenticação, Home, CORS e Swagger. Elas usam repositórios simulados e não exigem MySQL. A suíte também inclui `FluxoDecisaoControllerTest`, que verifica contextualização, ideias e avanço do brainstorming.

Para rodar também `BussolaApplicationTests` (contexto completo), configure as variáveis do banco e JWT nesse terminal e use `.\mvnw.cmd test`. Esse teste conecta ao MySQL e pode aplicar migrations pendentes.

## Alternativa: backend pelo PowerShell

Use isto se preferir não iniciar pelo IntelliJ. Na raiz, execute:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/bussola"
$env:DB_USERNAME = "root"
$senhaBanco = Read-Host "Senha do MySQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $senhaBanco).Password
$env:JWT_SECRET = [Environment]::GetEnvironmentVariable("JWT_SECRET", "User")
cd backend
.\mvnw.cmd spring-boot:run
```

Faça antes a primeira configuração da chave JWT. Deixe esse terminal aberto. As variáveis colocadas no IntelliJ não passam automaticamente para o PowerShell.

## Se algo não funcionar

| Sintoma | O que conferir |
| --- | --- |
| `Access denied for user` | Usuário/senha do MySQL na configuração usada para iniciar o backend. |
| `Unknown database bussola` | Crie o banco no Workbench; não crie as tabelas manualmente. |
| Erro com `JWT_SECRET` | Faça a configuração da chave acima e reinicie o IntelliJ. |
| Porta 8080 ocupada | Já pode existir um backend rodando. Pare a execução antiga antes de iniciar outra. |
| `Failed to fetch` ou CORS | Backend ligado e tela aberta pela porta 8765, usando localhost ou 127.0.0.1. |
| Site não abre na 8765 | Inicie o frontend e mantenha o terminal aberto. |
| Login fica em `Entrando...` ou o servidor demora | O login aguarda até 15 segundos e libera o botão se não receber resposta. No PowerShell clássico, deixar texto selecionado pode pausar o backend: pressione **Esc** no terminal do backend, mantenha-o aberto e tente novamente. Se persistir, confira os erros no terminal. Após atualizar o JavaScript, use **Ctrl + F5** na página. |
| Erro ao ler `idDaUrl` na nova decisão | Confira se `frontend/js/pages/decisao-api.js` está presente e carregado antes de `nova-decisao.js`/`brainstorming.js`. A integração atual inclui esse arquivo. Atualize os arquivos completos e use **Ctrl + F5**. |
| `401` | Faça login novamente e use o token; no Swagger, clique em Authorize. |
| `409` no cadastro | O e-mail já existe: faça login ou use outro e-mail de teste. |
| `BUILD FAILURE` | Leia o erro; não significa que o backend iniciou. |
| `cannot find symbol` para `EtapaInvalidaException` ou `IdeiaNaoEncontradaException` | Confira se os dois arquivos `.java` estão em `backend/src/main/java/br/com/bussola/exception/`, com o pacote `br.com.bussola.exception`. Esse erro ocorre antes da conexão ao banco; não exige trocar senha ou porta. Após obter a correção, execute os testes ou inicie novamente o backend. |

**Resumo:** MySQL ligado → ▶ BussolaApplication → `.\rodar-frontend.ps1` na raiz → abrir login.

## Prós e Contras e Matriz GUT

Reinicie o backend e use **Ctrl + F5** no frontend. Não há dependências, variáveis ou migrations novas.

**Pré-requisito:** a decisão precisa ter duas alternativas e estar na etapa interna **6 — Análise** (visual 5 de 5). A etapa Pesos ainda não está implementada, então o fluxo normal de uma decisão nova ainda não chega aqui. Não altere o banco pessoal para pular etapas. Os testes automatizados usam decisões simuladas nessa etapa, sem MySQL.

Quando Pesos estiver integrada, sua conclusão deve liberar a etapa 6 e abrir `analise.html?decisaoId=ID`. A Home e o indicador superior já apontam para essa tela quando a API informa a etapa 6.

### Conferir pela tela, com uma decisão já liberada

1. Na Home, clique em **Continuar** na decisão em Análise.
2. Em **Prós e Contras**, escolha a alternativa A ou B. Selecione Pró/Contra, escreva a descrição e salve. Edite um registro e confira que a outra alternativa permanece intacta.
3. Exclua um registro: primeiro cancele a confirmação; depois confirme e confira a lista.
4. Em **Matriz GUT**, cadastre um fator com notas 5, 4 e 3. A prioridade deve ser **60**. Edite para 1, 1 e 1: deve virar **1** e mudar de posição na lista.
5. Recarregue a página para conferir os dados salvos. Um rascunho impede trocar de técnica ou sair até salvar/cancelar. Falhas de envio exigem recarregar antes de tentar novamente, evitando duplicação.
6. Decisões concluídas são somente consultáveis. Salvar técnicas não conclui a decisão. Técnica 10/10/10, Inversão e resultado continuam pendentes da outra dupla.

Prós/Contras pertencem à alternativa; os fatores GUT pertencem à decisão inteira. A prioridade é calculada pelo servidor como G × U × T (1–125), ordenada de forma decrescente, com desempate pelo ID. Não existe conversão dessa prioridade em uma pontuação final das alternativas nesta entrega.

### Conferir no Swagger

Autentique-se e use **Authorize** como explicado acima. Substitua os IDs pelos da sua decisão e alternativas:

| Operação | Rota |
| --- | --- |
| Consultar ambas as alternativas e GUT | `GET /decisoes/{id}/analise` |
| Adicionar pró/contra | `POST /decisoes/{id}/opcoes/{opcaoId}/pros-contras` |
| Editar ou excluir pró/contra | `PUT` ou `DELETE /decisoes/{id}/opcoes/{opcaoId}/pros-contras/{registroId}` |
| Adicionar fator | `POST /decisoes/{id}/gut` |
| Editar ou excluir fator | `PUT` ou `DELETE /decisoes/{id}/gut/{registroId}` |

Corpo de pró/contra (descrição obrigatória, até 300 caracteres; tipo `PRO` ou `CONTRA`):

```json
{"tipo":"PRO","descricao":"Ganhar experiência profissional"}
```

Corpo de fator GUT (descrição obrigatória, até 200 caracteres; três notas inteiras de 1 a 5):

```json
{"descricao":"Prazo para inscrição","gravidade":5,"urgencia":4,"tendencia":3}
```

Criação retorna **201**, edição/consulta **200**, exclusão **204**. Dados inválidos retornam **400**, falta de autenticação **401**, decisão/registro de outra pessoa ou fora do vínculo indicado **404**, etapa anterior ou tentativa de alterar decisão concluída **409**. Alterações sinalizam resultados existentes como desatualizados, sem apagar a pontuação anterior.

### Testes desta entrega

No terminal, dentro de `backend`, execute:

```powershell
.\mvnw.cmd '-Dtest=TecnicasAnaliseControllerTest' test
```

Esse teste exercita controller, serviço e segurança, com repositórios simulados. Não exige senha do MySQL e não grava no seu banco. Para a suíte sem banco, use `'-Dtest=*Test'` no mesmo comando. Os testes não substituem a validação futura do caminho completo após a entrega de Pesos.

## Histórico de decisões

Reinicie o backend e atualize o navegador com **Ctrl + F5**. Na Home, clique em **Histórico** ou **Ver todas**. O cabeçalho das etapas também abre o histórico, preservando a proteção dos rascunhos.

- Pesquise pelo título e combine com Todas, Concluídas ou Em andamento. A busca trata `%` e `_` como texto, não como curingas.
- Ordene por última atualização (mais recente ou mais antiga) ou título. A lista mostra 10 decisões por página; use Anterior/Próxima. Os totais do topo representam sua conta inteira, mesmo com filtros.
- **Continuar decisão** abre a tela correspondente à etapa salva. Na etapa5, Pesos ainda pendente, mantém a consulta de Critérios; na etapa6, abre Análise.
- **Ver análise** de decisões concluídas está preparado para a tela de resultado da Dupla B. Enquanto ela não existir, mostra um aviso, sem inventar um resultado. Configure `resultadoDecisao` em `frontend/js/pages/historico-integracoes.js` quando a tela for entregue; receberá `decisaoId`. Também existe o evento cancelável `bussola:ver-resultado`.
- **Excluir** abre uma confirmação com o título e explica que a operação é irreversível. Cancelar não altera nada. Confirmar apaga a decisão e todos os seus registros relacionados. Após excluir, a Home recalcula seus totais usando os registros restantes.
- Em falha de rede durante exclusão, use Recarregar histórico para conferir o estado antes de tentar novamente.

### Teste manual e Swagger

Faça testes de exclusão somente em decisões de exemplo que você queira descartar. Crie decisões com títulos diferentes; pesquise, filtre, ordene, cancele uma exclusão e só então confirme a exclusão de um exemplo. Confira os totais ao retornar à Home.

`GET /decisoes` aceita `busca`, `status` (`EM_ANDAMENTO` ou `CONCLUIDA`, opcional), `ordem` (`recentes`, `antigas` ou `titulo`), `pagina` (a partir de0) e `tamanho` (1–50; padrão10). Retorna somente as decisões da conta autenticada, com paginação e totais.

`DELETE /decisoes/{id}?confirmar=true` retorna **204** ao excluir. Sem confirmação retorna **400**; sem autenticação, **401**; decisão inexistente/de outra conta, **404**. Não há restauração após a exclusão. As migrations existentes já têm a remoção em cascata; nenhuma migration nova é necessária.

No terminal dentro de `backend`, execute os testes sem banco:

```powershell
.\mvnw.cmd '-Dtest=HistoricoControllerTest' test
```

Para conferir a revisão de etapas existente:

```powershell
.\mvnw.cmd '-Dtest=FluxoDecisaoControllerTest,OpcoesServiceTest,CriteriosServiceTest' test
```

A rodada4 está implementada nas telas disponíveis: confirmação da revisão, preservação de dados/progresso, controle de etapas e confirmação de notas afetadas. A integração com Pesos e o recálculo/retorno a partir do Resultado aguardam essas funcionalidades da Dupla B. A contextualização continua editável somente antes de avançar, conforme implementação atual; o indicador de revisão começa em Brainstorming.
