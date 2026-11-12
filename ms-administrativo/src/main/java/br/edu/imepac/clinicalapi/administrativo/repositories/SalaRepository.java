package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    boolean existsByIdentificacaoIgnoreCase(String identificacao);

    boolean existsByIdentificacaoIgnoreCaseAndIdNot(String identificacao, Long id);
}
