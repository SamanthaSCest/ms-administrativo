package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Sala;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoSala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    boolean existsByNumeroIgnoreCase(String numero);

    boolean existsByNumeroIgnoreCaseAndIdNot(String numero, Long id);

    /** Integração: salas ativas (opcionalmente de um tipo). */
    List<Sala> findByAtivoTrueOrderByNumeroAsc();

    List<Sala> findByTipoAndAtivoTrueOrderByNumeroAsc(TipoSala tipo);
}
