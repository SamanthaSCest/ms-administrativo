package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.UF;
import jakarta.validation.constraints.Size;

/**
 * Filtros da busca de médicos (POST /medicos/busca). Vão no corpo, e não na URL, porque o nome é
 * dado pessoal (RNF08). Todos os campos são opcionais e se combinam (E).
 *
 * @param nome            parte do nome, sem diferenciar maiúsculas
 * @param crm             número do CRM completo
 * @param ufCrm           UF do CRM
 * @param especialidadeId médicos que atendem essa especialidade
 * @param ativo           situação; se omitido, traz só os ativos
 */
public record MedicoBuscaRequest(
        @Size(max = 150) String nome,
        @Size(max = 20) String crm,
        UF ufCrm,
        Long especialidadeId,
        Boolean ativo) {
}
