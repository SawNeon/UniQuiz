ALTER TABLE licao ADD
    qtd_facil INT NOT NULL CONSTRAINT df_licao_qtd_facil DEFAULT 0,
    qtd_medio INT NOT NULL CONSTRAINT df_licao_qtd_medio DEFAULT 0,
    qtd_dificil INT NOT NULL CONSTRAINT df_licao_qtd_dificil DEFAULT 0;

GO

ALTER TABLE licao ADD CONSTRAINT ck_licao_quantidades CHECK (
    qtd_facil BETWEEN 0 AND 30 AND qtd_medio BETWEEN 0 AND 30 AND qtd_dificil BETWEEN 0 AND 30
);

GO

UPDATE licao SET
    qtd_facil = (SELECT COUNT(*) FROM desafio d
                 WHERE d.licao_id = licao.id AND d.status = 'PUBLICADO' AND d.dificuldade = 'FACIL'),
    qtd_medio = (SELECT COUNT(*) FROM desafio d
                 WHERE d.licao_id = licao.id AND d.status = 'PUBLICADO' AND d.dificuldade = 'MEDIO'),
    qtd_dificil = (SELECT COUNT(*) FROM desafio d
                   WHERE d.licao_id = licao.id AND d.status = 'PUBLICADO' AND d.dificuldade = 'DIFICIL');

GO

CREATE TABLE desafio_da_tentativa (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_desafio_da_tentativa PRIMARY KEY,
    tentativa_id BIGINT NOT NULL CONSTRAINT fk_desafio_da_tentativa_tentativa REFERENCES tentativa (id),
    desafio_id BIGINT NOT NULL CONSTRAINT fk_desafio_da_tentativa_desafio REFERENCES desafio (id),
    posicao INT NOT NULL,
    CONSTRAINT uq_desafio_da_tentativa UNIQUE (tentativa_id, desafio_id)
);
