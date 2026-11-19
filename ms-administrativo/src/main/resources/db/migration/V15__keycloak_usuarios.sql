-- Autenticação e RBAC passam para o Keycloak (realm "clinical"): senha, grupos e permissões ficam lá.
-- Aqui fica só o vínculo da pessoa (médico/funcionário) com o usuário do Keycloak.
-- Usuários já existentes ficam sem keycloak_id: precisam ser recriados no Keycloak (a senha não migra,
-- o hash BCrypt não é importável sem extensão) e vinculados de novo pelo cadastro (PUT com senha).

DROP TABLE usuario_permissoes;
DROP TABLE usuario_grupos;
DROP TABLE grupo_permissoes;
DROP TABLE grupos;

ALTER TABLE usuarios DROP COLUMN senha;
ALTER TABLE usuarios ADD COLUMN keycloak_id VARCHAR(36);
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_keycloak_id UNIQUE (keycloak_id);

-- O administrador inicial agora vem do realm do Keycloak, sem pessoa vinculada: o registro local sai
DELETE FROM usuarios
WHERE NOT EXISTS (SELECT 1 FROM funcionarios f WHERE f.usuario_id = usuarios.id)
  AND NOT EXISTS (SELECT 1 FROM medicos m WHERE m.usuario_id = usuarios.id);
