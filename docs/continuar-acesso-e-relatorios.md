# Continuação: acesso seguro e relatórios do painel

Branch: `feat/acesso-e-relatorios` (parte de `feat/pin-sorteio-carga`). Ainda sem PR.

## Onde paramos

O pedido foi montar relatórios e auditoria no painel admin, mas antes "um sistema bom de acesso", nesta ordem:

| Etapa | O quê | Situação |
|---|---|---|
| 1 | Perfis do painel e gestão de usuários | Pronta (`a27574b`) |
| 2 | Segurança do login, sessões e tela de auditoria | Pronta (`a367e35`) |
| 3 | Entrada do colaborador | Pronta (`e818ffe`) |
| 4 | Relatórios respeitando os perfis | **A fazer** |

A KRATOS (homologação) ainda não recebeu nada disso: tudo foi verificado só localmente.

## O que já existe

**Perfis** (`api/.../acesso/dominio/PerfilDoPainel.java`)

| Perfil | Permissões |
|---|---|
| Administrador | todas |
| Gestor | `VER_RELATORIOS`, `EXPORTAR_RELATORIOS`, limitado aos setores atribuídos (`EscopoDeSetores`) |
| Auditor | `VER_RELATORIOS`, `EXPORTAR_RELATORIOS`, `VER_AUDITORIA` |

- As rotas exigem permissões (`PERM_*`) em `acesso/web/ConfiguracaoDeSeguranca.java`. Qualquer rota `/api/admin/**` não listada é negada: **ao criar os relatórios, incluir a regra de `/api/admin/relatorios/**`** (`VER_RELATORIOS`, e `EXPORTAR_RELATORIOS` para exportação).
- `UsuarioLogado` traz perfil, permissões e `escopo()` (setores do gestor). Senha provisória pendente zera as permissões.
- Migrações novas: `V5__registro_de_auditoria.sql`, `V6__perfis_do_painel.sql`, `V7__sessoes_detalhadas.sql`.

**Auditoria** (`api/.../auditoria/`)
- Tabela `registro_de_auditoria` só aceita inclusão (gatilho no banco).
- `InterceptadorDeAuditoria` registra todo POST/PUT/PATCH/DELETE em `/api/admin/**` usando `CatalogoDeRotas`. Toda rota nova que altera dados precisa entrar no catálogo (o teste `AcessoDoPainelTest` falha se faltar).
- Exportação de relatório deve registrar `AcaoAuditada.RELATORIO_EXPORTADO` (já existe no enum) via `ServicoDeAuditoria.registrar`.
- Consultas de leitura usam `JdbcClient` + `record` (ver `auditoria/persistencia/ConsultasDeAuditoria.java`). Datas são gravadas em UTC: passar `LocalDateTime` em UTC como parâmetro.

**Web** (`web/src/recursos/admin/`)
- `painel.ts`: seções do menu com permissão, `useContaDoPainel()`, `pode()`, `formatarMomento()`.
- `rotas.tsx`: `telaProtegida(caminho, permissao, carregar)` protege cada tela.
- Telas novas: `TelaDeUsuarios`, `TelaDeMinhaConta`, `TelaDeAuditoria`; entrada do colaborador em `recursos/acesso/TelaDeEntrada.tsx`.

## Etapa 4: relatórios (plano aprovado)

1. **Conformidade**: visão geral, por setor e por pessoa (quem concluiu cada módulo/lição, quem não começou).
2. **Exportação CSV/Excel**: escapar células que começam com `=`, `+`, `-`, `@` (injeção de fórmula) e registrar `RELATORIO_EXPORTADO` na auditoria.
3. **Desempenho e conteúdo**: acerto na 1ª resposta, lições perfeitas, abandono, alternativa errada mais escolhida, calibração da dificuldade (acerto real × nível).
4. **Engajamento semanal**.
5. Toda consulta aplica o `EscopoDeSetores`: o gestor só vê os próprios setores.
6. Não há biblioteca de gráficos no web: usar barras em CSS/SVG.
7. Ao terminar, a seção "Relatórios" entra em `SECOES_DO_PAINEL` (antes de Auditoria) com `VER_RELATORIOS`, e vira a primeira tela do Gestor e do Auditor.

## Pendências pequenas

- O Administrador ainda não vê a lista de sessões de outro usuário (só "Encerrar sessões").
- O registro de "Criou um usuário do painel" não guarda o login criado como alvo.
- Os testes de integração novos (`AcessoDoPainelTest`, `SessoesEAuditoriaTest`, `EntradaDoColaboradorTest`) e o `PinTest` ajustado **ainda não rodaram**: precisam de Docker (Testcontainers). Rodar numa máquina com Docker ou conferir no CI.

## Como rodar em outra máquina

1. `git fetch && git switch feat/acesso-e-relatorios`
2. Copiar `.env.example` para `.env` e preencher `UNIQUIZ_DB_SENHA` e `UNIQUIZ_ADMIN_SENHA` (nunca commitar).
3. SQL Server local na porta 1433 com o banco `uniquiz`: com Docker, `docker compose --env-file .env -f docker-compose.dev.yml up -d` (ver README). A API em perfil `dev` aplica as migrações (V5 a V7 entram como *out of order* se o banco já estava na V1001). Com Docker na máquina, os testes de integração também rodam.
4. API: `cd api` e `./mvnw spring-boot:run` com `SPRING_PROFILES_ACTIVE=dev` e as variáveis do `.env` (a senha do banco vem de `UNIQUIZ_DB_SENHA`; não deixar senha escrita no `application-dev.yml`).
5. Web: `cd web`, `npm install`, `npm run dev` (porta 5173, com proxy para a API).
6. Verificações: `./mvnw spotless:check test` na API; `npm run lint`, `npx tsc --noEmit`, `npm run cobertura`, `npm run build` no web.

No perfil dev o admin inicial não é obrigado a trocar a senha; em produção é (`UNIQUIZ_ADMIN_EXIGIR_TROCA_DE_SENHA`, padrão `true`).

## Combinados

- Commits sem menção ao Claude.
- Não guardar CPF de ninguém.
- Não commitar senhas (`.env`, `.env.example` preenchido, `application-dev.yml` com senha) nem o `descricao-do-pr.md`.
- Código sem comentários, seguindo os padrões já usados no projeto.
- KRATOS só quando for pedido.
