# Como rodar o Bússola

Este guia explica como configurar o ambiente local e executar o backend do Bússola. Ele descreve somente o que o projeto faz hoje: conectar em um MySQL local, aplicar a estrutura do banco com o Flyway e subir a aplicação Spring Boot.

Ainda não há frontend, endpoints nem autenticação prontos para uso. O que se executa neste momento é o backend.

## Pré-requisitos

Para rodar o backend, instale:

- Git
- MySQL 8
- JDK 25

O Maven não precisa ser instalado à parte. O projeto traz o Maven Wrapper na pasta `backend` (`mvnw` e `mvnw.cmd`).

O projeto foi validado com MySQL 8.0.46, Java 25 e Spring Boot 4.1.1. A aplicação usa MySQL 8; não é obrigatório ter exatamente a versão 8.0.46.

## Como o banco funciona

O banco de dados real não fica dentro do repositório Git. Cada integrante usa o próprio MySQL local durante o desenvolvimento.

A estrutura inicial das tabelas está em:

`backend/src/main/resources/db/migration/V1__Bussola.sql`

O projeto usa Flyway. Quando o backend sobe conectado a um banco `bussola` vazio, o Flyway procura as migrations em:

`classpath:db/migration`

e cria ou atualiza as tabelas necessárias. Não é preciso importar `V1__Bussola.sql` manualmente no MySQL.

O Flyway também cria a tabela `flyway_schema_history`. Ela é gerenciada pelo próprio Flyway e registra quais migrations já foram executadas.

## Criar o banco local

Abra o cliente do MySQL (por exemplo, o terminal do MySQL) e execute somente:

```sql
CREATE DATABASE bussola
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;
```

Esse comando cria apenas o banco vazio. Não crie as 12 tabelas manualmente. Na primeira execução do backend, o Flyway cria as tabelas a partir da migration.

Para conferir o banco antes disso:

```sql
USE bussola;
SHOW TABLES;
```

Antes da primeira execução do backend, é normal o comando `SHOW TABLES` não listar nenhuma tabela.

## Configuração do banco

O backend lê estas variáveis de ambiente:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

A URL local padrão é:

`jdbc:mysql://localhost:3306/bussola`

No PowerShell, na sessão em que você for rodar o projeto:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bussola"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="SUA_SENHA"
```

Substitua `SUA_SENHA` pela senha do seu MySQL local. Não escreva uma senha real neste arquivo nem em outro arquivo do repositório.

Não faça commit de senha. Não coloque credenciais reais em `application.properties`. Não versione um arquivo `.env` com credenciais.

Essas variáveis, definidas assim no PowerShell, valem apenas para aquela janela do terminal. Se fechar o terminal ou abrir outro, configure de novo antes de rodar os testes ou a aplicação.

Se as variáveis não forem definidas, `application.properties` usa estes valores padrão: a URL acima, o usuário `root` e senha vazia. Na maioria das instalações locais do MySQL isso não funciona, porque o usuário `root` tem senha. Por isso o caminho recomendado é definir as três variáveis.

## Java

Confira se o JDK 25 está disponível:

```powershell
java -version
javac -version
```

O projeto está configurado para Java 25.

Se o terminal não encontrar o Java, ou se aparecer outra versão, aponte `JAVA_HOME` para a pasta raiz do JDK 25. Essa pasta é a que contém `bin`, `lib` e os demais diretórios do JDK. Não aponte `JAVA_HOME` para a pasta `bin`.

Exemplo ilustrativo no PowerShell. Troque o caminho pelo local real do JDK na sua máquina:

```powershell
$env:JAVA_HOME="C:\caminho\para\jdk-25"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Essas duas linhas também valem só para a sessão atual do terminal.

## Executar os testes

Na raiz do repositório:

```powershell
cd backend
```

No Windows, com PowerShell, e com `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` já definidos nesse terminal:

```powershell
.\mvnw.cmd test
```

Na primeira execução, com o banco `bussola` vazio, acontece o seguinte:

1. O Spring conecta ao MySQL.
2. O Flyway encontra `V1__Bussola.sql`.
3. O Flyway cria `flyway_schema_history`.
4. O Flyway executa a migration V1.
5. As 12 tabelas do Bússola são criadas.
6. O Hibernate, com `ddl-auto=validate`, confere se as entidades correspondem ao banco. Ele não cria nem altera tabelas.
7. Os testes são executados.

O resultado esperado é:

```text
BUILD SUCCESS
```

## Executar a aplicação

As variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` precisam estar configuradas no mesmo terminal antes de iniciar.

Ainda dentro de `backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Se o banco `bussola` ainda estiver vazio, o Flyway aplica a V1 nesse momento, do mesmo modo que nos testes. O processo que permanece em execução é o backend Spring Boot.

## Como conferir o banco

No MySQL:

```sql
USE bussola;
SHOW TABLES;
```

Depois que a V1 tiver sido aplicada, devem existir estas 12 tabelas da aplicação:

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

Além delas, o Flyway cria:

- `flyway_schema_history`

No total, são 13 tabelas depois da migration inicial.

## Migrations futuras

Depois que `V1__Bussola.sql` já foi aplicada e compartilhada com o grupo, mudanças futuras na estrutura do banco não devem ser feitas editando a V1 de forma arbitrária.

Cada alteração estrutural nova deve virar uma migration nova, no mesmo diretório:

`backend/src/main/resources/db/migration/`

Exemplos de nome:

- `V2__descricao_da_alteracao.sql`
- `V3__descricao_da_alteracao.sql`

Assim, ao iniciar o backend, o Flyway aplica só o que ainda não consta em `flyway_schema_history`. Todos os integrantes atualizam o MySQL local da mesma forma.

## Problemas comuns

### Access denied for user

Em geral, o usuário ou a senha do MySQL estão incorretos, ou `DB_USERNAME` e `DB_PASSWORD` não foram configurados neste terminal. Confira as três variáveis e se a senha é a do seu MySQL local. Não grave essa senha no repositório.

### java não é reconhecido

O terminal não está encontrando o JDK 25. Verifique se o JDK 25 está instalado, se `JAVA_HOME` aponta para a pasta raiz desse JDK e se a pasta `bin` desse JDK está no `PATH`.

### Communications link failure ou Connection refused

O backend não conseguiu falar com o MySQL. Verifique se o serviço do MySQL está iniciado e se ele está escutando na porta usada em `DB_URL`. No exemplo deste guia, essa porta é `3306` em `localhost`.
