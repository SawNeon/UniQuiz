# UniQuiz

Desafios da TI para os colaboradores: LGPD, uso responsável de inteligência artificial, golpes digitais e segurança no dia a dia. A pessoa estuda o conteúdo do módulo, faz lições curtas, ganha pontos e troféus, aparece na classificação e, ao concluir um módulo, emite um certificado em PDF com código de validação.

O repositório tem duas partes:

| Pasta | O que é |
| --- | --- |
| `api/` | API em Java 21 com Spring Boot 3.5, SQL Server e Flyway |
| `web/` | Aplicativo em React com TypeScript, instalável no celular (PWA) |
| `deploy/` | Nginx e Docker Compose para o servidor |

## Como rodar no computador

Você precisa de JDK 21, Maven 3.9, Node 22 e Docker.

1. Copie `.env.example` para `.env` e preencha `UNIQUIZ_DB_SENHA` (senha forte para o SQL Server local) e `UNIQUIZ_ADMIN_SENHA` (no mínimo 12 caracteres).
2. Suba o banco e crie a base `uniquiz`:

   ```bash
   docker compose --env-file .env -f docker-compose.dev.yml up -d
   ```

3. Rode a API no perfil `dev`, que também carrega o conteúdo de exemplo (`db/dev`):

   ```bash
   cd api && UNIQUIZ_DB_SENHA=... UNIQUIZ_ADMIN_SENHA=... mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. Rode o aplicativo web. O Vite repassa `/api` para a porta 8080:

   ```bash
   cd web && npm install && npm run dev
   ```

5. Abra `http://localhost:5173`. Entre como "Ana Exemplo" ou "Bruno Teste" (dados fictícios). O painel fica em `/admin`, com o usuário `admin` e a senha que você definiu.

A documentação da API fica em `http://localhost:8080/api/docs/ui` no perfil `dev`.

### Primeira vez no repositório

O código foi escrito sem rodar os formatadores. Antes do primeiro push, rode:

```bash
cd api && mvn spotless:apply
cd web && npm install && npm run formatar
```

Faça commit do `web/package-lock.json` gerado. O CI usa `npm ci`, que exige esse arquivo.

## Testes

| Onde | Comando | O que cobre |
| --- | --- | --- |
| API | `mvn verify` | Correção de cada tipo de desafio, pontuação, sequência de dias, importação, regras de arquitetura (ArchUnit) e o fluxo completo com SQL Server em Testcontainers (precisa de Docker) |
| Web | `npm run testar` | Componentes de desafio, estado da lição e o seletor de lacunas |
| Ponta a ponta | `npm run testar:e2e` | Entrar, fazer uma lição e ver a classificação. Precisa da API rodando em `dev` |

## Variáveis de ambiente da API

| Variável | Para quê |
| --- | --- |
| `UNIQUIZ_DB_URL`, `UNIQUIZ_DB_USUARIO`, `UNIQUIZ_DB_SENHA` | Conexão com o SQL Server (2016 ou mais novo) |
| `UNIQUIZ_ADMIN_USUARIO`, `UNIQUIZ_ADMIN_SENHA` | Cria o primeiro administrador, só quando ainda não existe nenhum |
| `UNIQUIZ_URL_PUBLICA` | Endereço impresso no certificado para validação |
| `SPRING_PROFILES_ACTIVE` | `prod` no servidor (já é o padrão da imagem Docker) |

Pontuação, limites de requisições e duração da sessão ficam em `api/src/main/resources/application.yml`, na seção `uniquiz`.

## Como o jogo funciona

- **Caminho**: módulos com lições de 5 a 8 desafios. As lições liberam em sequência. Cada módulo pode ter um conteúdo para estudar antes.
- **Tipos de desafio**: múltipla escolha, verdadeiro ou falso e completar clicando. Cada tipo é uma classe em `conteudo/dominio` que valida o próprio conteúdo, gera a versão sem gabarito e corrige a resposta. Um tipo novo não mexe nos outros.
- **Correção**: sempre no servidor. A resposta certa só sai depois que a pessoa responde.
- **Pontos**: pontos do desafio × dificuldade (1; 1,5; 2), bônus a partir do 3º acerto seguido, de lição perfeita e da primeira lição do dia. Os pontos só entram no histórico (`evento_pontos`) quando a lição é concluída, então abandonar e recomeçar não rende pontos extras. Refazer uma lição já concluída vale metade dos pontos uma vez, depois não pontua mais.
- **Segunda chance**: os desafios errados voltam no fim da lição, sem pontos.
- **Sequência de dias**: conta dias úteis. Sábado e domingo não quebram a sequência.
- **Classificação**: top 10 da temporada e a posição de quem está logado, com quantos pontos faltam para subir.
- **Certificado**: ao concluir todas as lições de um módulo, a pessoa gera o PDF. O código de validação pode ser conferido em `/certificado/{codigo}`. O certificado guarda o nome e o título da época em que foi emitido.

## Importar a planilha do RH

No painel, em **Colaboradores**, envie um `.xlsx` ou `.csv` com as colunas `nome`, `setor` e `empresa`. A prévia mostra quem entra, quem é mantido, reativado e inativado. Nomes repetidos no mesmo setor e linhas incompletas bloqueiam a confirmação até serem corrigidos. Ninguém é apagado: quem sai da planilha fica inativo e mantém o histórico.

## Segurança

- O colaborador entra sem senha, escolhendo o próprio nome. Por isso o sistema **só pode ficar na rede interna ou atrás de VPN**. Nunca publique na internet sem essa barreira.
- A sessão fica em cookie `HttpOnly`, `Secure` e `SameSite=Lax`, com o token guardado como hash no banco. A área do admin usa outro cookie, restrito a `/api/admin`.
- O admin entra com senha guardada em BCrypt e é bloqueado por 15 minutos após 5 tentativas erradas.
- A proteção contra CSRF está desligada porque a API só aceita JSON, os cookies são `SameSite=Lax` e o site é servido na mesma origem.
- Há limite de requisições por IP. Na rede interna, muitas máquinas podem sair pelo mesmo IP, então o limite geral é folgado (300 por minuto). Ajuste se precisar.
- O texto do admin é exibido como texto simples ou Markdown limitado, sem HTML.
- A tela de entrada informa o que é guardado e para quê, como pede a LGPD.

## Publicação

1. Gere ou peça à TI um certificado e coloque em `deploy/certs/uniquiz.crt` e `deploy/certs/uniquiz.key`.
2. Copie `deploy/.env.example` para `deploy/.env` e preencha. Use um usuário de banco só com as permissões necessárias, nunca o `sa`.
3. Suba:

   ```bash
   cd deploy && docker compose up -d --build
   ```

Checklist antes de abrir para os colaboradores:

- [ ] HTTPS ativo e o HTTP redirecionando
- [ ] Porta do banco fechada para fora do servidor
- [ ] Acesso restrito à rede da empresa, testado de fora
- [ ] Backup diário do banco, com uma restauração testada
- [ ] Dois administradores cadastrados
- [ ] Primeira temporada criada, com o prêmio

## O que ficou para depois

- Tipos de desafio: completar digitando, ordenar, associar pares, encontrar o erro e caso prático
- Cartões de dica de tecnologia entre as lições
- Troféus "Relâmpago" e "Top 3 da temporada" e o Hall da Fama
- Arrastar e soltar para reordenar o caminho (hoje são botões de subir e descer)
- Ícones PNG para a instalação no celular (hoje só o SVG)
- Fontes oficiais da família Unimed, quando houver licença (hoje Nunito, Roboto Slab e Barlow Condensed)
- Gerar os tipos do front-end a partir da especificação OpenAPI (`npm run gerar:tipos`)
- Migrar para o Spring Boot 4
