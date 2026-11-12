# UniQuiz

Desafios da TI para os colaboradores: LGPD, uso responsável de inteligência artificial, golpes digitais e segurança no dia a dia. A pessoa estuda o conteúdo do módulo, faz lições curtas, ganha pontos e troféus, aparece na classificação e, ao concluir um módulo, emite um certificado em PDF com código de validação.

O repositório tem duas partes:

| Pasta | O que é |
| --- | --- |
| `api/` | API em Java 21 com Spring Boot 3.5, SQL Server e Flyway |
| `web/` | Aplicativo em React com TypeScript, instalável no celular (PWA) |
| `deploy/` | Nginx e Docker Compose para o servidor |

## Como rodar no computador

Você precisa de JDK 21, Node 22 ou mais novo e Docker. O Maven vem pelo wrapper (`./mvnw`, ou `mvnw.cmd` no Windows), que baixa a versão certa na primeira vez.

1. Copie `.env.example` para `.env` e preencha `UNIQUIZ_DB_SENHA` (senha forte para o SQL Server local) e `UNIQUIZ_ADMIN_SENHA` (no mínimo 12 caracteres).
2. Suba o banco e crie a base `uniquiz`:

   ```bash
   docker compose --env-file .env -f docker-compose.dev.yml up -d
   ```

3. Rode a API no perfil `dev`, que também carrega o conteúdo de exemplo (`db/dev`):

   ```bash
   cd api && UNIQUIZ_DB_SENHA=... UNIQUIZ_ADMIN_SENHA=... ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. Rode o aplicativo web. O Vite repassa `/api` para a porta 8080:

   ```bash
   cd web && npm install && npm run dev
   ```

5. Abra `http://localhost:5173`. Entre como "Ana Exemplo" ou "Bruno Teste" (dados fictícios): no primeiro acesso a pessoa cria um PIN de 3 números, e nas próximas vezes digita o mesmo PIN. O painel fica em `/admin`, com o usuário `admin` e a senha que você definiu.

No painel, em Colaboradores, dá para cadastrar uma pessoa avulsa (nome, empresa, setor e, se quiser, o PIN de 3 números; sem PIN, a pessoa cria o dela no primeiro acesso), importar a planilha do RH e redefinir o PIN de quem esqueceu (a pessoa cria outro no próximo acesso). Tudo fica no SQL Server; o PIN é guardado só como hash, e 5 erros seguidos bloqueiam a pessoa por 15 minutos.

A documentação da API fica em `http://localhost:8080/api/docs/ui` no perfil `dev`.

### Formatação

O CI recusa código fora do padrão. Antes de commitar, rode `./mvnw spotless:apply` em `api/` e `npm run formatar` em `web/`.

### Dependências com alerta conhecido

`npm audit` aponta um alerta moderado no `deepmerge`, que vem do `workbox-build` usado pelo plugin do PWA. Ele só roda durante o build e não vai para o navegador; ainda não há versão corrigida. `npm audit --omit=dev` fica limpo.

O npm 11 bloqueia scripts de instalação por padrão. O do `esbuild` não é necessário: o build funciona sem ele.

## Testes

| Onde | Comando | O que cobre |
| --- | --- | --- |
| API, domínio | `./mvnw test` | Correção de cada tipo de desafio, pontuação, sequência de dias, troféus, importação, PDF do certificado, limite de requisições e regras de arquitetura (ArchUnit). Roda sem banco |
| API, integração | `./mvnw verify` | Sobe um SQL Server real com Testcontainers e testa pela API: segurança e sessões, lição completa com segunda chance e pontos, certificado, classificação, conteúdo do admin e importação. Precisa de Docker; sem Docker esses testes são pulados |
| Web | `npm run cobertura` | Todas as telas do colaborador e do admin com a API simulada, mais componentes e funções. Falha se a cobertura cair abaixo de 90% das linhas ou 85% dos ramos |
| Ponta a ponta | `npm run testar:e2e` | No navegador de verdade (celular e computador): entrar, fazer uma lição e ver a classificação. Precisa do banco e da API rodando em `dev` |

Os testes de integração compartilham um único SQL Server e voltam ao conteúdo de exemplo antes de cada teste, então podem rodar em qualquer ordem.

### Testes de carga

A pasta `carga/` tem uma ferramenta em Node puro (sem dependências) que simula muita gente usando ao mesmo tempo: entrar com PIN, fazer a lição inteira com sorteio e ver a classificação. Ela cria dados fictícios com o prefixo "Carga" (pessoas, uma empresa, um setor, um módulo e um banco de desafios), mede latência (p50, p95, p99), erros e vazão por operação, e confere se cada lição sorteou a quantidade certa e foi corrigida como certa.

```bash
UNIQUIZ_ADMIN_SENHA=... node carga/executar.mjs --base=https://servidor --insegura --usuarios=500 --cenarios=
UNIQUIZ_ADMIN_SENHA=... node carga/executar.mjs --base=https://servidor --insegura --reutilizar --usuarios=500 --cenarios=licao
```

| Cenário | O que faz |
| --- | --- |
| `leitura` | Quem abre o app lê empresas, setores e nomes, com N conexões durante `--duracao` segundos |
| `login` | N pessoas entram com PIN no mesmo instante |
| `licao` | N pessoas fazem a lição inteira; `--rampa` espalha a chegada e `--pensar=min,max` simula o tempo de resposta em milissegundos |
| `continuo` | N pessoas repetem lições durante `--duracao` segundos |
| `limite` e `limite-falso` | Passam do limite de requisições, sem e com um `X-Forwarded-For` forjado |
| `forca-bruta` | Chuta PINs em paralelo contra uma pessoa para conferir que o bloqueio vale de verdade |

Todo o tráfego sai de um único IP, e a API limita cada IP a 300 requisições por minuto. Para medir a capacidade, suba esse limite só durante o teste (`UNIQUIZ_LIMITE_REQUISICOES_GERAL_POR_MINUTO`) e volte ao padrão depois. Os dados "Carga" precisam ser apagados do banco ao terminar. Não rode contra a produção.

## Variáveis de ambiente da API

| Variável | Para quê |
| --- | --- |
| `UNIQUIZ_DB_URL`, `UNIQUIZ_DB_USUARIO`, `UNIQUIZ_DB_SENHA` | Conexão com o SQL Server (2016 ou mais novo) |
| `UNIQUIZ_ADMIN_USUARIO`, `UNIQUIZ_ADMIN_SENHA` | Cria o primeiro administrador, só quando ainda não existe nenhum |
| `UNIQUIZ_URL_PUBLICA` | Endereço impresso no certificado para validação |
| `SPRING_PROFILES_ACTIVE` | `prod` no servidor (já é o padrão da imagem Docker) |

Pontuação, limites de requisições e duração da sessão ficam em `api/src/main/resources/application.yml`, na seção `uniquiz`.

## Como o jogo funciona

- **Caminho**: módulos com lições, que liberam em sequência. Cada módulo pode ter um conteúdo para estudar antes.
- **Sorteio de desafios**: ao criar a lição, o admin escolhe quantos desafios de cada nível (fácil, médio, difícil) cada pessoa responde, de 0 a 30 por nível. O banco da lição pode ter mais desafios publicados do que isso: a cada tentativa o sistema sorteia a quantidade pedida de cada nível e embaralha a ordem, e as alternativas da múltipla escolha também mudam de lugar. Assim ninguém decora a sequência das respostas. Como todos respondem a mesma quantidade por nível, a pontuação e a classificação continuam justas. Para publicar, o banco publicado de cada nível precisa ter pelo menos a quantidade sorteada, e tirar do ar um desafio que deixaria o banco menor que isso é recusado. Cada tentativa guarda o que sorteou, e só esses desafios aceitam resposta nela. Lições que já existiam passam a sortear todos os seus desafios publicados, como antes.
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
