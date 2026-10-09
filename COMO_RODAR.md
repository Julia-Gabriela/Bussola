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

| O que abrir | Endereço |
| --- | --- |
| Login | http://127.0.0.1:8765/pages/login.html |
| Cadastro | http://127.0.0.1:8765/pages/cadastro.html |
| Home autenticada | http://127.0.0.1:8765/pages/home.html |
| Nova decisão | http://127.0.0.1:8765/pages/nova-decisao.html |
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

## Testar contextualização e brainstorming pelas telas

Depois de atualizar o projeto, **reinicie o backend no IntelliJ** e atualize o navegador com **Ctrl + F5**. Não precisa criar tabelas nem mudar variáveis ou portas.

1. Faça login e clique em **Iniciar decisão** na Home.
2. Informe o título e, se quiser, o contexto. **Salvar para depois** mantém a decisão na etapa 1.
3. Reabra pela Home, altere o contexto e clique em **Salvar e continuar**. Deve abrir o brainstorming (etapa 2 de 6).
4. Selecione **Favorável**, **Desfavorável** ou **Dúvida**, escreva uma ideia e clique em **Adicionar**.
5. Use **Editar** para mudar texto ou categoria. **Excluir** pede confirmação. Cada ação é salva no banco.
6. Atualize a página ou retorne pela Home: as ideias devem continuar lá. Tanto o cartão da decisão quanto **Continuar** retomam o fluxo salvo.
7. Clique em **Próxima etapa** depois de salvar ou cancelar o texto em edição. A decisão passa para **Opções (etapa 3)**, preservando as ideias e o status **Em andamento**.

A tela de Opções será entregue pela Dupla B; por enquanto aparece a confirmação com as ideias salvas. Depois de avançar, as etapas anteriores ficam apenas para consulta até a entrega da revisão de etapas. Não há quantidade mínima de ideias exigida pela documentação. Texto de ideia é obrigatório ao adicionar/editar; limite técnico: 16.000 caracteres, compatível com a coluna TEXT.

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

Categorias aceitas: `FAVORAVEL`, `DESFAVORAVEL`, `DUVIDA`. Sem login: **401**. Dados inválidos: **400**. Decisão de outra pessoa ou ideia fora da decisão informada: **404**. Etapa incorreta ou decisão concluída: **409**. Repetir a conclusão da etapa imediatamente anterior não avança uma segunda vez.


O login expira após **60 minutos sem requisições autenticadas**. Quando receber 401 por expiração, faça login de novo. O botão **Sair** da Home chama `POST /auth/logout` e remove a sessão local; fechar Authorize no Swagger apenas remove o token da interface.

## Testes automatizados

No PowerShell, entre em `backend` a partir da raiz e execute os testes sem banco:

```powershell
cd backend
.\mvnw.cmd "-Dtest=*Test" test
```

Espere **BUILD SUCCESS**. Esse comando seleciona as classes terminadas em `Test`, incluindo contextualização, brainstorming, nova decisão, autenticação, Home e Swagger. `FluxoDecisaoControllerTest` cobre as novas rotas com serviço real e repositories simulados, sem exigir MySQL.

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
| `401` | Faça login novamente e use o token; no Swagger, clique em Authorize. |
| `409` no cadastro | O e-mail já existe: faça login ou use outro e-mail de teste. |
| `BUILD FAILURE` | Leia o erro; não significa que o backend iniciou. |

**Resumo:** MySQL ligado → ▶ BussolaApplication → `.\rodar-frontend.ps1` na raiz → abrir login.
