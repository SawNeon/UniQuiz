CREATE TABLE registro_de_auditoria (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_registro_de_auditoria PRIMARY KEY,
    momento DATETIME2 NOT NULL,
    ator_tipo NVARCHAR(20) NOT NULL CONSTRAINT ck_auditoria_ator_tipo
        CHECK (ator_tipo IN ('ADMINISTRADOR', 'COLABORADOR', 'ANONIMO')),
    ator_id BIGINT NULL,
    ator_descricao NVARCHAR(200) NULL,
    acao NVARCHAR(60) NOT NULL,
    alvo NVARCHAR(300) NULL,
    resultado NVARCHAR(10) NOT NULL CONSTRAINT ck_auditoria_resultado CHECK (resultado IN ('SUCESSO', 'FALHA')),
    detalhe NVARCHAR(500) NULL,
    ip NVARCHAR(45) NULL
);

GO

CREATE INDEX ix_auditoria_momento ON registro_de_auditoria (momento);

CREATE INDEX ix_auditoria_ator ON registro_de_auditoria (ator_tipo, ator_id, momento);

GO

CREATE TRIGGER tg_auditoria_somente_inclusao ON registro_de_auditoria
INSTEAD OF UPDATE, DELETE
AS
THROW 51000, N'O registro de auditoria aceita apenas inclusões.', 1;
