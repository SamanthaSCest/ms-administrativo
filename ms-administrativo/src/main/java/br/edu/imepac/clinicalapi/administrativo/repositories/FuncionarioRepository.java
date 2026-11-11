package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository de Funcionário (Spring Data JPA gera a implementação em tempo de execução).
 */
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByMatricula(String matricula);

    boolean existsByMatriculaAndIdNot(String matricula, Long id);
}
