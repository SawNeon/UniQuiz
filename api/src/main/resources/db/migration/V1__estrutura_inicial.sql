CREATE TABLE empresa (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_empresa PRIMARY KEY,
    nome NVARCHAR(150) NOT NULL CONSTRAINT uq_empresa_nome UNIQUE
);

CREATE TABLE setor (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_setor PRIMARY KEY,
    empresa_id BIGINT NOT NULL CONSTRAINT fk_setor_empresa REFERENCES empresa (id),
    nome NVARCHAR(150) NOT NULL,
    CONSTRAINT uq_setor_empresa_nome UNIQUE (empresa_id, nome)
);

CREATE TABLE colaborador (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_colaborador PRIMARY KEY,
    empresa_id BIGINT NOT NULL CONSTRAINT fk_colaborador_empresa REFERENCES empresa (id),
    setor_id BIGINT NOT NULL CONSTRAINT fk_colaborador_setor REFERENCES setor (id),
    nome NVARCHAR(200) NOT NULL,
    nome_normalizado NVARCHAR(200) NOT NULL,
    ativo BIT NOT NULL CONSTRAINT df_colaborador_ativo DEFAULT 1,
    criado_em DATETIME2 NOT NULL,
    CONSTRAINT uq_colaborador UNIQUE (empresa_id, setor_id, nome_normalizado)
);

CREATE TABLE administrador (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_administrador PRIMARY KEY,
    usuario NVARCHAR(60) NOT NULL CONSTRAINT uq_administrador_usuario UNIQUE,
    senha_hash NVARCHAR(100) NOT NULL,
    ativo BIT NOT NULL CONSTRAINT df_administrador_ativo DEFAULT 1,
    tentativas_falhas INT NOT NULL CONSTRAINT df_administrador_tentativas DEFAULT 0,
    bloqueado_ate DATETIME2 NULL
);

CREATE TABLE sessao (
    token_hash NVARCHAR(64) NOT NULL CONSTRAINT pk_sessao PRIMARY KEY,
    colaborador_id BIGINT NULL CONSTRAINT fk_sessao_colaborador REFERENCES colaborador (id),
    administrador_id BIGINT NULL CONSTRAINT fk_sessao_administrador REFERENCES administrador (id),
    criada_em DATETIME2 NOT NULL,
    expira_em DATETIME2 NOT NULL,
    CONSTRAINT ck_sessao_dono CHECK (
        (colaborador_id IS NOT NULL AND administrador_id IS NULL)
        OR (colaborador_id IS NULL AND administrador_id IS NOT NULL)
    )
);

CREATE TABLE modulo (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_modulo PRIMARY KEY,
    titulo NVARCHAR(150) NOT NULL,
    descricao NVARCHAR(500) NOT NULL,
    material_estudo NVARCHAR(MAX) NULL,
    ordem INT NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT ck_modulo_status CHECK (status IN ('RASCUNHO', 'PUBLICADO', 'ARQUIVADO'))
);

CREATE TABLE licao (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_licao PRIMARY KEY,
    modulo_id BIGINT NOT NULL CONSTRAINT fk_licao_modulo REFERENCES modulo (id),
    titulo NVARCHAR(150) NOT NULL,
    ordem INT NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT ck_licao_status CHECK (status IN ('RASCUNHO', 'PUBLICADO', 'ARQUIVADO'))
);

CREATE TABLE desafio (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_desafio PRIMARY KEY,
    licao_id BIGINT NOT NULL CONSTRAINT fk_desafio_licao REFERENCES licao (id),
    tipo NVARCHAR(40) NOT NULL,
    enunciado NVARCHAR(1000) NOT NULL,
    dificuldade NVARCHAR(10) NOT NULL CONSTRAINT ck_desafio_dificuldade CHECK (dificuldade IN ('FACIL', 'MEDIO', 'DIFICIL')),
    pontos INT NOT NULL CONSTRAINT ck_desafio_pontos CHECK (pontos BETWEEN 1 AND 100),
    explicacao NVARCHAR(2000) NOT NULL,
    dica NVARCHAR(500) NULL,
    conteudo_json NVARCHAR(MAX) NOT NULL CONSTRAINT ck_desafio_conteudo_json CHECK (ISJSON(conteudo_json) = 1),
    versao INT NOT NULL,
    ordem INT NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT ck_desafio_status CHECK (status IN ('RASCUNHO', 'PUBLICADO', 'ARQUIVADO'))
);

CREATE TABLE temporada (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_temporada PRIMARY KEY,
    nome NVARCHAR(100) NOT NULL,
    inicio DATE NOT NULL,
    fim DATE NOT NULL,
    premio_texto NVARCHAR(300) NULL,
    CONSTRAINT ck_temporada_periodo CHECK (fim >= inicio)
);

CREATE TABLE tentativa (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_tentativa PRIMARY KEY,
    colaborador_id BIGINT NOT NULL CONSTRAINT fk_tentativa_colaborador REFERENCES colaborador (id),
    licao_id BIGINT NOT NULL CONSTRAINT fk_tentativa_licao REFERENCES licao (id),
    temporada_id BIGINT NULL CONSTRAINT fk_tentativa_temporada REFERENCES temporada (id),
    iniciada_em DATETIME2 NOT NULL,
    concluida_em DATETIME2 NULL,
    acertos INT NOT NULL CONSTRAINT df_tentativa_acertos DEFAULT 0,
    total INT NOT NULL,
    perfeita BIT NULL,
    tempo_ms BIGINT NULL,
    percentual_pontos INT NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT ck_tentativa_status CHECK (status IN ('EM_ANDAMENTO', 'CONCLUIDA', 'ABANDONADA'))
);

CREATE TABLE resposta (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_resposta PRIMARY KEY,
    tentativa_id BIGINT NOT NULL CONSTRAINT fk_resposta_tentativa REFERENCES tentativa (id),
    desafio_id BIGINT NOT NULL CONSTRAINT fk_resposta_desafio REFERENCES desafio (id),
    desafio_versao INT NOT NULL,
    resposta_json NVARCHAR(MAX) NOT NULL CONSTRAINT ck_resposta_json CHECK (ISJSON(resposta_json) = 1),
    correta BIT NOT NULL,
    pontos INT NOT NULL,
    segunda_chance BIT NOT NULL,
    respondida_em DATETIME2 NOT NULL
);

CREATE TABLE evento_pontos (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_evento_pontos PRIMARY KEY,
    colaborador_id BIGINT NOT NULL CONSTRAINT fk_evento_pontos_colaborador REFERENCES colaborador (id),
    temporada_id BIGINT NULL CONSTRAINT fk_evento_pontos_temporada REFERENCES temporada (id),
    tentativa_id BIGINT NULL CONSTRAINT fk_evento_pontos_tentativa REFERENCES tentativa (id),
    motivo NVARCHAR(40) NOT NULL,
    pontos INT NOT NULL,
    criado_em DATETIME2 NOT NULL
);

CREATE TABLE trofeu (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_trofeu PRIMARY KEY,
    codigo NVARCHAR(40) NOT NULL CONSTRAINT uq_trofeu_codigo UNIQUE,
    nome NVARCHAR(100) NOT NULL,
    descricao NVARCHAR(300) NOT NULL,
    icone NVARCHAR(40) NOT NULL
);

CREATE TABLE colaborador_trofeu (
    colaborador_id BIGINT NOT NULL CONSTRAINT fk_colaborador_trofeu_colaborador REFERENCES colaborador (id),
    trofeu_id BIGINT NOT NULL CONSTRAINT fk_colaborador_trofeu_trofeu REFERENCES trofeu (id),
    conquistado_em DATETIME2 NOT NULL,
    CONSTRAINT pk_colaborador_trofeu PRIMARY KEY (colaborador_id, trofeu_id)
);

CREATE TABLE sequencia_diaria (
    colaborador_id BIGINT NOT NULL CONSTRAINT pk_sequencia_diaria PRIMARY KEY
        CONSTRAINT fk_sequencia_diaria_colaborador REFERENCES colaborador (id),
    dias_atuais INT NOT NULL,
    maior_sequencia INT NOT NULL,
    ultimo_dia DATE NOT NULL
);

CREATE TABLE certificado (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_certificado PRIMARY KEY,
    codigo NVARCHAR(12) NOT NULL CONSTRAINT uq_certificado_codigo UNIQUE,
    colaborador_id BIGINT NOT NULL CONSTRAINT fk_certificado_colaborador REFERENCES colaborador (id),
    modulo_id BIGINT NOT NULL CONSTRAINT fk_certificado_modulo REFERENCES modulo (id),
    nome_colaborador NVARCHAR(200) NOT NULL,
    titulo_modulo NVARCHAR(150) NOT NULL,
    emitido_em DATETIME2 NOT NULL,
    CONSTRAINT uq_certificado_colaborador_modulo UNIQUE (colaborador_id, modulo_id)
);

CREATE INDEX ix_colaborador_setor ON colaborador (setor_id, ativo) INCLUDE (nome);

CREATE INDEX ix_sessao_expira_em ON sessao (expira_em);

CREATE INDEX ix_licao_modulo ON licao (modulo_id, ordem);

CREATE INDEX ix_desafio_licao ON desafio (licao_id, ordem);

CREATE INDEX ix_tentativa_colaborador ON tentativa (colaborador_id, licao_id, status);

CREATE INDEX ix_tentativa_rapidos ON tentativa (licao_id, perfeita, tempo_ms)
    INCLUDE (colaborador_id, concluida_em, temporada_id);

CREATE UNIQUE INDEX ux_resposta_tentativa_desafio ON resposta (tentativa_id, desafio_id, segunda_chance);

CREATE INDEX ix_evento_pontos_ranking ON evento_pontos (temporada_id, colaborador_id)
    INCLUDE (pontos, criado_em);
