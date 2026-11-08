package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso ao banco via Spring Data JPA. Os métodos existsBy... viram SQL automaticamente
 * pelo nome (derived queries), sem precisar escrever a consulta.
 */
public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
