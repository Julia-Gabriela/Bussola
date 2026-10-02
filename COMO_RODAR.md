# Como rodar o Bússola

Este guia é para quem vai executar o projeto pela primeira vez.

Se esta é a primeira vez neste computador, siga [Primeira configuração](#primeira-configuração) do começo ao fim.

Se a máquina já foi preparada e você só quer ligar o projeto de novo, vá para [Rodar o Bússola normalmente](#rodar-o-bússola-normalmente).

Se você quer abrir uma tela só, vá para [Quero testar só uma tela. O que preciso rodar?](#quero-testar-só-uma-tela-o-que-preciso-rodar).

## O que eu faço agora?

A ordem prática é esta:

1. Abrir o PowerShell.
2. Ver se o MySQL está ligado.
3. Criar o banco `bussola` numa ferramenta de MySQL, não no PowerShell.
4. Informar usuário e senha no PowerShell.
5. Iniciar o backend e deixar essa janela aberta.
6. Abrir outro PowerShell, iniciar o frontend e deixar essa segunda janela aberta.
7. Abrir o navegador em `http://127.0.0.1:8765/pages/index.html`.

## Antes de começar: PowerShell não é MySQL

PowerShell e MySQL são programas diferentes. O lugar onde você cola o comando muda o resultado.

**PowerShell** é a janela preta ou azul do Windows. Nela você executa comandos do Windows e os comandos que iniciam o projeto.

Exemplos de comandos de PowerShell:

```powershell
Get-Service *mysql*
```

```powershell
Test-NetConnection localhost -Port 3306
```

```powershell
cd backend
```

```powershell
.\mvnw.cmd spring-boot:run
```

**SQL** é a linguagem usada dentro de uma ferramenta já conectada ao MySQL.

Exemplos de SQL:

```sql
CREATE DATABASE bussola;
```

```sql
USE bussola;
```

```sql
SHOW TABLES;
```

```sql
SELECT id, email FROM usuarios;
```

Não digite `USE bussola;`, `SELECT` nem `SHOW TABLES;` diretamente no PowerShell. Esses comandos precisam ser executados na ferramenta conectada ao MySQL. Se você colar SQL no PowerShell, o Windows responde que não reconhece o comando. Isso não significa que o banco quebrou.

## Como copiar um comando

Quando o guia mostrar um comando dentro de uma caixa, copie somente o texto do comando.

Não copie a palavra `powershell`, a palavra `sql`, nem os símbolos que existem só para formatar este arquivo.

Se a caixa mostrar:

```powershell
Get-Service *mysql*
```

copie somente:

```text
Get-Service *mysql*
```

Cole na janela indicada naquele passo e pressione Enter.

## Como o sistema funciona

O Bússola, para abrir no navegador e gravar um cadastro, precisa destas quatro partes ao mesmo tempo:

```text
MySQL
↓
Backend Spring Boot
↓
Frontend
↓
Navegador
```

- **MySQL** guarda os dados.
- **Backend** recebe o que o frontend envia e conversa com o banco.
- **Frontend** contém as páginas que a pessoa vê.
- **Navegador** é onde a pessoa acessa o Bússola. Não é o PowerShell.

Cada parte usa um número, chamado porta. Você não precisa aprender redes. Esse número só serve para saber onde cada parte está funcionando:

- MySQL → porta `3306`
- Backend → porta `8080`
- Frontend → porta `8765`

`localhost` e `127.0.0.1` significam este computador. Não é um site na internet.

## Pré-requisitos

Instale antes de começar:

- Git
- MySQL 8
- JDK 25
- um navegador
- Python 3, usado só para servir as páginas do frontend

O Maven não precisa ser instalado à parte. O projeto traz o Maven Wrapper na pasta `backend` (`mvnw` e `mvnw.cmd`). O comando `.\mvnw.cmd` usa esse Maven do próprio projeto.

O `pom.xml` configura Java 25 e Spring Boot 4.1.1. A aplicação usa MySQL 8. Não é obrigatório ter exatamente uma revisão intermediária, como 8.0.46.

# Primeira configuração

Esta parte prepara a máquina para executar o projeto. Em geral você faz isso uma vez.

Quando terminar e tudo estiver funcionando, nas próximas vezes use só o [Resumo rápido — toda vez que for rodar](#resumo-rápido--toda-vez-que-for-rodar).

## Passo 1 — Abrir o PowerShell

**Onde:** no Windows, não dentro do MySQL.

1. Abra o menu Iniciar.
2. Pesquise por `PowerShell`.
3. Abra **Windows PowerShell**.

Não abra como Administrador agora. Se um comando específico pedir permissão de administrador, o guia avisa naquele momento.

**Como saber que deu certo:** aparece uma linha parecida com `PS C:\Users\...>` e o cursor piscando. Essa linha é o convite para digitar. Ela se chama prompt.

**O que faço depois:** siga para o passo 2, nesta mesma janela.

**Deixo esta janela aberta?** Sim. Você ainda vai usá-la.

## Passo 2 — Verificar o MySQL

**Onde:** no PowerShell que você acabou de abrir.

**O que digitar:**

```powershell
Get-Service *mysql*
```

Um resultado possível é:

```text
Status   Name      DisplayName
------   ----      -----------
Running  MySQL80   MySQL80
```

`MySQL80` é só um exemplo. O nome na sua máquina pode ser outro. Olhe a coluna `Name`.

**Como saber o que fazer:**

- Se apareceu `Running` → o MySQL está ligado. Continue para o passo 3.
- Se apareceu `Stopped` → o MySQL existe, mas está parado. Inicie com o nome real da coluna `Name`. Se a coluna mostrou `MySQL80`, o comando é:

```powershell
Start-Service MySQL80
```

Troque `MySQL80` pelo `Name` que apareceu na sua tela. Se o PowerShell recusar por falta de permissão, feche-o, abra **Windows PowerShell** como Administrador só para este `Start-Service`, e volte ao PowerShell normal em seguida.

- Se nenhum serviço aparecer → pare e vá para [Problemas comuns](#1-get-service-não-encontra-mysql).

**Deixo esta janela aberta?** Sim.

## Passo 3 — Testar a porta 3306

**Onde:** no mesmo PowerShell.

**O que digitar:**

```powershell
Test-NetConnection localhost -Port 3306
```

Vão aparecer várias linhas. Você não precisa entender todas. Procure somente esta:

```text
TcpTestSucceeded : True
```

**Como saber o que fazer:**

- Se apareceu `TcpTestSucceeded : True` → deu certo. Continue.
- Se apareceu `TcpTestSucceeded : False` → pare e vá para [Problemas comuns](#3-tcptestsucceeded-false).

Esse teste não verifica usuário nem senha. Ele só confirma que existe um serviço respondendo na porta `3306`.

**Deixo esta janela aberta?** Sim.

## O comando mysql não é obrigatório

Não use `mysql -u root -p` como etapa obrigatória.

Se você digitar isso e aparecer:

```text
mysql : O termo 'mysql' não é reconhecido...
```

isso não significa que o servidor MySQL está desligado. Significa que o programa cliente `mysql` não está disponível no PowerShell.

Se `Get-Service *mysql*` mostrou `Running` e `TcpTestSucceeded : True`, o servidor MySQL está disponível. Continue. Não pare por causa dessa mensagem.

O comando abaixo é um teste opcional, só para quem já tem o cliente `mysql` configurado:

```powershell
mysql -u root -p
```

O terminal pede a senha. O que você digita pode não aparecer. Isso é normal. Pressione Enter depois de digitar. Para sair desse cliente, digite `exit`.

## Passo 4 — Criar ou conferir o banco

**ATENÇÃO: OS COMANDOS DESTA ETAPA SÃO SQL. NÃO COLE ESTES COMANDOS NO POWERSHELL.**

**Onde:** numa ferramenta conectada ao MySQL. Este projeto não exige uma ferramenta específica. Abra a que você já usa para administrar o MySQL. Se o MySQL Workbench estiver instalado, ele serve. Ele não é obrigatório.

**O que fazer se o banco ainda não existir.** Execute somente:

```sql
CREATE DATABASE bussola
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
```

Isso cria o banco vazio. Não crie tabelas na mão.

**O que fazer se o banco já existir.** Não execute `CREATE DATABASE` de novo. Execute:

```sql
USE bussola;
```

```sql
SHOW TABLES;
```

**Como saber que deu certo:** a ferramenta mostra o banco `bussola`. Se `SHOW TABLES` não listar tabelas, isso é normal antes da primeira subida do backend.

O Flyway, que faz parte do backend, cria as tabelas quando o backend conecta nesse banco vazio. O arquivo usado é `backend/src/main/resources/db/migration/V1__Bussola.sql`. A lista das tabelas está em [Tabelas](#tabelas).

**O que faço depois:** volte para o PowerShell. Deixe a ferramenta do MySQL como está. Você pode fechá-la e abri-la de novo mais tarde para consultar os dados.

**Deixo o PowerShell aberto?** Sim. A próxima etapa é nele.

## Passo 5 — Voltar para o PowerShell e informar a senha

Agora volte para o PowerShell.

**Onde:** no PowerShell que será usado para executar o backend. Se você ainda está na janela dos passos 1 a 3, use essa mesma janela.

**O que digitar,** uma linha de cada vez, pressionando Enter depois de cada uma:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bussola"
```

```powershell
$env:DB_USERNAME="root"
```

```powershell
$env:DB_PASSWORD="SUA_SENHA_DO_MYSQL"
```

O que cada linha significa:

- `DB_URL` é o endereço do banco. Nesta linha ele aponta para o MySQL deste computador, porta `3306`, banco `bussola`.
- `DB_USERNAME` é o usuário usado para acessar o MySQL. No exemplo, `root`.
- `DB_PASSWORD` é a senha desse usuário no MySQL.

`SUA_SENHA_DO_MYSQL` deve ser substituída pela senha real do MySQL daquela máquina. Apague o texto `SUA_SENHA_DO_MYSQL`, inclusive as aspas em volta dele continuam, e coloque a sua senha no lugar. Exemplo de formato, ainda sem senha verdadeira: `$env:DB_PASSWORD="a-senha-deste-computador"`.

Não coloque senha real neste documento. Não compartilhe a senha. Não coloque a senha no Git. Não coloque a senha em `application.properties`. Não faça commit da senha.

O arquivo `application.properties` lê essas três variáveis. Se `DB_URL` não for definida, o padrão é `jdbc:mysql://localhost:3306/bussola`. Se `DB_USERNAME` não for definida, o padrão é `root`. `DB_PASSWORD` não tem valor padrão.

**Como saber que deu certo:** o PowerShell volta ao prompt e não mostra uma mensagem de erro vermelha. A conferência do próximo bloco confirma sem revelar a senha.

**Deixo esta janela aberta?** Sim. É obrigatório. Veja a seção seguinte.

## Conferir sem mostrar a senha

**Onde:** no mesmo PowerShell em que você acabou de definir as três variáveis.

**O que digitar:**

```powershell
$env:DB_URL
```

```powershell
$env:DB_USERNAME
```

```powershell
if ($env:DB_PASSWORD) { "DB_PASSWORD configurada" } else { "DB_PASSWORD não configurada" }
```

**Resultado esperado:**

```text
jdbc:mysql://localhost:3306/bussola
root
DB_PASSWORD configurada
```

Se a última linha for `DB_PASSWORD não configurada`, repita a linha da senha. A senha real precisa estar entre aspas.

Não execute `$env:DB_PASSWORD` sozinho. Esse comando exibiria a senha na tela.

## A mesma janela

As variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` existem somente na janela do PowerShell em que foram digitadas.

Não feche essa janela. É nela que o backend será iniciado.

Se você fechar e abrir outro PowerShell, essas variáveis somem. Será preciso digitá-las de novo antes de `.\mvnw.cmd test` ou `.\mvnw.cmd spring-boot:run`.

## Conferir o Java

**Onde:** no mesmo PowerShell.

**O que digitar:**

```powershell
java -version
```

**Como saber que deu certo:** o texto mostra a versão `25`.

Se aparecer que `java` não é reconhecido, ou se a versão for outra, pare e vá para [Problemas comuns](#13-java-não-é-reconhecido). O backend não inicia sem o JDK 25.

**Deixo esta janela aberta?** Sim.

## Passo 6 — Testar o backend

**Onde:** no mesmo PowerShell, aquele em que a senha foi configurada.

**O que digitar.** Primeiro entre na pasta do backend. Faça isso a partir da pasta do projeto. Se o prompt não estiver na pasta do Bússola, vá até ela com `cd` antes. O caminho muda em cada computador. Estando na pasta do projeto:

```powershell
cd backend
```

Depois:

```powershell
.\mvnw.cmd test
```

A primeira execução pode demorar. Várias linhas vão passar. Você não precisa entender todas. Espere o comando terminar e o prompt voltar.

**Como saber o que fazer:**

- Se no final apareceu `BUILD SUCCESS` → os testes concluíram. Pode continuar.
- Se apareceu `BUILD FAILURE` → pare e vá para [Problemas comuns](#7-build-failure).

Quando dá certo, o Spring conecta ao MySQL, o Flyway confere as migrations e, se o banco estiver vazio, cria as tabelas. O Hibernate só confere se as tabelas batem com o código. Ele não cria tabelas por conta própria.

**Deixo esta janela aberta?** Sim. O próximo comando é nela, ainda dentro da pasta `backend`.

## Passo 7 — Iniciar o backend

**Onde:** ainda no mesmo PowerShell, dentro da pasta `backend`.

**O que digitar:**

```powershell
.\mvnw.cmd spring-boot:run
```

Várias linhas vão aparecer. Você não precisa entender todas. Procure principalmente:

```text
Tomcat started on port 8080
```

e:

```text
Started BussolaApplication
```

**Como saber que deu certo:**

- Se essas duas frases aparecerem → o backend está funcionando.
- Se aparecer `Access denied for user 'root'@'localhost'` → pare e vá para [Problemas comuns](#5-access-denied-for-user). Não abra o navegador esperando gravar cadastro.

O prompt, aquela linha `PS ...>`, não volta enquanto o backend estiver executando. Isso é normal. O programa está ocupando a janela.

Não digite mais comandos nessa janela. Não feche essa janela.

Para parar o backend mais tarde, clique nessa janela e pressione `Ctrl + C`. Só faça isso quando quiser desligar o backend.

Com o backend no ar, a página opcional do Swagger fica em `http://localhost:8080/swagger-ui/index.html`. Ela mostra o endpoint que existe hoje, `POST /auth/cadastro`. Você não precisa abri-la para usar o site.

**O que faço depois:** abra um segundo PowerShell para o frontend. Deixe este primeiro aberto.

# Frontend

## Abrir outro PowerShell

Agora não use o PowerShell do backend. Deixe ele aberto, com o texto `Started BussolaApplication` visível.

**Onde:** uma segunda janela do PowerShell. Abra pelo menu Iniciar, como no passo 1. Não feche a primeira.

**O que digitar.** Na segunda janela, estando na pasta do projeto:

```powershell
.\rodar-frontend.ps1
```

Esse script encontra a pasta `frontend` a partir do próprio arquivo. Não depende de um caminho fixo do computador.

**Como saber que deu certo:** o terminal mostra primeiro a página inicial:

```text
Página inicial:
http://127.0.0.1:8765/pages/index.html
```

e, em seguida, os links de Cadastro, Sobre e Home provisória. Escolha o endereço que deseja testar e abra no navegador. Não cole a URL no PowerShell.

### Forma manual

Se preferir não usar o script, ainda dá para subir o frontend na mão. Estando na pasta do projeto:

```powershell
cd frontend
```

```powershell
python -m http.server 8765
```

O Python pode mostrar só:

```text
Serving HTTP on :: port 8765 (http://[::]:8765/) ...
```

Esse endereço não é a página do Bússola. Para abrir a página inicial, use no navegador:

```text
http://127.0.0.1:8765/pages/index.html
```

Não digite mais comandos nessa janela. Não feche essa janela.

Para parar o frontend mais tarde, clique nessa segunda janela e pressione `Ctrl + C`.

Não abra os arquivos HTML dando dois cliques, no endereço `file:///`. A página de cadastro precisa falar com o backend por HTTP.

## O que deve estar aberto agora?

Confira antes de abrir o navegador.

**Janela 1 — backend**

Está executando:

```powershell
.\mvnw.cmd spring-boot:run
```

Status esperado no texto da janela:

```text
Tomcat started on port 8080
Started BussolaApplication
```

Não fechar.

**Janela 2 — frontend**

Está executando:

```powershell
.\rodar-frontend.ps1
```

Status esperado: o texto `Página inicial:` seguido de `http://127.0.0.1:8765/pages/index.html`. O Python também pode mostrar `Serving HTTP on :: port 8765`. Essa linha do Python não substitui o link da página inicial.

Não fechar.

**MySQL**

O serviço está `Running`. Você já conferiu isso com `Get-Service *mysql*`.

**Navegador**

É a próxima etapa. É onde o Bússola será aberto. Não digite o endereço do site no PowerShell.

# Abrir o Bússola

**Onde:** no navegador. Não é no PowerShell.

1. Abra o navegador.
2. Clique na barra de endereço, a faixa onde normalmente aparece o site.
3. Digite:

```text
http://127.0.0.1:8765/pages/index.html
```

4. Pressione Enter.

Essa é a entrada normal atual do Bússola. A página é a Landing, o arquivo `frontend/pages/index.html`.

**Como saber que deu certo:** a página inicial do Bússola aparece. Você vê o texto de uma decisão que não precisa ser tomada no impulso, e o botão do cabeçalho.

**Deixo as janelas do PowerShell abertas?** Sim. As duas.

A entrada normal é a Landing, `http://127.0.0.1:8765/pages/index.html`. No estado atual do protótipo, os dois chamados da Landing levam para `cadastro.html`: **Entre ou cadastre-se** e **Começar uma decisão**. Ainda não existe login. **Começar uma decisão** ainda não inicia uma decisão. Os dois só abrem o cadastro.

O cadastro, quando a pessoa já está em `cadastro.html`, foi testado no navegador e funcionou: o formulário envia `POST /auth/cadastro`, o backend grava no MySQL e a resposta `201` abre a Home provisória. O botão **Sair** dessa Home volta para `index.html`. Ele não faz logout, porque não há autenticação. Quando login e o fluxo de decisão existirem, esses cliques precisam ser revistos.

Para abrir o formulário sem passar pela Landing, use o endereço direto de teste, explicado em [Exemplo: quero testar somente a tela de cadastro](#exemplo-quero-testar-somente-a-tela-de-cadastro).

A página Sobre, se quiser abri-la direto, é `http://127.0.0.1:8765/pages/sobre.html`.

# Testar o cadastro

Este teste precisa do MySQL, do backend e do frontend ao mesmo tempo. O endereço direto do formulário, na barra do navegador, é:

```text
http://127.0.0.1:8765/pages/cadastro.html
```

Esse endereço não é a entrada do sistema. A entrada continua sendo `index.html`.

Preencha nome completo, data de nascimento, e-mail, senha e o aceite dos Termos de Uso e da Política de Privacidade. Depois clique em **Criar conta gratuita**.

O caminho atual do protótipo, já testado no navegador até a Home, é:

```text
index.html
↓
Entre ou cadastre-se ou Começar uma decisão
↓
cadastro.html
↓
POST /auth/cadastro
↓
backend
↓
MySQL
↓
201 Created
↓
home.html provisória
↓
Sair
↓
index.html
```

`home.html` ainda não é a Home definitiva. Ela existe para validar esse caminho. O botão **Sair** não faz logout no backend, porque ainda não existe login de usuário. Ele só volta para `index.html`. Quando uma autenticação real existir, esse botão precisa ser revisto.

O que a tela faz com a resposta:

- `201 Created` → o cadastro foi aceito e o navegador abre `home.html`.
- `400` → os dados são inválidos. A pessoa permanece no cadastro.
- `409` → o e-mail já está cadastrado. A pessoa permanece no cadastro.
- falha de conexão → a pessoa permanece no cadastro e vê "Não foi possível concluir o cadastro. Tente novamente."

O frontend em `http://127.0.0.1:8765` e o backend em `http://localhost:8080` são origens diferentes para o navegador. O `SecurityConfig` já permite essas origens locais no `POST /auth/cadastro`. Os detalhes estão em [CORS](#cors).

# Confirmar o cadastro no banco

**ATENÇÃO: OS COMANDOS ABAIXO SÃO SQL. NÃO COLE NO POWERSHELL.**

**Onde:** na ferramenta usada para administrar o MySQL, a mesma do passo 4.

```sql
USE bussola;
```

```sql
SELECT
    id,
    nome_completo,
    data_nascimento,
    email,
    aceite_termos_em
FROM usuarios
ORDER BY id DESC;
```

**Como saber que deu certo:** se o cadastro acabou de ser realizado com sucesso, a pessoa aparece entre os registros mais recentes, no topo dessa lista.

A consulta não pede a senha. A senha digitada no formulário não deve aparecer em texto puro no banco. O backend gera um hash com BCrypt e grava esse hash na coluna `senha_hash`. O aceite dos termos não vira um texto de "sim". O backend preenche `aceite_termos_em` com a data e a hora do aceite.

Se a lista estiver vazia, o `201` não aconteceu. Volte ao teste do cadastro. Não conclua que salvou só porque a página mudou sem a resposta `201`.

# Rodar o Bússola normalmente

Para usar o projeto pelo fluxo normal, estas quatro partes precisam estar disponíveis ao mesmo tempo:

```text
MySQL
↓
Backend
↓
Frontend
↓
Navegador
```

1. O MySQL precisa estar `Running`. No PowerShell: `Get-Service *mysql*`.
2. No PowerShell que será usado para o backend, configure o banco. Troque `SUA_SENHA_DO_MYSQL` pela senha real do MySQL desta máquina:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bussola"
```

```powershell
$env:DB_USERNAME="root"
```

```powershell
$env:DB_PASSWORD="SUA_SENHA_DO_MYSQL"
```

3. Entre na pasta do backend, a partir da pasta do projeto:

```powershell
cd backend
```

4. Inicie:

```powershell
.\mvnw.cmd spring-boot:run
```

5. Espere aparecer `Tomcat started on port 8080` e `Started BussolaApplication`.
6. Não feche esse terminal. O prompt não volta. Isso é normal. Para parar depois, use `Ctrl + C` nessa janela.
7. Abra outro PowerShell. Deixe o do backend aberto.
8. Na pasta do projeto, inicie o frontend:

```powershell
.\rodar-frontend.ps1
```

9. Espere o terminal mostrar a página inicial `http://127.0.0.1:8765/pages/index.html`.
10. Não feche esse segundo terminal. Para parar depois, use `Ctrl + C` nele.
11. Abra o navegador, não o PowerShell, e acesse:

```text
http://127.0.0.1:8765/pages/index.html
```

Essa é a entrada normal do sistema. Na Landing, **Entre ou cadastre-se** e **Começar uma decisão** levam para `cadastro.html`. Um cadastro aceito abre `home.html`. O botão **Sair** dessa Home volta para `index.html`.

Não abra o HTML por `file:///`. O script entrega as páginas por HTTP na porta `8765`. O endereço `http://127.0.0.1:8765/...` é o que vai na barra do navegador. SQL, como `SELECT`, fica na ferramenta do MySQL. São três lugares diferentes. A forma manual, `cd frontend` e `python -m http.server 8765`, está na seção do frontend.

# Quero testar só uma tela. O que preciso rodar?

Depende do que a tela faz.

## Caso 1 — Tela que não precisa do backend

Algumas telas são HTML, CSS e JavaScript de aparência. Elas não enviam nem buscam dados na API. Para vê-las, normalmente basta o frontend.

Abra um PowerShell na pasta do projeto e execute:

```powershell
.\rodar-frontend.ps1
```

Deixe o terminal aberto. Procure a linha da página inicial, `http://127.0.0.1:8765/pages/index.html`.

### Quero testar só o visual do cadastro

1. Inicie o frontend com `.\rodar-frontend.ps1`.
2. No navegador, abra `http://127.0.0.1:8765/pages/cadastro.html`.

Se for apenas conferir layout, isso pode ser suficiente. O backend não precisa estar ligado para a página aparecer.

### Quero testar o cadastro funcionando

1. O MySQL precisa estar `Running`.
2. O backend precisa estar rodando na porta `8080`.
3. O frontend precisa estar rodando na porta `8765`.
4. No navegador, abra `http://127.0.0.1:8765/pages/cadastro.html`.
5. Preencha o formulário.
6. Clique em **Criar conta gratuita**.
7. O frontend chama `POST /auth/cadastro`.

Depois abra a página no navegador. Estas páginas existem hoje e não chamam a API para aparecer:

- Landing: `http://127.0.0.1:8765/pages/index.html`
- Sobre: `http://127.0.0.1:8765/pages/sobre.html`

Se o teste for só aparência, responsividade, textos ou layout, e a página não chamar a API, não é necessário subir o backend só para vê-la. Isso vale para estas telas atuais. Não vale automaticamente para uma tela futura.

## Caso 2 — Tela que usa o backend

Se a tela envia, salva, busca, altera ou exclui dados pela API, abrir o HTML não basta. É preciso:

```text
MySQL
+
Backend
+
Frontend
```

## Exemplo: quero testar somente a tela de cadastro

Mesmo abrindo `cadastro.html` direto, o botão de criar conta conversa com `POST http://localhost:8080/auth/cadastro`. Por isso o MySQL, o backend e o frontend precisam estar ligados.

1. No PowerShell, confira o MySQL:

```powershell
Get-Service *mysql*
```

Se apareceu `Running`, continue. Se não, veja [Problemas comuns](#problemas-comuns).

2. No PowerShell que será usado para o backend:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bussola"
```

```powershell
$env:DB_USERNAME="root"
```

```powershell
$env:DB_PASSWORD="SUA_SENHA_DO_MYSQL"
```

3. Entre no backend, a partir da pasta do projeto:

```powershell
cd backend
```

4. Rode:

```powershell
.\mvnw.cmd spring-boot:run
```

5. Espere `Tomcat started on port 8080` e `Started BussolaApplication`.
6. Deixe essa janela aberta.
7. Abra outro PowerShell.
8. Na pasta do projeto, inicie o frontend:

```powershell
.\rodar-frontend.ps1
```

9. Espere a página inicial aparecer no texto do terminal.
10. Deixe essa segunda janela aberta.
11. Agora abra direto no navegador:

```text
http://127.0.0.1:8765/pages/cadastro.html
```

Aqui a Landing é pulada de propósito, porque o teste é o cadastro. Isso não transforma `cadastro.html` na página inicial. A página inicial continua sendo:

```text
http://127.0.0.1:8765/pages/index.html
```

## Teste visual e teste funcional

Há dois jeitos de olhar uma tela.

**Teste visual:** layout, cores, responsividade, espaçamento e textos. Se a tela não depende de dados do backend para aparecer, o frontend basta.

**Teste funcional:** criar cadastro, salvar, buscar, editar, excluir ou enviar um formulário para a API. Se a função depende da API, o backend precisa estar rodando. Se essa operação usa o banco, o MySQL também precisa estar rodando.

No cadastro, ver o formulário pode ser só com o frontend. Clicar em **Criar conta** e gravar a pessoa exige MySQL, backend e frontend.

| O que quero testar | MySQL | Backend | Frontend |
| --- | --- | --- | --- |
| Landing visual | Não | Não | Sim |
| Sobre visual | Não | Não | Sim |
| Cadastro apenas visual | Não | Não | Sim |
| Cadastro funcionando de verdade | Sim | Sim | Sim |

A Landing e a página Sobre atuais não carregam dados da API. O formulário de cadastro aparece sem a API. O envio desse formulário chama `POST /auth/cadastro`.

## Como abrir uma página específica

Com o frontend iniciado por `.\rodar-frontend.ps1`, os arquivos de `frontend/pages/` abrem no navegador assim:

```text
http://127.0.0.1:8765/pages/NOME-DO-ARQUIVO.html
```

Exemplos que existem hoje:

- `index.html`: `http://127.0.0.1:8765/pages/index.html`
- `sobre.html`: `http://127.0.0.1:8765/pages/sobre.html`
- `cadastro.html`: `http://127.0.0.1:8765/pages/cadastro.html`
- `home.html`: `http://127.0.0.1:8765/pages/home.html`

Isso serve para desenvolvimento e teste. No fluxo normal do protótipo, a pessoa entra por `index.html`. **Entre ou cadastre-se** e **Começar uma decisão** levam para `cadastro.html`. O botão **Sair** da Home provisória volta para `index.html`. **Sobre o projeto** continua levando para `sobre.html`. **Início** continua levando para `index.html`.

Não abra as páginas por `file:///`. Use o servidor HTTP acima e o endereço `http://127.0.0.1:8765/...`. Assim o teste fica no mesmo tipo de endereço que as telas usam para conversar com a API.

# Resumo rápido — toda vez que for rodar

Três caminhos. Não misture comando, endereço e SQL.

**Quero rodar o sistema completo**

```text
MySQL Running
↓
backend :8080
↓
frontend :8765
↓
abrir index.html
```

O endereço, no navegador, é `http://127.0.0.1:8765/pages/index.html`.

**Quero testar só uma tela visual**

```text
frontend :8765
↓
abrir diretamente a URL da tela
```

**Quero testar uma funcionalidade que usa API**

```text
MySQL, se a operação usar banco
+
backend
+
frontend
↓
abrir diretamente a tela que quero testar
```

Exemplo de cadastro funcionando de verdade: MySQL, backend e frontend, e no navegador `http://127.0.0.1:8765/pages/cadastro.html`. Isso não é a página inicial.

Isto é comando de PowerShell, na pasta do projeto: `.\rodar-frontend.ps1`.

Isto é endereço para o navegador: `http://127.0.0.1:8765/pages/index.html`.

Isto é SQL para a ferramenta do MySQL: `SELECT id, email FROM usuarios;`.

Os passos numerados abaixo ligam o sistema completo quando o banco `bussola` já existe.

Use esta lista quando a primeira configuração já tiver sido feita. O banco `bussola` já existe. Você já sabe a senha do MySQL.

Se algum "apareceu?" for não, vá para [Problemas comuns](#problemas-comuns). Não continue no escuro.

## Passo 1 — MySQL

**Onde:** um PowerShell novo.

```powershell
Get-Service *mysql*
```

Apareceu `Running`?

- Sim → continue.
- Não → veja [Problemas comuns](#problemas-comuns).

## Passo 2 — Backend

**Onde:** na janela que será usada para o backend. Pode ser a mesma do passo 1.

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bussola"
```

```powershell
$env:DB_USERNAME="root"
```

```powershell
$env:DB_PASSWORD="SUA_SENHA_DO_MYSQL"
```

Troque `SUA_SENHA_DO_MYSQL` pela senha real do seu MySQL. Não execute `$env:DB_PASSWORD` sozinho.

Estando na pasta do projeto:

```powershell
cd backend
```

```powershell
.\mvnw.cmd spring-boot:run
```

Procure `Tomcat started on port 8080` e `Started BussolaApplication`.

Apareceu?

- Sim → o backend está funcionando. Deixe essa janela aberta. Não digite mais nada nela.
- Não → veja [Problemas comuns](#problemas-comuns).

## Passo 3 — Frontend

**Onde:** outro PowerShell. Não use a janela do backend.

Estando na pasta do projeto:

```powershell
.\rodar-frontend.ps1
```

Procure `Página inicial:` e o endereço `http://127.0.0.1:8765/pages/index.html`.

Apareceu?

- Sim → o frontend está funcionando. Deixe essa janela aberta.
- Não → veja [Problemas comuns](#9-frontend-não-abre).

## Passo 4 — Navegador

**Onde:** no navegador. Não digite a URL no PowerShell.

Acesse:

```text
http://127.0.0.1:8765/pages/index.html
```

Pronto. Essa é a página inicial. O endereço de `cadastro.html` não é a entrada do sistema.

# Detalhes que não fazem parte do liga-e-desliga

Leia esta parte quando quiser entender o que o projeto guarda. Para ligar o sistema no dia a dia, o resumo rápido basta.

## Tabelas

**Onde:** na ferramenta do MySQL, não no PowerShell.

```sql
USE bussola;
```

```sql
SHOW TABLES;
```

Depois que a migration V1 tiver sido aplicada, devem existir estas 12 tabelas da aplicação:

- `usuarios`
- `decisoes`
- `opcoes`
- `criterios`
- `avaliacoes`
- `ideias_brainstorming`
- `itens_gut`
- `pros_contras`
- `reflexoes_101010`
- `inversoes`
- `resultados`
- `tokens_recuperacao_senha`

Além delas, o Flyway cria `flyway_schema_history`. No total, são 13 tabelas depois da migration inicial.

## Migrations futuras

Depois que `V1__Bussola.sql` já foi aplicada e compartilhada com o grupo, não altere esse arquivo para representar uma mudança nova de estrutura.

Uma migration já compartilhada e aplicada não deve ser simplesmente modificada. A alteração nova vira um arquivo novo em `backend/src/main/resources/db/migration/`, por exemplo `V2__descricao_da_alteracao.sql` ou `V3__descricao_da_alteracao.sql`.

Na próxima subida, o Flyway aplica só o que ainda não está em `flyway_schema_history`. Cada integrante atualiza o próprio MySQL local desse modo. O banco não fica dentro do Git.

## CORS

O frontend local e o backend usam origens diferentes. A página fica em `http://127.0.0.1:8765` ou `http://localhost:8765`. A API fica em `http://localhost:8080`. Para o navegador, isso é outro endereço, então ele só aceita a resposta se o backend autorizar.

`backend/src/main/java/br/com/bussola/security/SecurityConfig.java` já faz essa autorização no caminho `POST /auth/cadastro`. As origens permitidas são exatamente:

- `http://127.0.0.1:8765`
- `http://localhost:8765`

Os métodos permitidos nesse caminho são `POST` e `OPTIONS`. Os cabeçalhos permitidos são `Content-Type` e `Accept`. Não há permissão para qualquer origem.

O mesmo arquivo continua liberando `POST /auth/cadastro` e a consulta do Swagger sem login, e ignora o CSRF só nesse cadastro. O cadastro pela tela, com o frontend na porta `8765` e o backend na porta `8080`, foi testado no navegador e funcionou.

Se a porta ou o endereço do frontend mudar, essa lista no `SecurityConfig` precisa ser atualizada. Sem isso, o DevTools pode mostrar de novo que a resposta CORS foi bloqueada.

## Problemas comuns

Cada item é um problema diferente. Leia o sintoma antes de trocar a senha ou recriar o banco.

### 1. Get-Service não encontra MySQL

**O que significa:** o PowerShell não achou um serviço com `mysql` no nome. O MySQL pode não estar instalado, ou o serviço pode ter outro nome.

**O que conferir:** a instalação do MySQL 8 neste computador.

**Onde:** no PowerShell, com `Get-Service *mysql*`. A janela Serviços do Windows (`Win + R`, depois `services.msc`) também lista serviços, se quiser procurar visualmente.

**Quando continuar:** só depois que um serviço do MySQL aparecer e estiver `Running`. Isso é diferente de um serviço encontrado com status `Stopped`.

### 2. MySQL aparece Stopped

**O que significa:** o MySQL está instalado e parado.

**O que conferir:** a coluna `Name`.

**Onde:** no PowerShell, `Start-Service` com esse nome. Se faltar permissão, use o PowerShell como Administrador só nesse comando.

**Quando continuar:** quando um novo `Get-Service *mysql*` mostrar `Running`, e o teste da porta `3306` mostrar `True`.

### 3. TcpTestSucceeded False

**O que significa:** nada aceitou conexão na porta `3306`. O serviço pode estar parado, o MySQL pode escutar outra porta, ou a instalação pode estar incompleta.

**O que conferir:** o status do serviço, antes de mexer em senha.

**Onde:** no PowerShell, `Get-Service *mysql*` e de novo `Test-NetConnection localhost -Port 3306`.

**Quando continuar:** quando aparecer `TcpTestSucceeded : True`. Esse teste continua sem dizer se a senha está certa.

### 4. mysql não é reconhecido

**O que significa:** o cliente de linha de comando não está disponível no PowerShell. O servidor pode estar `Running` mesmo assim.

**O que conferir:** `Get-Service *mysql*` e a porta `3306`.

**Onde:** no PowerShell. Não é preciso corrigir o `PATH` para subir o Bússola.

**Quando continuar:** se o serviço está `Running` e `TcpTestSucceeded` é `True`. Use a ferramenta gráfica para o SQL.

### 5. Access denied for user

**O que significa:** o MySQL foi encontrado, mas recusou o usuário ou a senha. A mensagem típica é `Access denied for user 'root'@'localhost' (using password: YES)`.

**O que conferir:** `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` na mesma janela do backend. A senha tem de ser a senha real daquele MySQL, não o texto de exemplo.

**Onde:** no PowerShell do backend. Feche o processo com `Ctrl + C` se ele ainda estiver na tela, defina as três variáveis de novo e rode `.\mvnw.cmd spring-boot:run` outra vez. Não grave a senha no repositório.

**Quando continuar:** quando aparecerem `Tomcat started on port 8080` e `Started BussolaApplication`. Antes disso o Spring não termina, a porta `8080` não fica disponível, o navegador pode mostrar "Não foi possível concluir o cadastro. Tente novamente." e nenhum usuário é salvo.

### 6. Banco bussola não existe

**O que significa:** o MySQL aceitou a autenticação e não encontrou o banco. O log costuma citar `Unknown database`.

**O que conferir:** se o `CREATE DATABASE bussola` foi feito.

**Onde:** na ferramenta do MySQL, com o SQL do passo 4. Não cole esse SQL no PowerShell.

**Quando continuar:** depois de criar o banco vazio e subir o backend de novo, até ver `Started BussolaApplication`.

### 7. BUILD FAILURE

**O que significa:** `.\mvnw.cmd test` não chegou a `BUILD SUCCESS`.

**O que conferir:** as últimas linhas do log. Muitas vezes é o item 5, o item 6 ou o item 13.

**Onde:** no PowerShell do backend.

**Quando continuar:** quando uma nova execução terminar com `BUILD SUCCESS`.

### 8. Backend não permanece na porta 8080

**O que significa:** `http://localhost:8080` não responde porque o processo encerrou durante a inicialização. `Tomcat initialized` sem `Tomcat started` e sem `Started BussolaApplication` não conta como backend pronto.

**O que conferir:** o texto da janela 1, em especial `Access denied` ou `Unknown database`.

**Onde:** no PowerShell em que você rodou `.\mvnw.cmd spring-boot:run`.

**Quando continuar:** quando as duas frases de sucesso estiverem na tela e a janela continuar aberta, sem voltar ao prompt.

### 9. Frontend não abre

**O que significa:** o navegador não carrega a página.

**O que conferir:** a janela 2 ainda está aberta, com `Página inicial:` ou `Serving HTTP on :: port 8765`? O endereço na barra do navegador é `http://127.0.0.1:8765/pages/index.html`, e não o texto `http://[::]:8765/` que o Python imprime?

**Onde:** segundo PowerShell para o servidor, navegador para o endereço. Página que não abre é diferente de página que abre e falha ao chamar a API.

**Quando continuar:** quando a Landing aparecer.

### 10. Failed to fetch

**O que significa:** o navegador não completou a chamada ao backend. Na tela, a mensagem é "Não foi possível concluir o cadastro. Tente novamente."

**O que conferir:** a janela 1 ainda está em `Started BussolaApplication`? A página foi aberta pelo endereço `8765`, não por `file:///`? Se o backend estiver no ar e o erro continuar, veja CORS.

**Onde:** janela do backend, janela do frontend e a barra de endereço do navegador.

**Quando continuar:** quando a resposta for `201`. Até lá, não assuma que um usuário novo foi salvo.

### 11. CORS

**O que significa:** o navegador bloqueou a resposta porque a página e a API estão em origens diferentes e os cabeçalhos CORS não bateram. No DevTools, a mensagem fala em CORS, preflight ou `Ensure CORS response header values are valid`.

**O que conferir:** a página está em `http://127.0.0.1:8765` ou `http://localhost:8765`? O backend que está no ar já inclui a configuração atual do `SecurityConfig`? Essas duas origens, o método `POST` e os cabeçalhos `Content-Type` e `Accept` são os que o código permite em `/auth/cadastro`. Se o frontend for servido em outra porta, essa lista precisa ser atualizada no backend. Isso é diferente de `Access denied`, em que o MySQL recusa a senha e o backend nem permanece no ar.

**Onde:** no código `SecurityConfig.java` e no endereço da barra do navegador. Não se corrige trocando um comando SQL.

**Quando continuar:** quando o cadastro pela tela receber `201` e abrir `home.html`. Com as origens acima e o backend reiniciado depois da configuração, esse caminho já foi testado e funcionou.

### 12. 409 e-mail já cadastrado

**O que significa:** a API respondeu. Aquele e-mail já existe na tabela `usuarios`. O backend estava no ar e recusou a duplicata. A pessoa permanece no cadastro.

**O que conferir:** a consulta SQL em [Confirmar o cadastro no banco](#confirmar-o-cadastro-no-banco), na ferramenta do MySQL.

**Onde:** para um teste novo, use outro e-mail no formulário do navegador.

**Quando continuar:** o sistema está respondendo. Não trate o `409` como falha de conexão.

### 13. Java não é reconhecido

**O que significa:** o PowerShell não encontrou o JDK 25.

**O que conferir:** se o JDK 25 está instalado. `JAVA_HOME` deve apontar para a pasta raiz desse JDK, a que contém `bin` e `lib`. Não aponte `JAVA_HOME` para a pasta `bin`.

**Onde:** no PowerShell, nesta sessão:

```powershell
$env:JAVA_HOME="C:\caminho\para\jdk-25"
```

```powershell
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Troque `C:\caminho\para\jdk-25` pela pasta real do JDK na sua máquina. Essas duas linhas valem só para essa janela.

**Quando continuar:** quando `java -version` mostrar a versão `25`.
