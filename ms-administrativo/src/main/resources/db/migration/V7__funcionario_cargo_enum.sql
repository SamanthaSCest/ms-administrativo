-- Cargo do funcionário passa a ser o enum Cargo (RECEPCIONISTA, ESTOQUISTA, RESPONSAVEL_FINANCEIRO, ADMINISTRADOR).
-- Converte os textos livres já gravados: admins viram ADMINISTRADOR; valores fora do enum viram RECEPCIONISTA.
UPDATE funcionarios SET cargo = 'ADMINISTRADOR'
WHERE usuario_id IN (SELECT id FROM usuarios WHERE perfil = 'ADMIN');

UPDATE funcionarios SET cargo = 'RECEPCIONISTA'
WHERE cargo NOT IN ('RECEPCIONISTA', 'ESTOQUISTA', 'RESPONSAVEL_FINANCEIRO', 'ADMINISTRADOR');
