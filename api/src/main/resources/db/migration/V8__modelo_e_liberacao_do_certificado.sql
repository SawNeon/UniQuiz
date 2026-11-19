ALTER TABLE modulo ADD certificado_liberado BIT NOT NULL
    CONSTRAINT df_modulo_certificado_liberado DEFAULT 0;
GO

UPDATE modulo SET certificado_liberado = 1;
GO

CREATE TABLE modelo_de_certificado (
    id INT NOT NULL CONSTRAINT pk_modelo_de_certificado PRIMARY KEY
        CONSTRAINT ck_modelo_de_certificado_unico CHECK (id = 1),
    layout NVARCHAR(MAX) NOT NULL,
    fundo_pdf VARBINARY(MAX) NULL,
    fundo_previa VARBINARY(MAX) NULL,
    nome_da_arte NVARCHAR(200) NULL,
    atualizado_em DATETIME2 NOT NULL,
    atualizado_por BIGINT NULL CONSTRAINT fk_modelo_de_certificado_administrador REFERENCES administrador (id)
);

CREATE INDEX ix_certificado_modulo ON certificado (modulo_id, nome_colaborador);
