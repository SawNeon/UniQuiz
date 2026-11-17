UPDATE colaborador
SET pin_hash = CHAR(123) + 'bcrypt' + CHAR(125) + '$2a$10$RpupASC9T1U3KkVeG43haesUCHukp5QrHs5vVh1S2cKyyPesTk/by',
    tentativas_falhas_pin = 0,
    pin_bloqueado_ate = NULL
WHERE pin_hash IS NULL;
