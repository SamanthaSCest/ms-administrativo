package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository de Paciente (Spring Data JPA gera a implementação em tempo de execução).
 */
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);
}
