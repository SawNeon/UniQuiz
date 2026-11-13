ALTER TABLE administrador ADD
    nome NVARCHAR(150) NULL,
    perfil NVARCHAR(20) NOT NULL CONSTRAINT df_administrador_perfil DEFAULT 'ADMINISTRADOR',
    precisa_trocar_senha BIT NOT NULL CONSTRAINT df_administrador_troca_de_senha DEFAULT 0,
    senha_alterada_em DATETIME2 NULL,
    criado_em DATETIME2 NULL,
    ultimo_acesso_em DATETIME2 NULL;

GO

UPDATE administrador
SET nome = usuario, criado_em = SYSUTCDATETIME(), senha_alterada_em = SYSUTCDATETIME()
WHERE nome IS NULL;

GO

ALTER TABLE administrador ALTER COLUMN nome NVARCHAR(150) NOT NULL;

ALTER TABLE administrador ALTER COLUMN criado_em DATETIME2 NOT NULL;

ALTER TABLE administrador ADD CONSTRAINT ck_administrador_perfil
    CHECK (perfil IN ('ADMINISTRADOR', 'GESTOR', 'AUDITOR'));

CREATE TABLE administrador_setor (
    administrador_id BIGINT NOT NULL CONSTRAINT fk_administrador_setor_administrador REFERENCES administrador (id),
    setor_id BIGINT NOT NULL CONSTRAINT fk_administrador_setor_setor REFERENCES setor (id),
    CONSTRAINT pk_administrador_setor PRIMARY KEY (administrador_id, setor_id)
);
