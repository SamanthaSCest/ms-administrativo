package br.edu.imepac.clinicalapi.administrativo.dto.integracao;

import br.edu.imepac.clinicalapi.administrativo.entities.UF;

import java.util.List;
import java.util.UUID;

/**
 * Resumo do médico (integração e eventos). Sem CPF. Compatível com o MedicoResumo(id, nome, crm)
 * que o ms-agendamento já lê: os campos a mais são ignorados por ele.
 */
public record MedicoResumo(Long id, UUID uuid, String nome, String crm, UF ufCrm,
                           List<EspecialidadeResumo> especialidades, boolean ativo) {
}
