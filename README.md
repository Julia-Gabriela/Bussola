<p align="center">
  <img src="docs/readme/titulo-bussola.svg" alt="Logo Bússola" width="480"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-E0B860?style=for-the-badge&logo=openjdk&logoColor=E0B860&labelColor=17120D" alt="Java 25"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.1-E0B860?style=for-the-badge&logo=springboot&logoColor=E0B860&labelColor=17120D" alt="Spring Boot 4.1.1"/>
  <img src="https://img.shields.io/badge/MySQL-8-E0B860?style=for-the-badge&logo=mysql&logoColor=E0B860&labelColor=17120D" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/HTML%20·%20CSS%20·%20JS-vanilla-E0B860?style=for-the-badge&logo=javascript&logoColor=E0B860&labelColor=17120D" alt="HTML, CSS e JavaScript vanilla"/>
  <img src="https://img.shields.io/badge/Projeto-acadêmico-E0B860?style=for-the-badge&labelColor=17120D" alt="Projeto acadêmico"/>
</p>

O **Bússola** é uma aplicação web de apoio à tomada de decisão. Ele conduz a pessoa por um processo guiado de reflexão para comparar duas alternativas, organizando ideias, critérios, prioridades e consequências antes da escolha final.

O projeto nasceu de uma dificuldade comum entre jovens adultos, principalmente entre **18 e 28 anos**: decisões acadêmicas, profissionais ou pessoais envolvem muitas possibilidades, incertezas e fatores racionais e emocionais ao mesmo tempo. Listas soltas, planilhas e conselhos ajudam, mas raramente oferecem um caminho estruturado.

A proposta do Bússola é justamente **orientar, e não decidir**. O sistema não aponta a "resposta certa": ele reúne técnicas de análise em um único fluxo, mostra como cada alternativa se saiu nos critérios que a própria pessoa definiu e preserva a autonomia sobre a decisão final. O uso é permitido a partir de 16 anos, e a ferramenta não substitui acompanhamento psicológico ou profissional.

---

![Funcionalidades](https://img.shields.io/badge/🧭%20Funcionalidades-E0B860?style=for-the-badge)

**Conta e acesso**
- **Cadastro** com nome completo, data de nascimento (idade mínima de 16 anos), e-mail, senha e aceite dos **Termos de Uso** e da **Política de Privacidade**
- **Login e logout**, com sessão encerrada após 60 minutos de inatividade
- **Meu perfil:** consultar e editar dados pessoais, trocar o e-mail (mediante senha atual), trocar a senha e excluir a própria conta com senha e confirmação
- **Home** com o total de decisões concluídas e em andamento, decisões recentes e atalho para continuar de onde parou

**Fluxo guiado de decisão, em seis etapas**
1. **Contextualização:** título e contexto da decisão
2. **Brainstorming:** ideias organizadas em *Ponto positivo*, *Ponto negativo* e *Dúvida*, com edição e exclusão
3. **Opções:** definição das duas alternativas comparadas
4. **Critérios:** notas de 1 a 5 para cada alternativa em cinco critérios predefinidos (*Segurança*, *Crescimento*, *Risco*, *Impacto emocional* e *Alinhamento a valores*), além de **critérios personalizados**
5. **Pesos:** importância de 1 a 5 para cada critério
6. **Análise**, com quatro técnicas:
   - **Prós e Contras** de cada alternativa
   - **Matriz GUT** (Gravidade × Urgência × Tendência) aplicada aos próprios prós e contras, para priorizar o que mais pesa
   - **Reflexão 10/10/10:** como cada escolha será vista daqui a 10 minutos, 10 meses e 10 anos
   - **Inversão:** o que poderia dar errado em cada alternativa e como evitar

**Resultado e acompanhamento**
- **Resultado Comparativo** com pontuação ponderada (nota × peso), nota de *Risco* invertida, percentuais e os critérios que mais influenciaram. Empates não geram recomendação automática
- **Revisão de etapas anteriores** enquanto a decisão está em andamento, com confirmação e preservação dos dados já preenchidos
- **Salvar para depois** e retomada pela Home ou pelo Histórico
- **Finalização com confirmação:** a decisão passa a *Concluída* e fica disponível para consulta somente leitura
- **Histórico** com busca, filtro por status, ordenação, paginação e exclusão de decisões mediante confirmação
- **Painel de acessibilidade** (detalhes mais abaixo)

---

![Protótipo](https://img.shields.io/badge/🎨%20Protótipo%20no%20Figma-E0B860?style=for-the-badge)

Antes da implementação, o Bússola passou por uma etapa de **pesquisa, planejamento e prototipação de interfaces** no Figma. O protótipo definiu a identidade cartográfica do projeto, a jornada pelas etapas da decisão e a apresentação do resultado.

🔗 [Visualizar o protótipo no Figma](https://www.figma.com)

> O Figma registra o processo de design. A aplicação implementada evoluiu a partir dele, com ajustes de usabilidade, acessibilidade e fluxo, por isso pode apresentar diferenças em relação ao protótipo.

**Identidade visual:** tons escuros de couro e papel envelhecido, com detalhes dourados que remetem ao corpo e à agulha da bússola. As interfaces usam elementos de mapa, rotas tracejadas e rosa dos ventos.

| Cor | Hex | Uso no projeto |
| --- | --- | --- |
| ![#17120D](https://img.shields.io/badge/%20-17120D?style=flat-square) | `#17120D` | Fundo principal |
| ![#211B14](https://img.shields.io/badge/%20-211B14?style=flat-square) | `#211B14` | Superfícies e painéis |
| ![#E0B860](https://img.shields.io/badge/%20-E0B860?style=flat-square) | `#E0B860` | Dourado de destaque |
| ![#D4AD68](https://img.shields.io/badge/%20-D4AD68?style=flat-square) | `#D4AD68` | Dourado de acento e bordas |
| ![#C4B090](https://img.shields.io/badge/%20-C4B090?style=flat-square) | `#C4B090` | Bege dos textos de apoio |
| ![#EEE9DF](https://img.shields.io/badge/%20-EEE9DF?style=flat-square) | `#EEE9DF` | Texto sobre fundo escuro |

Tipografia: **Georgia** nos títulos e **Inter** (ou Segoe UI) nos textos de interface.

---

![Tecnologias](https://img.shields.io/badge/🛠️%20Tecnologias%20Utilizadas-E0B860?style=for-the-badge)

- **Front-end:** HTML5, CSS3 e JavaScript vanilla, sem framework, servidos como páginas estáticas
- **Back-end:** Java 25 e Spring Boot 4.1.1 (Spring Web MVC, Spring Security e Jakarta Validation)
- **Autenticação:** JWT assinado com HS256 (Spring Security OAuth2 JOSE) e senhas com BCrypt
- **Banco de dados:** MySQL 8
- **Persistência:** Spring Data JPA, Hibernate e MySQL Connector/J
- **Migrations:** Flyway, com sete versões do esquema (`V1` a `V7`)
- **Documentação da API:** springdoc-openapi com Swagger UI
- **Testes:** JUnit Jupiter, Mockito, Spring Boot Test e MockMvc
- **Prototipação:** Figma
- **Ferramentas de desenvolvimento:** Maven Wrapper, Lombok, Git e GitHub. O frontend é servido localmente com o servidor HTTP do Python

---

![Segurança](https://img.shields.io/badge/🔒%20Segurança%20dos%20Dados-E0B860?style=for-the-badge)

Os cuidados abaixo estão implementados no código do projeto:

- **Senhas** são armazenadas apenas como hash **BCrypt** (fator de custo 10), nunca em texto puro.
- **Autenticação** por token JWT Bearer, sem cookies de autenticação nem sessão HTTP no servidor. Cada login gera uma sessão registrada no banco, que expira após **60 minutos de inatividade**.
- **Rotas protegidas:** somente cadastro, login e a documentação da API são públicos. As demais exigem um token válido.
- **Controle de acesso por usuário:** as decisões são sempre buscadas pelo identificador da decisão **e** pelo usuário autenticado, então uma conta não acessa as decisões de outra.
- **Validação** dos dados de entrada no servidor, com mensagens genéricas para credenciais inválidas (e-mail inexistente e senha incorreta recebem a mesma resposta).
- **Troca de senha** encerra todas as sessões abertas da conta. A troca de e-mail exige a senha atual.
- **Exclusão de conta** pela própria interface, com senha atual e confirmação explícita. A conta e seus registros associados são removidos.
- **CORS** restrito às origens locais do frontend durante o desenvolvimento.
- **Segredos fora do código:** a senha do banco e a chave JWT são lidas de variáveis de ambiente.

Essas medidas reduzem riscos, mas o projeto foi validado apenas em ambiente local. Uma implantação pública ainda exigiria HTTPS e uma revisão de segurança específica.

---

![Acessibilidade](https://img.shields.io/badge/♿%20Acessibilidade-E0B860?style=for-the-badge)

Todas as páginas têm o botão **Abrir preferências de acessibilidade** no cabeçalho. Ele abre um painel com:

- **Tamanho do texto** de 100% a 150%, em passos de 10%
- **Alto contraste**, mantendo as cores do Bússola
- **Redução de animações**, respeitando também a preferência do sistema operacional (`prefers-reduced-motion`)
- **Restaurar padrão**

As preferências ficam salvas **apenas neste navegador** e são aplicadas antes da página aparecer, sem piscar. Elas não são enviadas ao servidor.

Além do painel:
- **Navegação completa por teclado**, com o link *Pular para o conteúdo principal* e **foco sempre visível**
- **Mensagens de erro acessíveis:** o campo inválido é anunciado, a mensagem aparece logo abaixo dele e o foco é levado até ele, sem perder o que já foi digitado
- **Avisos de status** anunciados por leitores de tela (`role="status"`), como confirmações de salvamento
- **Resultado compreensível sem gráficos:** pontuações, percentuais e influências também aparecem em texto
- **Responsividade** de 320 px até telas grandes, inclusive com zoom de 200% e texto ampliado, e **alvos de toque** com pelo menos 44 px

O fluxo completo de decisão foi percorrido somente pelo teclado, e as auditorias automáticas com axe-core e Lighthouse não apontaram problemas. O objetivo é atender às diretrizes WCAG 2.2 nível AA, mas o projeto **não possui certificação** e ainda falta validar com leitores de tela reais e dispositivos móveis físicos.

---

![Como rodar o projeto](https://img.shields.io/badge/⚙️%20Como%20Rodar%20o%20Projeto-E0B860?style=for-the-badge)

O Bússola tem três partes que precisam estar ligadas ao mesmo tempo:

| Parte | Tecnologia | Porta |
| --- | --- | --- |
| Banco de dados | MySQL | `3306` |
| Backend (API) | Java + Spring Boot | `8080` |
| Frontend | Páginas estáticas servidas pelo Python | `8765` |

Os comandos abaixo usam o **PowerShell do Windows**, ambiente em que o projeto foi desenvolvido e testado. O guia completo, com solução de problemas comuns, está em [`COMO_RODAR.md`](./COMO_RODAR.md).

**Pré-requisitos**

- **JDK 25** (confira com `java -version`)
- **MySQL Server 8**
- **Python 3**, usado só para servir o frontend
- **Git**
- O **Maven não precisa ser instalado**: o projeto traz o Maven Wrapper (`backend\mvnw.cmd`), que baixa o Maven na primeira execução

1. **Clone o repositório:**
   ```powershell
   git clone https://github.com/Julia-Gabriela/Bussola.git
   cd Bussola
   ```

2. **Crie o banco de dados** (só na primeira vez), no MySQL Workbench ou em outro cliente MySQL:
   ```sql
   CREATE DATABASE IF NOT EXISTS bussola CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
   Não é preciso criar tabelas: o **Flyway** aplica as migrations automaticamente quando o backend inicia.

3. **Gere a chave JWT** (uma vez por computador). O bloco abaixo cria uma chave aleatória e a salva no usuário do Windows, sem exibi-la:
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
   Depois, feche e abra o terminal para que ele reconheça a variável.

4. **Inicie o backend** em um primeiro terminal. A senha do MySQL é digitada de forma oculta e vale só para esse terminal:
   ```powershell
   cd backend
   $senhaBanco = Read-Host "Senha do MySQL" -AsSecureString
   $env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $senhaBanco).Password
   .\mvnw.cmd spring-boot:run
   ```
   Funcionou quando aparecer `Started BussolaApplication`. **Deixe esse terminal aberto.**

   Por padrão, o backend usa o banco `bussola` em `localhost:3306` com o usuário `root`. Se o seu ambiente for diferente, defina também `DB_URL` e `DB_USERNAME` antes de iniciar.

5. **Inicie o frontend** em um segundo terminal, na pasta raiz do projeto:
   ```powershell
   .\rodar-frontend.ps1
   ```
   Se o PowerShell bloquear o script, use o comando equivalente:
   ```powershell
   python -m http.server 8765 --directory frontend
   ```

6. **Acesse no navegador:**
   ```
   http://127.0.0.1:8765/pages/index.html
   ```
   Crie uma conta em **Cadastro** e entre no sistema. Abra sempre pelo endereço com a porta `8765`, porque abrir o HTML com duplo clique não funciona com a API.

**Documentação da API (Swagger):** com o backend ligado, acesse `http://localhost:8080/swagger-ui/index.html`.

**Testes automatizados:** na pasta `backend`, execute:
```powershell
.\mvnw.cmd test
```
A maioria dos testes usa repositórios simulados e não precisa do MySQL.

> Nunca coloque senhas ou chaves reais no código, no Git ou em capturas de tela.

---

![Arquitetura](https://img.shields.io/badge/🏗️%20Arquitetura-E0B860?style=for-the-badge)

O frontend e o backend são **aplicações separadas** que se comunicam por uma **API REST** em JSON.

```text
Navegador (HTML, CSS, JS)  ──HTTP + JWT Bearer──▶  API Spring Boot  ──JPA──▶  MySQL
        frontend/                                      backend/            (Flyway)
```

**Frontend** (`frontend/`)
- `pages/`: as 17 páginas da aplicação, das públicas (início, sobre, login e cadastro) às etapas da decisão, ao resultado, ao histórico e ao perfil
- `js/pages/`: o script de cada página, responsável pela interação e pelas chamadas à API
- `js/shared/`: módulos compartilhados, como cabeçalho, autenticação, revisão de etapas e acessibilidade
- `style/` e `assets/`: estilos e recursos visuais

**Backend** (`backend/src/main/java/br/com/bussola/`), organizado em camadas:
- `controller`: recebe as requisições HTTP e devolve as respostas
- `service`: concentra as regras de negócio, como avanço entre etapas, revisão, cálculo do resultado e finalização
- `repository`: acesso aos dados com Spring Data JPA
- `model`: entidades JPA e enums
- `dto`: formatos de entrada e saída da API, separados das entidades
- `security` e `config`: autenticação JWT, controle de sessões, CORS e configurações
- `exception`: tratamento centralizado das respostas de erro

A estrutura do banco é versionada pelas migrations do Flyway em `backend/src/main/resources/db/migration`.

---

![Equipe](https://img.shields.io/badge/👩‍💻%20Equipe-E0B860?style=for-the-badge)

Projeto desenvolvido em grupo por estudantes de Ciência da Computação:

- [Júlia Gabriela Gomes da Silva](https://github.com/Julia-Gabriela)
- [Lara Ewellen de Carvalho Rocha](https://github.com/Laraewellen)
- [Luiza Lima de Sá](https://github.com/luizalima13)
- [Maria Eduarda Nunes Bezerra](https://github.com/marianunx)

📌 Repositório original da equipe: [Julia-Gabriela/Bussola](https://github.com/Julia-Gabriela/Bussola)

---

![Licença](https://img.shields.io/badge/📜%20Licença-E0B860?style=for-the-badge)

O repositório **não possui um arquivo de licença**. O Bússola é um **projeto acadêmico**, e qualquer uso, cópia ou redistribuição deve ser combinado previamente com as integrantes da equipe.
