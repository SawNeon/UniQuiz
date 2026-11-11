INSERT INTO empresa (nome) VALUES (N'Unimed Exemplo');

INSERT INTO setor (empresa_id, nome)
SELECT e.id, s.nome
FROM empresa e
CROSS JOIN (VALUES (N'Tecnologia da Informação'), (N'Faturamento')) AS s (nome)
WHERE e.nome = N'Unimed Exemplo';

INSERT INTO colaborador (empresa_id, setor_id, nome, nome_normalizado, ativo, criado_em)
SELECT s.empresa_id, s.id, c.nome, c.nome_normalizado, 1, SYSUTCDATETIME()
FROM setor s
JOIN (VALUES
    (N'Tecnologia da Informação', N'Ana Exemplo', N'ANA EXEMPLO'),
    (N'Tecnologia da Informação', N'Bruno Teste', N'BRUNO TESTE'),
    (N'Faturamento', N'Carla Demonstração', N'CARLA DEMONSTRACAO')
) AS c (setor, nome, nome_normalizado) ON c.setor = s.nome;

INSERT INTO temporada (nome, inicio, fim, premio_texto)
VALUES (
    N'Temporada de exemplo',
    DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1),
    EOMONTH(GETDATE()),
    N'Destaque no mural da TI'
);

INSERT INTO modulo (titulo, descricao, material_estudo, ordem, status) VALUES
(
    N'LGPD na prática',
    N'O que a lei protege, quem é quem e o que fazer no dia a dia.',
    N'# LGPD na prática

A Lei Geral de Proteção de Dados (Lei 13.709/2018) protege os **dados pessoais** de pessoas físicas, em qualquer meio: sistema, planilha, papel ou conversa.

## Quem é quem

- **Titular**: a pessoa a quem os dados se referem. Paciente, beneficiário, colaborador.
- **Controlador**: quem decide como os dados são usados. Aqui, a cooperativa.
- **Operador**: quem trata os dados em nome do controlador, como um fornecedor.
- **Encarregado**: a pessoa que cuida da proteção de dados e fala com a ANPD.

## Dado pessoal e dado sensível

Dado pessoal é qualquer informação que identifica alguém: nome, CPF, telefone, número da carteirinha.

Dado sensível pede ainda mais cuidado. Inclui **saúde**, origem racial, religião, opinião política, vida sexual, dado genético e biometria. Numa operadora de saúde, ele aparece o tempo todo.

## No dia a dia

- Use os dados só para a finalidade do seu trabalho.
- Recebeu um dado por engano? Não encaminhe. Avise o encarregado.
- Não tire foto ou captura de tela de dados de pacientes.
- Bloqueie o computador ao sair da mesa (Windows + L).',
    1,
    N'PUBLICADO'
),
(
    N'IA com responsabilidade',
    N'Como usar ferramentas de inteligência artificial sem expor dados nem errar.',
    N'# IA com responsabilidade

Ferramentas de inteligência artificial ajudam a escrever, resumir e organizar ideias. Elas também trazem riscos novos.

## O que entra pode sair

Tudo o que você cola numa ferramenta de IA pode ficar guardado fora da empresa. Por isso:

- **Nunca** cole nomes, CPF, dados de pacientes ou informações internas.
- Use apenas as ferramentas **aprovadas pela TI**.
- Na dúvida, descreva o problema de forma genérica.

## A IA erra com confiança

A IA pode inventar fatos, números e referências, e escrever tudo com muita segurança. Confira sempre com a fonte original.

## Quem assina é você

A IA ajuda a ganhar tempo, mas a responsabilidade pelo que é enviado continua sendo de quem envia. Revise antes de usar em documentos, e-mails e relatórios.',
    2,
    N'PUBLICADO'
);

INSERT INTO licao (modulo_id, titulo, ordem, status)
SELECT id, N'O básico da LGPD', 1, N'PUBLICADO' FROM modulo WHERE titulo = N'LGPD na prática';

INSERT INTO licao (modulo_id, titulo, ordem, status)
SELECT id, N'IA no trabalho', 1, N'PUBLICADO' FROM modulo WHERE titulo = N'IA com responsabilidade';

INSERT INTO desafio (licao_id, tipo, enunciado, dificuldade, pontos, explicacao, dica, conteudo_json, versao, ordem, status)
SELECT l.id, d.tipo, d.enunciado, d.dificuldade, 10, d.explicacao, NULL, d.conteudo_json, 1, d.ordem, N'PUBLICADO'
FROM licao l
JOIN (VALUES
    (1, N'MULTIPLA_ESCOLHA', N'O que a LGPD protege?', N'FACIL',
     N'A Lei 13.709/2018 protege os dados pessoais de pessoas físicas, em qualquer meio.',
     N'{"alternativas":[{"id":"a","texto":"Os dados pessoais de pessoas físicas","correta":true,"motivo":""},{"id":"b","texto":"Apenas os dados das empresas","correta":false,"motivo":"A LGPD trata de dados de pessoas, como colaboradores, pacientes e beneficiários."},{"id":"c","texto":"Só documentos em papel","correta":false,"motivo":"A lei vale para dados em qualquer meio, digital ou físico."}]}'),
    (2, N'VERDADEIRO_OU_FALSO', N'Verdadeiro ou falso?', N'FACIL',
     N'Dado sensível inclui saúde, origem racial, religião e biometria, entre outros. Numa operadora de saúde, ele aparece todo dia.',
     N'{"afirmacao":"Dados de saúde são dados pessoais sensíveis pela LGPD.","verdadeira":true,"motivoDoErro":"A LGPD classifica dados de saúde como sensíveis, com regras mais rígidas de uso."}'),
    (3, N'COMPLETAR_CLICANDO', N'Complete a frase.', N'MEDIO',
     N'Titular é a pessoa dona dos dados: o paciente, o beneficiário, o colaborador.',
     N'{"texto":"A pessoa a quem os dados se referem é chamada de {{1}}.","lacunas":[{"id":1,"banco":[{"palavra":"titular","correta":true,"motivo":""},{"palavra":"controlador","correta":false,"motivo":"O controlador é quem decide como os dados são usados, como a cooperativa."},{"palavra":"operador","correta":false,"motivo":"O operador trata os dados em nome do controlador, como um fornecedor."}]}]}'),
    (4, N'MULTIPLA_ESCOLHA', N'Você recebeu por engano uma planilha com dados de pacientes de outro setor. O que fazer?', N'MEDIO',
     N'Dado recebido por engano é um incidente. O encarregado orienta o que fazer e avalia se é preciso comunicar a ANPD.',
     N'{"alternativas":[{"id":"a","texto":"Avisar o encarregado de dados e seguir a orientação dele","correta":true,"motivo":""},{"id":"b","texto":"Encaminhar para o setor certo e seguir o trabalho","correta":false,"motivo":"Encaminhar espalha o dado ainda mais. Primeiro avise quem cuida da proteção de dados."},{"id":"c","texto":"Guardar a planilha, pode ser útil depois","correta":false,"motivo":"Guardar dados que não são do seu trabalho aumenta o risco de vazamento."}]}'),
    (5, N'VERDADEIRO_OU_FALSO', N'Verdadeiro ou falso?', N'DIFICIL',
     N'Pela LGPD, o dado é usado para uma finalidade específica, informada ao titular.',
     N'{"afirmacao":"Se o paciente autorizou o uso dos dados uma vez, a empresa pode usá-los para qualquer finalidade.","verdadeira":false,"motivoDoErro":"O uso precisa seguir a finalidade informada. Uma finalidade nova exige uma base legal própria."}')
) AS d (ordem, tipo, enunciado, dificuldade, explicacao, conteudo_json) ON 1 = 1
WHERE l.titulo = N'O básico da LGPD';

INSERT INTO desafio (licao_id, tipo, enunciado, dificuldade, pontos, explicacao, dica, conteudo_json, versao, ordem, status)
SELECT l.id, d.tipo, d.enunciado, d.dificuldade, 10, d.explicacao, NULL, d.conteudo_json, 1, d.ordem, N'PUBLICADO'
FROM licao l
JOIN (VALUES
    (1, N'MULTIPLA_ESCOLHA', N'Antes de colar um texto numa ferramenta de IA, o que você deve fazer?', N'FACIL',
     N'O que entra numa ferramenta de IA pode ficar guardado fora da empresa.',
     N'{"alternativas":[{"id":"a","texto":"Tirar nomes, CPF e dados de pacientes","correta":true,"motivo":""},{"id":"b","texto":"Corrigir os erros de digitação","correta":false,"motivo":"Erro de digitação não expõe ninguém. O risco está no que o texto revela sobre as pessoas."},{"id":"c","texto":"Traduzir o texto para o inglês","correta":false,"motivo":"O idioma não muda o risco. Os dados pessoais continuam expostos."}]}'),
    (2, N'VERDADEIRO_OU_FALSO', N'Verdadeiro ou falso?', N'FACIL',
     N'Revise tudo o que a IA produz antes de usar, principalmente em documentos oficiais.',
     N'{"afirmacao":"A resposta de uma IA pode estar errada mesmo parecendo confiante.","verdadeira":true,"motivoDoErro":"Ferramentas de IA podem inventar fatos, números e referências. Sempre confira."}'),
    (3, N'COMPLETAR_CLICANDO', N'Complete a frase.', N'MEDIO',
     N'Antes de liberar uma ferramenta, a TI avalia onde os dados ficam guardados e quem tem acesso a eles.',
     N'{"texto":"Use apenas ferramentas de IA {{1}} pela TI.","lacunas":[{"id":1,"banco":[{"palavra":"aprovadas","correta":true,"motivo":""},{"palavra":"gratuitas","correta":false,"motivo":"Ser gratuita não garante que a ferramenta proteja os dados da empresa."},{"palavra":"populares","correta":false,"motivo":"Ser conhecida não quer dizer que a ferramenta é segura para dados da empresa."}]}]}'),
    (4, N'MULTIPLA_ESCOLHA', N'Uma IA resumiu um processo interno para você enviar à diretoria. O que fazer?', N'MEDIO',
     N'A IA ajuda a ganhar tempo, mas a revisão é sempre de quem assina o trabalho.',
     N'{"alternativas":[{"id":"a","texto":"Conferir o resumo com o documento original antes de enviar","correta":true,"motivo":""},{"id":"b","texto":"Enviar direto, a IA já resumiu","correta":false,"motivo":"Quem envia é responsável pelo conteúdo. A IA pode ter deixado algo importante de fora."},{"id":"c","texto":"Pedir outro resumo para a IA e enviar o segundo","correta":false,"motivo":"Pedir de novo não garante que o resumo esteja certo. A conferência precisa ser sua."}]}'),
    (5, N'VERDADEIRO_OU_FALSO', N'Verdadeiro ou falso?', N'DIFICIL',
     N'Dados de saúde nunca vão para ferramentas não aprovadas, mesmo sem o nome do paciente.',
     N'{"afirmacao":"Dá para colar a foto de um exame numa IA pública se o nome do paciente estiver cortado.","verdadeira":false,"motivoDoErro":"Exames têm outros dados que identificam a pessoa, como data e número de atendimento. E são dados sensíveis."}')
) AS d (ordem, tipo, enunciado, dificuldade, explicacao, conteudo_json) ON 1 = 1
WHERE l.titulo = N'IA no trabalho';
