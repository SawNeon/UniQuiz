ALTER TABLE sessao ADD
    ultimo_uso_em DATETIME2 NULL,
    inatividade_maxima_minutos INT NULL,
    ip NVARCHAR(45) NULL,
    dispositivo NVARCHAR(120) NULL;
GO

UPDATE sessao SET ultimo_uso_em = criada_em;
UPDATE sessao SET inatividade_maxima_minutos = 30 WHERE administrador_id IS NOT NULL;
GO

ALTER TABLE sessao ALTER COLUMN ultimo_uso_em DATETIME2 NOT NULL;
CREATE INDEX ix_sessao_administrador ON sessao (administrador_id);
CREATE INDEX ix_sessao_colaborador ON sessao (colaborador_id);
