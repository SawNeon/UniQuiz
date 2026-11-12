UPDATE licao SET
    qtd_facil = (SELECT COUNT(*) FROM desafio d
                 WHERE d.licao_id = licao.id AND d.status = 'PUBLICADO' AND d.dificuldade = 'FACIL'),
    qtd_medio = (SELECT COUNT(*) FROM desafio d
                 WHERE d.licao_id = licao.id AND d.status = 'PUBLICADO' AND d.dificuldade = 'MEDIO'),
    qtd_dificil = (SELECT COUNT(*) FROM desafio d
                   WHERE d.licao_id = licao.id AND d.status = 'PUBLICADO' AND d.dificuldade = 'DIFICIL')
WHERE qtd_facil + qtd_medio + qtd_dificil = 0;
