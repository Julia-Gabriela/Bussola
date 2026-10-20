# Como rodar o Bússola

## Para ligar no dia a dia

Se você já configurou o projeto neste computador, faça só estes passos:

1. **MySQL:** precisa estar ligado. No PowerShell, `Get-Service *mysql*` deve mostrar `Running`. Se estiver parado, inicie o serviço pelo aplicativo Serviços do Windows.
2. **Backend no IntelliJ:** abra o projeto, selecione **BussolaApplication** no topo e clique em **▶**. Após atualizar o código, sincronize **pom.xml → Maven → Sync Project** e reinicie a aplicação. Aguarde **Started BussolaApplication** e a porta **8080**. Deixe rodando. **Sem IntelliJ** (Cursor, VS Code ou só PowerShell): siga "Como executar o Bússola sem IntelliJ IDEA", mais abaixo.
3. **Frontend:** abra um PowerShell na pasta raiz do projeto e execute:

```powershell
.\rodar-frontend.ps1
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

**Para parar:** botão ■ no IntelliJ (ou **Ctrl + C** no terminal do backend, se ele foi iniciado pelo PowerShell) e **Ctrl + C** no terminal do frontend. Fechar a aba do navegador não desliga os servidores.

## Links úteis

Após atualizar o frontend, pressione Ctrl + F5 no navegador. Na Home, Continuar abre a tela correspondente à etapa salva. Ao revisar Brainstorming, Opções, Critérios ou Pesos pelo indicador superior, use o botão de avanço; a revisão salva alterações sem reiniciar o progresso. Notas pendentes ainda precisam de confirmação.

Para conferir o layout, teste uma janela de computador e uma tela estreita. Os campos e espaços estão mais compactos; listas longas e telas de celular mantêm rolagem para não cortar conteúdo.

| O que abrir | Endereço |
| --- | --- |
| Login | http://127.0.0.1:8765/pages/login.html |
| Cadastro | http://127.0.0.1:8765/pages/cadastro.html |
| Home autenticada | http://127.0.0.1:8765/pages/home.html |
| Meu perfil | http://127.0.0.1:8765/pages/perfil.html (após entrar na conta) |
| Histórico | http://127.0.0.1:8765/pages/historico.html |
| Nova decisão | http://127.0.0.1:8765/pages/nova-decisao.html |
| Brainstorming | `http://127.0.0.1:8765/pages/brainstorming.html?decisaoId=ID_DA_DECISAO` (após concluir a contextualização) |
| Avaliação por critérios | `http://127.0.0.1:8765/pages/criterios.html?decisaoId=ID_DA_DECISAO` (após concluir Opções) |
| Priorização de pesos | http://127.0.0.1:8765/pages/pesos.html?decisaoId=ID_DA_DECISAO (após concluir Critérios) |
| Definição das opções | `http://127.0.0.1:8765/pages/opcoes.html?decisaoId=ID_DA_DECISAO` (use uma decisão sua na etapa 3 ou posterior) |
| Método 10–10–10 | `http://127.0.0.1:8765/pages/metodo-101010.html?decisaoId=ID_DA_DECISAO` (etapa interna 6 — Análise) |
| Inversão | `http://127.0.0.1:8765/pages/inversao.html?decisaoId=ID_DA_DECISAO` (etapa interna 6 — Análise) |
| Resultado comparativo | `http://127.0.0.1:8765/pages/resultado.html?decisaoId=ID_DA_DECISAO` (após concluir as quatro técnicas) |
| Consulta de decisão finalizada | `http://127.0.0.1:8765/pages/consulta.html?decisaoId=ID_DA_DECISAO` (somente decisões **Concluídas**; somente leitura) |
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

## Como executar o Bússola sem IntelliJ IDEA

Este roteiro usa só o **PowerShell** (pelo terminal do Cursor, do VS Code ou do próprio Windows). Não precisa de IntelliJ nem de Docker: o banco é o MySQL instalado no computador.

O Bússola tem três partes, e as três precisam estar ligadas ao mesmo tempo para usar as telas:

| Parte | O que é | Porta | Onde fica |
| --- | --- | --- | --- |
| Banco | MySQL (serviço do Windows) | 3306 | instalado no computador |
| Backend | API Java + Spring Boot | 8080 | pasta `backend` |
| Frontend | páginas HTML/CSS/JS servidas pelo Python | 8765 | pasta `frontend` |

### 1. Pré-requisitos

| Programa | Versão | Como conferir no PowerShell | Observação |
| --- | --- | --- | --- |
| Java (JDK) | **25** (o `pom.xml` define `java.version` 25) | `java -version` deve mostrar `25` | Recomendado: Eclipse Temurin 25. |
| MySQL Server | **8.0** (validado com 8.0.46 Community) | `Get-Service MySQL80` deve mostrar `Running` | O nome do serviço pode variar; `Get-Service *mysql*` lista todos. |
| Python | 3 (validado com 3.11) | `python --version` | Usado só para servir o frontend. |
| Maven | **não precisa instalar** | — | O projeto traz o *Maven Wrapper* (`backend\mvnw.cmd`), que baixa sozinho o Maven 3.9.16 na primeira execução (precisa de internet nessa primeira vez). |

O Maven Wrapper usa o Java indicado em `JAVA_HOME` ou, se ela não existir, o `java` do PATH. Para conferir:

```powershell
java -version
$env:JAVA_HOME
cd backend
.\mvnw.cmd -v
```

O último comando deve mostrar `Apache Maven 3.9.16` e `Java version: 25...`. Se mostrar outra versão do Java, veja "Problemas comuns" abaixo.

**Como abrir o terminal no Cursor ou VS Code:** abra a pasta do projeto (**File → Open Folder** → pasta `Bussola`), depois **Terminal → New Terminal** (ou **Ctrl + '**). O terminal abre já na pasta raiz do projeto. Confira se é **PowerShell** no canto do painel do terminal; se for outro, escolha PowerShell na setinha ao lado do **+**. Para abrir um segundo terminal, clique no **+**.

### 2. Configurar o banco de dados

1. **Ligar o MySQL.** Normalmente o serviço inicia junto com o Windows. Confira:

    ```powershell
    Get-Service MySQL80
    Test-NetConnection 127.0.0.1 -Port 3306
    ```

    O primeiro deve mostrar `Running`; o segundo, `TcpTestSucceeded : True`. Se estiver parado, abra o aplicativo **Serviços** do Windows (tecla Windows → digite "Serviços"), clique com o botão direito em **MySQL80** → **Iniciar**. Pelo terminal, também funciona `Start-Service MySQL80`, mas só em um PowerShell aberto **como administrador**.

2. **Banco usado:** `bussola`, em `localhost:3306`, com o usuário `root` por padrão (veja `backend/src/main/resources/application.properties`).

3. **Criar o banco (só na primeira vez).** O backend **não** cria o banco sozinho; ele precisa existir antes. No MySQL Workbench, execute:

    ```sql
    CREATE DATABASE IF NOT EXISTS bussola CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
    ```

    Sem o Workbench, dá para usar o cliente que vem com o MySQL (ele pede a senha do MySQL e não a mostra):

    ```powershell
    & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "CREATE DATABASE IF NOT EXISTS bussola CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    ```

4. **Tabelas:** não crie tabelas nem rode scripts SQL manualmente. As tabelas são criadas e atualizadas pelo **Flyway** automaticamente toda vez que o backend inicia, usando os arquivos de `backend/src/main/resources/db/migration` (V1 a V5). Nunca edite uma migration que já foi aplicada.

### 3. Configurar as variáveis de ambiente

O backend lê estas variáveis (definidas em `application.properties`):

| Variável | Obrigatória? | Padrão | Para que serve |
| --- | --- | --- | --- |
| `DB_PASSWORD` | **sim** | — | Senha do usuário do MySQL. |
| `JWT_SECRET` | **sim** | — | Chave que assina os logins (Base64 de pelo menos 32 bytes). Não é a senha do banco. |
| `DB_URL` | não | `jdbc:mysql://localhost:3306/bussola` | Só mude se o banco tiver outro nome, host ou porta. |
| `DB_USERNAME` | não | `root` | Só mude se usar outro usuário do MySQL. |

**`JWT_SECRET` (uma vez por computador).** Use o bloco de "Primeira configuração neste computador", passo 3: ele gera uma chave aleatória e a salva no usuário do Windows sem exibi-la. Depois disso, todo terminal **novo** já recebe `JWT_SECRET`. Terminais que já estavam abertos não enxergam a variável: feche e abra o terminal (ou o Cursor/VS Code).

**`DB_PASSWORD` (a cada terminal novo).** Por segurança, não salve a senha do MySQL no Windows nem em arquivos do projeto. Digite-a no terminal que vai rodar o backend; o texto fica oculto:

```powershell
$senhaBanco = Read-Host "Senha do MySQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $senhaBanco).Password
```

Variáveis definidas com `$env:` valem **só para aquele terminal**. Ao fechar o terminal, abrir outro ou reiniciar o Cursor/VS Code, defina `DB_PASSWORD` de novo. Se o seu banco não usar os padrões, defina também, no mesmo terminal:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/bussola"
$env:DB_USERNAME = "SEU_USUARIO_DO_MYSQL"
```

Para conferir sem mostrar valores:

```powershell
"DB_PASSWORD definida: " + [bool]$env:DB_PASSWORD
"JWT_SECRET definida: " + [bool]$env:JWT_SECRET
```

Não coloque senhas reais em `application.properties`, no Git, em prints ou neste arquivo.

### 4. Executar o backend pelo terminal

No **primeiro terminal**, a partir da pasta raiz `Bussola`:

```powershell
cd backend
$senhaBanco = Read-Host "Senha do MySQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $senhaBanco).Password
.\mvnw.cmd spring-boot:run
```

1. Na primeira vez, o wrapper baixa o Maven e as dependências; pode demorar alguns minutos.
2. Funcionou quando aparecerem as linhas `Tomcat started on port 8080 (http)` e `Started BussolaApplication in ... seconds`. O backend fica em **http://localhost:8080** (teste aberto: **http://localhost:8080/swagger-ui/index.html**).
3. As migrations pendentes do Flyway são aplicadas nesse momento, automaticamente.
4. **Deixe este terminal aberto** enquanto usa o sistema. O terminal fica "ocupado" com o log do backend; isso é normal.
5. **Para encerrar:** clique no terminal e pressione **Ctrl + C**. Se aparecer `Deseja finalizar o arquivo em lotes (S/N)?`, digite `S` e Enter.

Se terminar com `BUILD FAILURE` ou `APPLICATION FAILED TO START`, o backend **não** está rodando: leia a mensagem e veja "Problemas comuns".

### 5. Executar o frontend

Abra um **segundo terminal** (botão **+** no painel do terminal). Ele deve estar na pasta **raiz** `Bussola` (não em `backend`). Se não estiver, use `cd ..` ou `cd` até a raiz. Execute:

```powershell
.\rodar-frontend.ps1
```

O script entra em `frontend`, roda `python -m http.server 8765` e mostra os links úteis. Abra no navegador:

- **http://127.0.0.1:8765/pages/login.html** (login), ou
- **http://127.0.0.1:8765/pages/index.html** (página inicial).

Use sempre o endereço com a porta **8765**; abrir o HTML com duplo clique (`file:///`) não funciona com o backend. **Deixe este terminal aberto também.** Para encerrar: **Ctrl + C** nesse terminal.

Backend e frontend rodam em **terminais separados**, ao mesmo tempo. Fechar a aba do navegador não desliga nenhum dos dois.

### 6. Ordem de inicialização (resumo)

1. **MySQL ligado:** `Get-Service MySQL80` → `Running`.
2. **Terminal 1 (backend):** `cd backend`, definir `DB_PASSWORD` (e `JWT_SECRET`, se ainda não estiver salva no Windows).
3. **Terminal 1:** `.\mvnw.cmd spring-boot:run` e esperar `Started BussolaApplication`.
4. **Terminal 2 (frontend), na raiz:** `.\rodar-frontend.ps1`.
5. **Navegador:** http://127.0.0.1:8765/pages/login.html. Entre com um usuário cadastrado no Bússola (a senha do MySQL não serve para login).

O frontend até abre sem o backend (dá para ver o layout), mas login, cadastro e qualquer tela com dados exigem o backend, e o backend exige o MySQL.

### 7. Executar os testes sem IntelliJ

Todos os comandos abaixo rodam na pasta `backend`:

```powershell
cd backend
.\mvnw.cmd test
```

- Espere `BUILD SUCCESS` e o resumo `Tests run: N, Failures: 0, Errors: 0, Skipped: ...`.
- Quase todos os testes usam repositórios simulados e **não** precisam do MySQL.
- A exceção é `BussolaApplicationTests` (`contextLoads`): ele sobe a aplicação inteira, conecta ao MySQL real, aplica migrations pendentes e confere se as entidades batem com as tabelas. Ele só roda quando `DB_PASSWORD` e `JWT_SECRET` estão definidas **nesse terminal**; sem elas, aparece como `Skipped: 1` no resumo (não é falha). Para rodá-lo, defina `DB_PASSWORD` como no passo 3, com o MySQL ligado, e execute:

    ```powershell
    .\mvnw.cmd "-Dtest=BussolaApplicationTests" test
    ```

**Uma classe específica** (vírgula separa várias):

```powershell
.\mvnw.cmd "-Dtest=ResultadoServiceTest" test
.\mvnw.cmd "-Dtest=ResultadoServiceTest,HistoricoControllerTest" test
```

**Só conferir se o código compila** (sem rodar testes):

```powershell
.\mvnw.cmd test-compile
```

**Como ler falhas ligadas ao MySQL:** se o erro citar `Access denied for user`, `Communications link failure`, `Unknown database 'bussola'` ou `flywayInitializer`, o problema é de banco ou de variável (veja a tabela abaixo), não do código testado. Falhas com `expected: ... but was: ...` são falhas reais de regra de negócio.

### 8. Problemas comuns

| Sintoma | Causa provável | O que fazer |
| --- | --- | --- |
| `java` não é reconhecido como comando | Java não instalado ou fora do PATH | Instale o JDK 25 (marque a opção de configurar `JAVA_HOME`/PATH no instalador) e abra um terminal novo. |
| `release version 25 not supported`, ou `.\mvnw.cmd -v` mostra outro Java | Java antigo em `JAVA_HOME` ou no PATH | Aponte para o JDK 25 neste terminal: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25..."` (use a pasta real do seu JDK) e `$env:Path = "$env:JAVA_HOME\bin;$env:Path"`. Para valer sempre, ajuste `JAVA_HOME` em **Configurações → Sistema → Sobre → Configurações avançadas → Variáveis de Ambiente**. |
| `Communications link failure` / `Connection refused` | MySQL desligado | `Get-Service MySQL80`; inicie pelo aplicativo Serviços. |
| `Access denied for user 'root'@'localhost' (using password: YES)` | Senha errada **ou `DB_PASSWORD` não definida neste terminal** (sem a variável, o Spring envia o texto `${DB_PASSWORD}` como senha, por isso aparece "YES") | Defina `DB_PASSWORD` de novo neste terminal e reinicie o backend. |
| `Unknown database 'bussola'` | Banco ainda não criado | Crie o banco (passo 2.3). Não crie as tabelas. |
| Erro em `flywayInitializer` / `Migration ... failed` / `checksum mismatch` | Migration alterada depois de aplicada ou banco em estado inconsistente | Não edite migrations já aplicadas; desfaça a alteração local no arquivo da migration. Não apague tabelas de um banco com dados reais; peça ajuda ao grupo. |
| `Could not resolve placeholder 'JWT_SECRET'` ou `JWT_SECRET deve conter pelo menos 32 bytes` | Chave JWT ausente ou inválida | Faça o passo 3 da primeira configuração e **abra um terminal novo**. |
| `Port 8080 was already in use` | Já existe um backend rodando (outro terminal ou o IntelliJ) | Use o backend que já está rodando ou pare-o com **Ctrl + C** no terminal dele. Para descobrir quem usa a porta: `Get-NetTCPConnection -LocalPort 8080 -State Listen` e `Get-Process -Id NUMERO_DO_OwningProcess`. |
| Porta 8765 ocupada | Já existe um frontend rodando (outro terminal) | No Windows, o Python **pode iniciar sem mostrar erro** mesmo com outra cópia na mesma porta, e as páginas passam a vir de qualquer uma delas. Antes de iniciar, confira `Get-NetTCPConnection -LocalPort 8765 -State Listen`: se aparecer algo, use o frontend que já está aberto ou pare-o com **Ctrl + C** no terminal dele. |
| Telas abrem, mas login/dados falham (`Failed to fetch`, erro de CORS) | Backend desligado, ou página aberta por endereço diferente | Confira `Started BussolaApplication` no terminal 1 e abra as páginas por `http://127.0.0.1:8765` ou `http://localhost:8765` (o backend só aceita essas duas origens). Depois de atualizar arquivos, **Ctrl + F5**. |
| Variável "sumiu" | Terminal novo ou Cursor/VS Code reiniciado | `$env:` vale só para o terminal atual: defina de novo. `JWT_SECRET` salva no usuário só aparece em terminais abertos depois de salvar. |
| `rodar-frontend.ps1 não pode ser carregado porque a execução de scripts foi desabilitada` | Política de execução do PowerShell | Rode só para este comando, sem mudar nada permanente: `powershell -ExecutionPolicy Bypass -File .\rodar-frontend.ps1`. Ou, sem script: `python -m http.server 8765 --directory frontend` (na raiz). Se o arquivo veio de um download/zip, `Unblock-File .\rodar-frontend.ps1` também resolve. Não desative a proteção do Windows de forma permanente. |
| `O comando python não foi encontrado` | Python não instalado ou fora do PATH | Instale o Python 3 (marque **Add python.exe to PATH**) e abra um terminal novo. |

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
3. Abra novamente e clique em Confirmar e avançar. Só então a tela envia a conclusão ao backend e segue o fluxo. Em Critérios, avalie todas as alternativas; depois, escolha os pesos e avance para Análise.
4. **Salvar para depois** continua salvando sem abrir essa confirmação e sem encerrar a etapa. No Brainstorming, salvar ideias também não conclui a etapa.

Essa confirmação pertence à interface. No Swagger, as rotas de conclusão continuam avançando diretamente quando a requisição é válida. Não houve mudança nos comandos de inicialização; após atualizar, reinicie o backend e use **Ctrl + F5** nas páginas.

## Revisar uma etapa anterior

Enquanto a decisão está em andamento, Brainstorming, Opções, Critérios e Pesos podem ser reabertos pelo indicador. A Análise abre depois de concluir Pesos. A contextualização continua editável só na etapa 1. Uma decisão concluída permanece somente para consulta.

Mantenha MySQL, backend e frontend ligados. Use uma decisão sua que já tenha passado da etapa que você quer revisar.

1. Na etapa atual, clique no nome de uma etapa anterior no indicador. A página não muda. Abre o aviso **Revisar esta etapa?**, com o texto de que as respostas podem ser alteradas e de que informações e resultados seguintes poderão ser atualizados.
2. Clique em **Cancelar** ou pressione **Esc**. O aviso fecha, o endereço continua o mesmo e a decisão não é gravada de novo. O foco volta ao passo clicado.
3. Abra de novo e clique em **Continuar revisão**. A etapa escolhida abre com `revisao=1` na URL e mostra o aviso **Suas alterações podem atualizar o resultado desta análise.** No indicador, essa página fica em destaque. A etapa já salva no backend continua marcada como alcançada, com o círculo só contornado, sem o mesmo preenchimento da página aberta. O botão de concluir a etapa fica oculto. Em Opções e Critérios, o salvamento aparece como **Salvar alterações**. Em Critérios, se a decisão já chegou em Pesos, **Continuar para pesos** fica à esquerda e **Salvar alterações** à direita, na mesma linha.
4. Altere uma resposta e salve. Recarregue a página: a alteração continua, os identificadores das opções, critérios ou ideias já existentes continuam os mesmos e `etapaAtual` não volta nem avança. Os dados das etapas seguintes não são apagados.
5. No indicador, o passo da etapa atual (quando essa etapa tem página) volta para ela sem o aviso de revisão. Qualquer etapa ainda não alcançada não é clicável. Análise abre a tela de técnicas quando a decisão chega à etapa 6. Abrir Critérios ou Opções direto na URL antes da hora continua respondendo **409** e a tela não libera a edição.
6. Sem `revisao=1`, uma etapa já passada abre só para consulta, sem o aviso de revisão. Se a decisão já passou de Critérios e a tela aberta é a própria avaliação, clique de novo em **Critérios** no indicador: o popup **Revisar esta etapa?** aparece e **Continuar revisão** acrescenta `revisao=1`. A decisão de outra conta responde **404**.

## Sair sem perder o que foi digitado

Nas telas de Contextualização, Brainstorming, Opções e Critérios, a página compara os campos com o último dado carregado ou salvo. Digitar e depois apagar até voltar ao mesmo texto libera a saída. Carregar a página não conta como alteração.

Se houver diferença e você clicar no indicador, em Início, em Sair ou em outro link interno, abre o aviso **Alterações não salvas**, com o botão **Continuar editando**. Esc fecha o aviso e mantém o que foi digitado. Não há botão para descartar. **Salvar para depois** ou **Salvar alterações** grava e, se der certo, libera a navegação. Se o salvamento falhar, o texto permanece e a saída continua bloqueada.

No Brainstorming, cada ideia adicionada, editada ou excluída já é gravada na hora. O bloqueio vale para o texto ainda no formulário e para uma exclusão que ainda não foi confirmada. Em Critérios, incluir ou remover um critério, ou mudar uma nota, bloqueia a saída até salvar. Confirmar notas pendentes não grava essas mudanças: a tela pede para salvar antes.

Atualizar ou fechar a aba usa o aviso do próprio navegador. O botão Voltar também é interceptado quando a página consegue. O navegador pode ignorar essa proteção em alguns fechamentos.

A alteração de informações anteriores não recalcula automaticamente os pesos nem o resultado da análise. Quando o nome de uma alternativa já avaliada muda, as notas ficam pendentes de confirmação, como explica a seção seguinte.

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
9. Com uma pendência aberta, o quadro **Para liberar o resultado comparativo** da Inversão mostra o item **Avaliações dos critérios** (lido do mesmo dado que o backend usa para bloquear o resultado) e o botão **Revisar avaliações**. Ao clicar em **Ver resultado comparativo**, a Inversão é concluída e a página de Resultado informa qual alternativa precisa ser revisada (por exemplo, "Confirme as notas da alternativa A (nome) em Critérios"), com o link **Revisar avaliações** já em foco e **Voltar para a Inversão**.
10. **Revisar avaliações** abre Critérios em revisão, já com o aviso **Revise sua avaliação** e o foco no botão de confirmação. Vindo da Inversão, aparece **Voltar para a Inversão**; vindo do Resultado, **Voltar ao resultado**. Se houver texto não salvo na Inversão ou nota não salva em Critérios, a saída é bloqueada como de costume. Depois de confirmar, volte e abra o resultado novamente.

Não confunda essa pendência com `resultados.requer_atualizacao`: esse campo só marca que uma pontuação gravada ficou antiga e não bloqueia nada. A consulta do resultado sempre calcula a partir das notas e pesos atuais; a pontuação só é gravada de novo ao finalizar a decisão. O que bloqueia é `opcoes.avaliacao_pendente`. Lembre também que trocar o nome de uma opção desfaz a conclusão do 10/10/10 e da Inversão: conclua-as de novo antes de ver o resultado.

Alterar notas ou pesos sinaliza resultados existentes para atualização. Esta entrega não cria um novo cálculo de recomendação.

## Testar contextualização e brainstorming

O fluxo disponível é Home → Contextualização → Brainstorming → Opções → Critérios → Pesos → Análise. A Análise inclui Prós e Contras, Matriz GUT, 10–10–10 e Inversão. A página Resultado comparativo só libera depois de concluir Prós e Contras, Matriz GUT, 10–10–10 e Inversão.

Depois de atualizar o projeto, **reinicie o backend no IntelliJ** e atualize o navegador com **Ctrl + F5**. Não precisa criar tabelas nem mudar variáveis ou portas.

1. Faça login e clique em **Iniciar decisão** na Home.
2. Informe o título e, se quiser, o contexto. **Salvar para depois** mantém a decisão na etapa 1.
3. Reabra pela Home, altere o contexto e clique em **Salvar e continuar** e depois em **Confirmar e avançar**. Deve abrir o brainstorming (etapa 2 de 6).
4. Selecione **Favorável**, **Desfavorável** ou **Dúvida**, escreva uma ideia e clique em **Adicionar**.
5. Use **Editar** para mudar texto ou categoria. **Excluir** pede confirmação. Cada ação é salva no banco.
6. Atualize a página ou retorne pela Home: as ideias devem continuar lá. Tanto o cartão da decisão quanto **Continuar** retomam o fluxo salvo.
7. Adicione pelo menos uma ideia. Sem nenhuma ideia salva, **Próxima etapa** fica bloqueada. Depois, clique em **Próxima etapa** e escolha **Confirmar e avançar**. A decisão passa para **Opções (etapa 3)**, preservando as ideias e o status **Em andamento**.

A tela de Opções já está implementada em `opcoes.html`. A conclusão do brainstorming abre `opcoes.html?decisaoId=ID_DA_DECISAO` após salvar `etapaAtual: 3`. Sem pedir revisão, as etapas anteriores ficam para consulta. A revisão com confirmação está descrita em **Revisar uma etapa anterior**. É obrigatório manter pelo menos uma ideia salva para concluir o brainstorming. Texto de ideia é obrigatório ao adicionar/editar; limite técnico: 16.000 caracteres, compatível com a coluna TEXT.

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

Categorias aceitas: `FAVORAVEL`, `DESFAVORAVEL`, `DUVIDA`. Sem login: **401**. Dados inválidos: **400**. Decisão de outra pessoa ou ideia fora da decisão informada: **404**. Incluir, editar ou excluir ideia em uma decisão em andamento que já passou do brainstorming responde **201**, **200** ou **204** e não altera `etapaAtual`. A contextualização e a conclusão do brainstorming fora da etapa exata, o acesso antes do brainstorming e qualquer alteração em decisão concluída respondem **409**. Concluir sem nenhuma ideia responde **409**. Repetir a conclusão quando a decisão já está na etapa 3 não avança uma segunda vez.



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

Integração entre as etapas: Opções abre Critérios depois de concluir. Critérios abre Pesos e envia o identificador da decisão. O evento cancelável bussola:opcoes-concluidas continua disponível. A retomada encaminha a etapa 5 para Pesos e a etapa 6 para Análise.

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
8. Após concluir a avaliação, a tela abre Pesos. Ajuste cada critério de 1 a 5; a soma e a barra atualizam enquanto você move os controles. Próxima etapa salva os pesos e abre Análise. Para editar notas ou pesos depois, use Revisar uma etapa anterior; as alterações são salvas sem reiniciar o progresso.

O indicador **Etapa 3 de 5** segue o Figma e conta as etapas da análise; o banco/Home contam também a contextualização, por isso Critérios é a etapa 4 de 6.

**Totais:** são somas brutas sem pesos, com indicação de parcial quando há notas faltantes. Não são uma recomendação: incluem a nota de Risco no sentido escolhido pelo usuário. O resultado comparativo interpreta `sentido: MENOR_MELHOR` (6 − nota) antes de comparar alternativas. Os pesos existentes não são redefinidos ao salvar notas.

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
- Critérios abre a página de Pesos após concluir a avaliação; o evento cancelável bussola:criterios-concluidos continua disponível.
- A API retorna `criterios[].id`, `codigo`, `nome`, `descricao`, `personalizado`, `peso`, `sentido`, `notaA` e `notaB`. IDs são null somente para os cinco critérios ainda não gravados; depois de salvar, são os IDs reais para Pesos/Avaliações. O GET não cria registros.
- `sentido` é `MENOR_MELHOR` em Risco e `MAIOR_MELHOR` nos demais (inclusive critérios personalizados). O resultado comparativo normaliza Risco como `6 - nota` antes de aplicar o peso (veja "Como o resultado é calculado"). O total bruto desta tela não deve ser reutilizado como resultado final.
- GET /decisoes/{id}/pesos consulta os valores; PUT salva sem avançar; POST /decisoes/{id}/pesos/concluir salva e avança da etapa 5 para 6. Envie uma entrada por critério, com id e peso inteiro de 1 a 5. Critérios ausentes, repetidos ou de outra decisão são rejeitados.
- Mudanças nos pesos sinalizam resultados anteriores como desatualizados. A tela não calcula nem recomenda uma alternativa.

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

`.\mvnw.cmd test` roda a suíte inteira. `BussolaApplicationTests` (contexto completo) conecta ao MySQL real e pode aplicar migrations pendentes; ele só roda quando `DB_PASSWORD` e `JWT_SECRET` estão definidas nesse terminal e, sem elas, aparece como `Skipped: 1`. Detalhes em "Como executar o Bússola sem IntelliJ IDEA", item 7.

## Alternativa: backend pelo PowerShell

Está descrita, passo a passo, em "Como executar o Bússola sem IntelliJ IDEA" (itens 3 e 4). As variáveis colocadas no IntelliJ não passam automaticamente para o PowerShell: defina `DB_PASSWORD` no terminal que vai rodar o backend.

## Se algo não funcionar

| Sintoma | O que conferir |
| --- | --- |
| `Access denied for user` | Usuário/senha do MySQL na configuração usada para iniciar o backend. |
| `Unknown database bussola` | Crie o banco no Workbench; não crie as tabelas manualmente. |
| Erro com `JWT_SECRET` | Faça a configuração da chave acima e reinicie o IntelliJ (ou abra um terminal novo, se usa o PowerShell). |
| Porta 8080 ocupada | Já pode existir um backend rodando. Pare a execução antiga antes de iniciar outra. |
| `Failed to fetch` ou CORS | Backend ligado e tela aberta pela porta 8765, usando localhost ou 127.0.0.1. |
| Site não abre na 8765 | Inicie o frontend e mantenha o terminal aberto. |
| Login fica em `Entrando...` ou o servidor demora | O login aguarda até 15 segundos e libera o botão se não receber resposta. No PowerShell clássico, deixar texto selecionado pode pausar o backend: pressione **Esc** no terminal do backend, mantenha-o aberto e tente novamente. Se persistir, confira os erros no terminal. Após atualizar o JavaScript, use **Ctrl + F5** na página. |
| Erro ao ler `idDaUrl` na nova decisão | Confira se `frontend/js/pages/decisao-api.js` está presente e carregado antes de `nova-decisao.js`/`brainstorming.js`. A integração atual inclui esse arquivo. Atualize os arquivos completos e use **Ctrl + F5**. |
| `401` | Faça login novamente e use o token; no Swagger, clique em Authorize. |
| `409` no cadastro | O e-mail já existe: faça login ou use outro e-mail de teste. |
| `BUILD FAILURE` | Leia o erro; não significa que o backend iniciou. |
| `cannot find symbol` para `EtapaInvalidaException` ou `IdeiaNaoEncontradaException` | Confira se os dois arquivos `.java` estão em `backend/src/main/java/br/com/bussola/exception/`, com o pacote `br.com.bussola.exception`. Esse erro ocorre antes da conexão ao banco; não exige trocar senha ou porta. Após obter a correção, execute os testes ou inicie novamente o backend. |

**Resumo:** MySQL ligado → ▶ BussolaApplication (ou `.\mvnw.cmd spring-boot:run` em `backend`, com `DB_PASSWORD` definida) → `.\rodar-frontend.ps1` na raiz → abrir login.

## Prós e Contras e Matriz GUT

Reinicie o backend e use **Ctrl + F5** no frontend. Não há dependências, variáveis ou migrations novas.

Pré-requisito: a decisão deve ter duas alternativas e estar na etapa interna 6 — Análise (visual 5 de 5). Conclua Critérios, defina os pesos e avance. Não altere o banco pessoal para pular etapas.

Ao concluir Pesos, o backend libera a etapa 6 e a tela abre a página de Análise. A Home e o indicador retomam essa tela quando a API informa a etapa 6.

### Conferir pela tela, com uma decisão já liberada

1. Na Home, clique em **Continuar** na decisão em Análise.
2. Em **Prós e Contras**, as duas alternativas aparecem ao mesmo tempo: **Alternativa A — nome** e **Alternativa B — nome**, cada uma com os cards **Prós (n)** e **Contras (n)**. Clique em **+ Adicionar pró** ou **+ Adicionar contra** no card desejado. O formulário abre dentro do próprio card, já ligado àquela alternativa e àquele tipo. Escreva a descrição e clique em **Salvar**: o item aparece na lista e o contador aumenta. Em **Editar**, o mesmo card mostra o texto atual e o botão **Salvar alterações**. Confira que os outros três cards permanecem intactos.
3. Exclua um registro: primeiro cancele a confirmação; depois confirme e confira a lista.
4. Em **Matriz GUT**, cadastre um fator com notas 5, 4 e 3. A prioridade deve ser **60**. Edite para 1, 1 e 1: deve virar **1** e mudar de posição na lista.
5. Recarregue a página para conferir os dados salvos. Um texto digitado e ainda não salvo (novo ou em **Editar**) impede trocar de técnica, avançar, sair ou abrir outro formulário até clicar em **Salvar** ou **Cancelar**; um formulário aberto sem alterações não bloqueia nada. Se o backend recusar a descrição, o texto continua no card para corrigir; falhas de conexão ou do servidor exigem clicar em **Recarregar dados** antes de tentar novamente, evitando duplicação.
6. Na barra inferior, **Salvar para depois** grava o rascunho do formulário aberto pela mesma operação do botão **Salvar** (sem rascunho, apenas confirma que tudo já está salvo). **Próxima: Matriz GUT** abre a aba GUT e, nela, **Próxima: 10/10/10** abre o método 10/10/10. Nenhum dos dois altera a etapa da decisão.
7. Decisões concluídas são somente consultáveis. Salvar técnicas não conclui a decisão. O método 10–10–10 e a Inversão continuam acessíveis na barra de técnicas a qualquer momento. O botão **Ver resultado comparativo** fica só na Inversão, a última técnica da sequência Prós e Contras → Matriz GUT → 10/10/10 → Inversão.

Prós/Contras pertencem à alternativa; os fatores GUT pertencem à decisão inteira. A prioridade é calculada pelo servidor como G × U × T (1–125), ordenada de forma decrescente, com desempate pelo ID. Essa prioridade organiza os fatores da Matriz GUT; o resultado comparativo usa as notas e os pesos dos critérios.

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

Esse teste exercita controller, serviço e segurança, com repositórios simulados. Não exige senha do MySQL e não grava no seu banco. Para a suíte sem banco, use `'-Dtest=*Test'` no mesmo comando. Os testes não substituem a validação do caminho completo pelas telas, incluindo Pesos.

## Histórico de decisões

Reinicie o backend e atualize o navegador com **Ctrl + F5**. Na Home, clique em **Histórico** ou **Ver todas**. O cabeçalho das etapas também abre o histórico, preservando a proteção dos rascunhos.

- Pesquise pelo título e combine com Todas, Concluídas ou Em andamento. A busca trata `%` e `_` como texto, não como curingas.
- Ordene por última atualização (mais recente ou mais antiga) ou título. A lista mostra 10 decisões por página; use Anterior/Próxima. Os totais do topo representam sua conta inteira, mesmo com filtros.
- Continuar decisão abre a tela correspondente: Critérios na etapa 4, Pesos na etapa 5 e Análise na etapa 6.
- Decisões **concluídas** (finalizadas na página de Resultado) mostram **Concluída**, **Finalizada em** com a data, **Visualizar resultado** e **Excluir decisão**. Não têm **Continuar decisão**. **Visualizar resultado** abre `resultado.html?decisaoId=ID` (rota `resultadoDecisao` em `frontend/js/pages/historico-integracoes.js`; na Home, em `home-integracoes.js`). O evento cancelável `bussola:ver-resultado` continua disponível.
- Uma decisão na etapa 6 de 6 (100%) continua **Em andamento** até ser finalizada explicitamente.
- **Excluir** / **Excluir decisão** abre uma confirmação com o título e explica que a exclusão é permanente. Cancelar não altera nada. Confirmar apaga a decisão e todos os seus registros relacionados. Após excluir, a Home recalcula seus totais usando os registros restantes.
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

A revisão de Critérios e Pesos preserva os dados e o progresso e sinaliza resultados anteriores quando notas ou pesos mudam. O resultado comparativo usa a soma de nota ajustada × peso para cada alternativa (no Risco, nota ajustada = 6 − nota). A contextualização permanece editável somente antes de avançar; o indicador de revisão começa em Brainstorming.

## Finalizar uma decisão

Este é um teste funcional: precisa de MySQL, backend e frontend ligados. Depois de atualizar o código, reinicie o backend (ou aguarde o reinício automático, se ele estiver rodando com `spring-boot:run`) e atualize o navegador com **Ctrl + F5**.

Use uma decisão de teste, não uma decisão real que você queira continuar editando: **finalizar não pode ser desfeito**.

1. Leve uma decisão de teste até o resultado (Prós e Contras, Matriz GUT, 10–10–10 e Inversão concluídos, sem avaliações pendentes) e abra `http://127.0.0.1:8765/pages/resultado.html?decisaoId=ID_DA_DECISAO`. Só visualizar o resultado **não** finaliza nada: no Histórico ela continua **Em andamento**.
2. No fim da página, clique em **Finalizar decisão**. Abre o popup **Finalizar esta decisão?**, com o fundo escurecido e desfocado. O foco começa em **Continuar analisando**.
3. **Continuar analisando** ou a tecla **Esc** fecham o popup sem alterar nada; o foco volta ao botão **Finalizar decisão**.
4. **Finalizar** desabilita os dois botões ("Finalizando…") até o backend responder. Se der certo, o popup fecha e a página mostra **Decisão finalizada em … · somente consulta**, sem **Revisar análise** nem **Finalizar decisão**, com os botões **Visualizar minha análise** e **Voltar ao Histórico**. As pontuações exibidas são as mesmas.
5. Se o backend recusar (por exemplo, porque outra aba criou uma pendência), o erro aparece dentro do popup e a decisão continua **Em andamento**. Ao fechar o popup, o resultado é consultado de novo e a página mostra o motivo do bloqueio, sem pontuações desatualizadas.
6. Recarregue a página e confira no **Histórico**: a decisão aparece como **Concluída**, com **Visualizar resultado** e **Excluir decisão**. O filtro **Concluídas** e os totais do topo já a contam como concluída.
7. Abrir uma URL antiga de edição dessa decisão (por exemplo `analise.html?decisaoId=ID`, `criterios.html?decisaoId=ID&revisao=1`, `inversao.html?decisaoId=ID`) mostra o quadro **Esta decisão foi concluída**, com **Visualizar minha análise** (abre a consulta na etapa correspondente), **Visualizar resultado** e **Voltar ao Histórico**, e todos os campos ficam somente para consulta.

Na API (Swagger), `POST /decisoes/{id}/finalizar`:

| Situação | Resposta |
|---|---|
| Resultado válido | **200**, com `status: "CONCLUIDA"`, `finalizadaEm` e as pontuações gravadas |
| Ainda falta algo (técnica não concluída, avaliação pendente, etapa anterior) | **409** com o motivo em `erro`; a decisão continua `EM_ANDAMENTO` |
| Decisão já concluída | **200**, devolvendo o resultado sem gravar nada nem mudar o status |
| Duas finalizações ao mesmo tempo (duas abas) | ambas **200**; a segunda espera a primeira e encontra a decisão já concluída |
| Decisão inexistente ou de outra conta | **404** |
| Sem login | **401** |

Depois de concluída, todas as rotas de escrita da decisão (contexto, ideias, opções, critérios, confirmação de avaliações, pesos, prós e contras, GUT, 10–10–10 e Inversão) respondem **409**. `GET /decisoes/{id}/resultado` continua funcionando e nunca grava nada: calcula a partir das notas e pesos, que não mudam mais depois de concluída. A exclusão (`DELETE /decisoes/{id}?confirmar=true`) continua permitida. Não há migration nova: o status usa a coluna `status` existente, e a data de finalização exibida é a última atualização da decisão, que não muda mais depois de concluída.

Testes automatizados (no terminal dentro de `backend`, sem banco):

```powershell
.\mvnw.cmd '-Dtest=ResultadoServiceTest,HistoricoControllerTest' test
```

### Como o resultado é calculado

- Cada critério contribui com **nota ajustada × peso**. Nos critérios favoráveis (inclusive os personalizados), a nota ajustada é a própria nota. No **Risco**, a nota informada mede o risco e é invertida uma única vez no cálculo: **nota ajustada = 6 − nota** (risco 1 conta 5, risco 3 conta 3, risco 5 conta 1). Com peso 3: risco 1 = 15 pontos, risco 3 = 9, risco 5 = 3. A nota gravada no banco não muda.
- A pontuação máxima é a soma de **peso × 5** de todos os critérios. As barras do Resultado mostram a pontuação de cada critério sobre o máximo dele (por exemplo, 12/15), e o Risco mostra "Risco informado X → conta Y".
- **Empate:** se as duas alternativas somam o mesmo, o Resultado mostra **Empate entre as alternativas**, a pontuação comum e um selo neutro, sem destacar nenhuma. A ordem de cadastro não desempata. Uma decisão empatada pode ser finalizada normalmente.
- A resposta de `GET /decisoes/{id}/resultado` e de `POST /decisoes/{id}/finalizar` traz `empate` (true/false) e `recomendadaId` (id da alternativa de maior pontuação, ou `null` no empate). Em cada critério: `nota` (informada), `sentido`, `notaAjustada` e `pontuacao`.
- O GET do resultado é só leitura, então várias abas podem consultá-lo ao mesmo tempo. As pontuações são gravadas em `resultados` apenas ao finalizar, com a decisão bloqueada.
- Decisões finalizadas antes desta correção são exibidas com o cálculo corrigido, feito a partir das notas gravadas (que não mudam). O total antigo continua no banco, sem alteração.

Para conferir: em uma decisão de teste, dê Risco 1 para A e 5 para B (os outros critérios iguais). A deve ficar à frente. Para ver o empate, use notas diferentes que somem o mesmo, por exemplo Segurança A=5/B=1 e Crescimento A=1/B=5 com o mesmo peso.

Testes automatizados (no terminal dentro de `backend`, sem banco): `.\mvnw.cmd '-Dtest=ResultadoServiceTest' test`.

## Consultar todas as etapas de uma decisão finalizada

Teste funcional: precisa de MySQL, backend e frontend ligados (a página só lê dados da API). Não há rota nova no backend; a consulta usa os mesmos `GET` das telas de edição. Depois de atualizar o código, recarregue o navegador com **Ctrl + F5**.

1. No **Histórico**, clique em **Visualizar resultado** de uma decisão **Concluída**.
2. No fim da página de Resultado, clique em **Visualizar minha análise**. Abre `consulta.html?decisaoId=ID&etapa=contexto`.
3. No topo aparece o aviso fixo **Decisão finalizada — somente leitura** e o indicador com as 10 etapas: Contexto, Brainstorm, Opções, Critérios (notas e critérios personalizados), Pesos, Prós e Contras, Matriz GUT, 10/10/10, Inversão e Resultado. A etapa atual fica destacada e o título mostra "Consulta · Etapa X de 10".
4. Navegue clicando em qualquer etapa do indicador (em qualquer ordem) ou pelos botões **← Anterior** e **Próxima →** no fim da página. Os botões **Voltar** e **Avançar** do navegador também funcionam, e o endereço guarda a etapa (`&etapa=criterios`, por exemplo), então dá para recarregar ou compartilhar o link da etapa.
5. **Resultado comparativo** (no topo) ou a etapa 10 voltam para `resultado.html`; **Voltar ao Histórico** volta à lista.
6. Confira que não há campos, botões de salvar/editar/excluir/adicionar nem "Concluir": é só leitura. No Histórico, a decisão continua **Concluída** com a mesma data de finalização.

Situações esperadas:

- Decisão **em andamento**: a página não mostra os dados e orienta a continuar pelo Histórico (a consulta completa só existe depois de finalizar). A revisão e a edição continuam pelas telas normais.
- Decisão de **outra conta** ou inexistente: a página mostra "Decisão não encontrada para sua conta" e nenhum dado; a API responde **404**.
- Sem login: a página volta para o login.

Para iniciar uma nova análise, use **+ Nova decisão**; a consulta não reabre nem copia a decisão finalizada.

## Método 10–10–10

A página `frontend/pages/metodo-101010.html` permite registrar, para **cada uma das duas opções**, como a pessoa imagina que se sentirá em **10 minutos, 10 meses e 10 anos**. Os nomes vêm da própria decisão. Não há respostas automáticas nem textos de exemplo salvos como se fossem respostas do usuário.

### Iniciar e abrir

Continue usando os mesmos comandos/terminais. **Reinicie o backend atualizado**, com MySQL disponível, e espere **Started BussolaApplication**. O Flyway aplica automaticamente **V4__Conclusao_reflexao_101010.sql**, que adiciona uma coluna opcional de data de conclusão da técnica em `decisoes`. Os textos usam a tabela `reflexoes_101010` já existente. Não apague o banco nem altere migrations antigas. Não há novas dependências, variáveis ou portas. Mantenha o frontend aberto e use **Ctrl + F5**.

A API exige uma decisão sua na **etapa interna 6 — Análise**, com duas alternativas. Na Home/Histórico, continue essa decisão e, na tela de Análise, clique em **Técnica 10/10/10**. Também é possível abrir a URL da tabela de links. Trocar o ID ou a etapa na URL não libera acesso.

A tela de Pesos permite chegar aqui pelo fluxo normal: Home, Contextualização, Brainstorming, Opções, Critérios, Pesos e Análise.

Opcionalmente, para testar somente esta técnica com uma decisão fictícia:

1. Cadastre uma **conta de teste** no Bússola e entre nela.
2. No MySQL Workbench, conecte ao banco local usado pelo backend (3307 neste computador, se mantida a configuração anterior; o padrão do projeto é 3306).
3. Abra **File → Open SQL Script** e selecione `backend/src/test/resources/metodo-101010-desenvolvimento.sql`.
4. Substitua somente `SEU_EMAIL_DE_TESTE` pelo e-mail dessa conta e execute o script completo pelo botão de raio. Não coloque senha no arquivo e não envie a alteração do e-mail ao Git.
5. O script cria uma **nova decisão fictícia** com duas opções na etapa 6 e devolve a URL. Nenhuma decisão existente é modificada. Se o e-mail não existir, não cria registros. Cada execução válida cria outro cenário de teste.
6. Abra a URL na aba autenticada ou retome pela Home. O script não é migration, não roda automaticamente e não substitui o fluxo de Pesos.

### Conferir pela tela

1. Confira o título e os nomes de A/B. Inicialmente os seis campos devem estar vazios.
2. Preencha só um campo e clique em **Salvar para depois**. Recarregue: o texto deve permanecer e a técnica ainda não estará concluída. Cada texto aceita até **16.000 caracteres**; apagar e salvar limpa aquela resposta.
3. Com campos ainda vazios, clique em **Próxima: Inversão**. Se houver texto não salvo, ele é salvo como em **Salvar para depois**; em seguida a Inversão abre, sem confirmação e sem concluir o 10–10–10.
4. Volte pela aba **Técnica 10/10/10**, preencha os seis campos e clique em **Próxima: Inversão**. Cancele com **Continuar revisando** ou **Esc**: nenhum envio deve acontecer e as respostas permanecem. Abra novamente e escolha **Confirmar e continuar**: a técnica é concluída e a Inversão abre.
5. A técnica fica marcada como concluída, mas a decisão permanece **EM_ANDAMENTO**, na etapa 6. Já concluída e sem alterações, **Próxima: Inversão** apenas abre a Inversão. O resultado comparativo é aberto pela Inversão, depois que Prós e Contras, Matriz GUT, 10–10–10 e Inversão estiverem concluídas e todas as avaliações tiverem sido confirmadas.
6. Recarregue para conferir as seis respostas e a indicação de conclusão. Enquanto a decisão estiver em andamento, editar e salvar respostas exige nova conclusão da técnica. Revisar o nome ou a descrição de uma alternativa também remove a conclusão do 10–10–10, preservando seus textos para revisão. Uma decisão concluída é apenas consultável.
7. Os links **Prós e Contras**, **Matriz GUT** e **Inversão** abrem a técnica correspondente com o mesmo ID. Rascunhos não salvos impedem a saída.
8. Se um envio falhar sem confirmar a gravação, os textos permanecem na tela e novos envios ficam bloqueados. Use **Recarregar dados**, confirmando a substituição pelo que realmente foi salvo. Não há reenvio automático.

**Teste visual:** sem a API, somente o cabeçalho e os estados de carregamento/erro podem ser conferidos. Para ver as opções reais, preencher, salvar e concluir, use **MySQL + backend + frontend**. O navegador não cria uma decisão de teste automaticamente.

### API e integração

No Swagger, autentique-se, use **Authorize** e o ID da decisão fictícia:

| Operação | Rota | Resultado |
| --- | --- | --- |
| Consultar | GET /decisoes/{decisaoId}/reflexoes-101010 | 200; não cria registros; textos ausentes são null |
| Salvar parcial | PUT /decisoes/{decisaoId}/reflexoes-101010 | 200; salva as duas opções atomicamente, mantendo IDs |
| Concluir técnica | POST /decisoes/{decisaoId}/reflexoes-101010/concluir | 200; exige seis textos e registra a data de conclusão, sem encerrar a decisão |

Copie do GET os IDs reais de A/B, que seguem a ordem crescente de ID das opções. Exemplo de corpo completo para PUT/POST:

```json
{
  "opcaoA": { "opcaoId": 21, "curtoPrazo": "Alívio", "medioPrazo": "Aprendizado", "longoPrazo": "Experiência" },
  "opcaoB": { "opcaoId": 22, "curtoPrazo": "Calma", "medioPrazo": "Estabilidade", "longoPrazo": "Equilíbrio" }
}
```

No PUT, os textos podem ser null. Sempre envie as duas opções; campos omitidos/nulos limpam aquela resposta. IDs trocados/de outra decisão, texto acima do limite ou conclusão incompleta retornam **400**; sem sessão, **401**; decisão inexistente/de outra conta, **404**; etapa diferente de 6 ou alteração de decisão concluída, **409**. O Swagger não mostra o diálogo do frontend: POST válido conclui diretamente.

A resposta traz `decisaoId`, `titulo`, `etapaAtual`, `status`, `concluida`, `concluidaEm` e `opcaoA/B`, com ID/nome/descrição da opção, `reflexaoId` e os três textos. Repetir dados idênticos não duplica registros nem altera a data de conclusão. Alterações marcam os resultados existentes das opções afetadas para atualização, sem apagar a pontuação anterior.

As rotas ficam em `frontend/js/pages/metodo-101010-integracoes.js`: **Próxima: Inversão** usa `inversao` (`inversao.html`). A navegação acrescenta `decisaoId`. O evento cancelável `bussola:101010-concluido` informa `detail: { decisaoId, etapa: 6 }` quando o usuário segue para a Inversão com a técnica concluída. Esse evento não significa que toda a decisão foi finalizada.

### Testes automatizados

No PowerShell, **dentro de backend**, execute:

```powershell
.\mvnw.cmd '-Dtest=Reflexoes101010ServiceTest,Reflexoes101010ControllerTest,OpcoesServiceTest' test
```

Espere **BUILD SUCCESS**. Os testes usam repositórios simulados, sem MySQL, e verificam salvamento parcial, seis respostas obrigatórias, titularidade, ordem das etapas, IDs estáveis, reenvios, revisão, conclusão independente, autenticação e CORS. Eles não substituem o teste de persistência no MySQL pelo roteiro acima.

## Técnica de Inversão

A página `frontend/pages/inversao.html` registra **possíveis fracassos e ações de prevenção para cada alternativa**. A barra superior mantém as quatro técnicas disponíveis. Os nomes de A/B vêm da decisão; os exemplos do Figma não são gravados como respostas.

### Iniciar e testar

1. Continue usando os mesmos comandos. Com **MySQL disponível**, reinicie o backend e aguarde **Started BussolaApplication**. O Flyway aplica **V5__Conclusao_inversao.sql**, acrescentando a data opcional de conclusão da técnica. A tabela `inversoes` já existe; não apague o banco nem edite migrations aplicadas.
2. Mantenha os terminais do backend e frontend abertos. No navegador, pressione **Ctrl + F5**, faça login e retome uma decisão sua em **Análise**, após concluir Pesos.
3. Clique em **Inversão** na barra. A URL recebe `decisaoId`; a API exige etapa interna 6 e duas alternativas. Alterar a URL não permite pular etapas.
4. Os quatro campos aparecem juntos, sem precisar escolher a alternativa: primeiro o grupo **Alternativa A — nome**, com "O que poderia dar errado" e "Como evitar" lado a lado; abaixo, o mesmo para **Alternativa B**. No celular, os campos ficam empilhados. O **Tab** percorre problemas A, prevenção A, problemas B e prevenção B.
5. **Salvar para depois** aceita campos incompletos. Atualize a página para verificar a persistência e confira que cada texto voltou no campo da alternativa certa. O contador geral (**x de 4**) e o de cada alternativa (**x de 2 preenchidos**) acompanham o que está digitado. Textos aceitam até 16.000 caracteres; apagar e salvar limpa a resposta. Nada é salvo automaticamente.
6. **Ver resultado comparativo** exige os quatro campos. Se faltar algum, o quadro acima dos botões lista quais respostas faltam e a tela foca o primeiro campo vazio. Com tudo preenchido, abre uma confirmação; **Continuar revisando** ou **Esc** não envia dados. **Confirmar e continuar** salva e conclui somente a técnica.
7. A decisão permanece **EM_ANDAMENTO**, na etapa 6. O quadro **Para liberar o resultado comparativo**, acima dos botões, mostra a situação de Prós e Contras, Matriz GUT, 10/10/10 e Inversão. Depois de concluir, a tela abre `resultado.html`, que faz a única consulta ao resultado no backend. Se ainda faltar algo (inclusive avaliações pendentes), a página de Resultado explica o motivo, não mostra pontuação e oferece **Revisar avaliações** (quando há pendência), **Voltar para a Inversão** e **Voltar à Análise**. As respostas da Inversão continuam salvas. Concluir a Inversão não finaliza a decisão; isso só acontece pelo botão **Finalizar decisão** do Resultado (veja "Finalizar uma decisão").
8. Recarregue e confira textos e conclusão. Alterar respostas ou revisar nome/descrição de uma opção exige concluir novamente a Inversão; os textos anteriores permanecem. Salvar a Inversão não altera a conclusão do 10–10–10; revisar uma opção pode exigir nova conclusão de ambas as técnicas.
9. Tente sair com texto não salvo: a navegação deve ser bloqueada. Se um envio falhar sem confirmação, os campos permanecem e o reenvio fica bloqueado até **Recarregar dados**, com confirmação de descarte do rascunho.
10. Uma decisão globalmente concluída permite somente consulta. Nenhuma decisão real precisa ser alterada diretamente no banco para testar.

**Teste visual:** apenas frontend permite conferir cabeçalho e estado de erro, mas a área de respostas depende da API autenticada. **Teste funcional:** carregar as alternativas, salvar, concluir e conferir persistência exige **MySQL + backend + frontend**. Não há novas dependências, variáveis, portas ou comandos de inicialização.

### Swagger e integração

Faça login e use **Authorize**, como explicado no início do guia. As rotas autenticadas são:

| Operação | Rota |
| --- | --- |
| Consultar sem criar registros | GET /decisoes/{decisaoId}/inversao |
| Salvar respostas parciais | PUT /decisoes/{decisaoId}/inversao |
| Salvar quatro respostas e concluir a técnica | POST /decisoes/{decisaoId}/inversao/concluir |

Copie os IDs reais de `opcaoA` e `opcaoB` do GET. Exemplo de corpo:

```json
{
  "opcaoA": { "opcaoId": 21, "fracassos": "Assumir mais tarefas do que consigo cumprir", "prevencoes": "Combinar prioridades e limites antes de aceitar" },
  "opcaoB": { "opcaoId": 22, "fracassos": "Deixar de buscar experiências práticas", "prevencoes": "Participar de projetos e reservar tempo para praticar" }
}
```

PUT aceita null ou texto vazio; campos omitidos limpam a resposta. Sempre envie ambas as opções. POST exige os quatro textos não vazios. As duas alternativas são salvas na mesma transação; reenvios idênticos preservam IDs e data de conclusão.

**200** indica sucesso; **400** indica campos/IDs inválidos, limite excedido ou conclusão incompleta; **401**, falta de autenticação; **404**, decisão inexistente ou de outra conta; **409**, etapa incorreta, decisão concluída para escrita ou registros duplicados que precisam de conferência.

Cada perspectiva é armazenada na tabela existente `inversoes`, com códigos `FRACASSOS` e `PREVENCOES`, vinculada à opção. Outros registros legados são preservados. A resposta inclui `fracassosId`, `fracassos`, `prevencoesId` e `prevencoes` para cada opção, além de `concluida` e `concluidaEm`. Alterações sinalizam resultados existentes para atualização, sem apagar sua pontuação.

A tela de resultado é configurada em `frontend/js/pages/inversao-integracoes.js` (`resultado: "resultado.html"`). O evento cancelável `bussola:inversao-concluida` leva `detail: { decisaoId, etapa: 6 }`; não significa conclusão global da decisão.

### Testes automatizados

No PowerShell, dentro da pasta **backend**, execute:

```powershell
.\mvnw.cmd '-Dtest=InversaoServiceTest,InversaoControllerTest,OpcoesServiceTest' test
```

Aguarde **BUILD SUCCESS**. Esses testes usam repositórios simulados, sem MySQL. Verificam autorização, etapas, salvamento parcial, validação dos quatro campos, IDs preservados, reenvios, revisão, dados legados e CORS. Não substituem o teste de persistência no banco pelo roteiro acima.

## Meu perfil

A página `frontend/pages/perfil.html` permite consultar e editar os dados pessoais, trocar a senha, consultar os documentos legais e solicitar a exclusão da própria conta.

### Iniciar e testar

1. Para conferir apenas o layout, inicie o frontend e abra `http://127.0.0.1:8765/pages/perfil.html`. A tela exige uma sessão válida; sem login, será redirecionado à página de entrada.
2. Para testar as operações, mantenha **MySQL + backend + frontend** ligados, faça login e, na Home, abra **Meu perfil** pelo menu da conta.
3. Edite o nome ou a data de nascimento e use **Salvar dados**. Para testar o cancelamento, altere um campo e escolha **Descartar alterações**. A idade mínima é 16 anos.
4. A troca do e-mail exige a senha atual. Se o novo endereço já estiver cadastrado, a tela informa que não foi possível usá-lo.
5. A alteração de senha exige a senha atual e a confirmação da nova senha. Após a alteração, todas as sessões são encerradas; entre novamente com a senha nova.
6. Os documentos legais abrem diretamente na página. A exclusão da conta exige a senha atual e a confirmação da ação irreversível. Faça esse teste somente com uma conta e decisões descartáveis: os dados da conta são removidos.

**Teste visual:** o frontend sozinho permite conferir o layout, mas consultar e alterar dados exige login e API. **Teste funcional:** salvar dados, trocar senha e excluir a conta exige **MySQL + backend + frontend**. O perfil não acrescenta migration, dependências, variáveis de ambiente ou portas.

### API e testes automatizados

As operações autenticadas também podem ser conferidas no Swagger, com login e **Authorize**:

| Operação | Rota |
| --- | --- |
| Consultar o perfil | GET /perfil |
| Atualizar dados pessoais | PUT /perfil |
| Alterar senha | PUT /perfil/senha |
| Excluir a própria conta | DELETE /perfil |

Para atualizar dados, envie `nomeCompleto`, `dataNascimento` e `email`. A senha atual só é necessária quando o e-mail muda:

```json
{
  "nomeCompleto": "Nome para teste",
  "dataNascimento": "2000-01-01",
  "email": "pessoa@example.invalid",
  "senhaAtual": null
}
```

Para trocar a senha, envie `senhaAtual`, `novaSenha` e `confirmacaoSenha`. Para excluir, envie `senhaAtual` e `confirmar: true`; a operação é definitiva. Use apenas uma conta descartável ao testar a exclusão.

No PowerShell, dentro da pasta **backend**, execute os testes automatizados do perfil:

```powershell
.\mvnw.cmd '-Dtest=PerfilServiceTest,PerfilControllerTest' test
```

Os testes automatizados não usam MySQL e não substituem a conferência funcional da persistência com o banco.
