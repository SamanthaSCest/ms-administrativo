package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository de Médico (Spring Data JPA gera a implementação em tempo de execução).
 */
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByCrm(String crm);

    boolean existsByCrmAndIdNot(String crm, Long id);

    boolean existsByEspecialidadeId(Long especialidadeId);
}
