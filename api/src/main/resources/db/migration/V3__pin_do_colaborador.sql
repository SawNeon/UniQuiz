ALTER TABLE colaborador ADD
    pin_hash NVARCHAR(100) NULL,
    tentativas_falhas_pin INT NOT NULL CONSTRAINT df_colaborador_tentativas_pin DEFAULT 0,
    pin_bloqueado_ate DATETIME2 NULL;
