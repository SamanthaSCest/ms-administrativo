package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import br.edu.imepac.clinicalapi.administrativo.entities.UF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repository de Médico (Spring Data JPA gera a implementação em tempo de execução).
 */
public interface MedicoRepository extends JpaRepository<Medico, Long>, JpaSpecificationExecutor<Medico> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByCrmAndUfCrm(String crm, UF ufCrm);

    boolean existsByCrmAndUfCrmAndIdNot(String crm, UF ufCrm, Long id);

    /** Algum médico atende a especialidade? (navega pela coleção especialidades) */
    boolean existsByEspecialidadesId(Long especialidadeId);

    /** RN11: quantos médicos ativos ainda atendem a especialidade. */
    long countByEspecialidadesIdAndAtivoTrue(Long especialidadeId);

    /** Integração: médicos ativos que atendem a especialidade. */
    List<Medico> findByEspecialidadesIdAndAtivoTrueOrderByNomeAsc(Long especialidadeId);
}
