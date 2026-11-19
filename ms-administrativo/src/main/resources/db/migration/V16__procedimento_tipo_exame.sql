-- Exames passam a ser cadastrados junto aos procedimentos (mesmos campos: TUSS, especialidade, duração, valor base,
-- insumos e custos dos médicos), diferenciados pelo tipo. Os já cadastrados são procedimentos.
ALTER TABLE procedimentos ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'PROCEDIMENTO';

CREATE INDEX ix_procedimentos_tipo ON procedimentos (tipo);

-- A sala volta a poder ser do tipo EXAME (TipoSala). A coluna salas.tipo é texto, então só o enum muda.
