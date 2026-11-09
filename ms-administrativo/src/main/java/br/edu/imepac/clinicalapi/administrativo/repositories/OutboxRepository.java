package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.OutboxEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvento, String> {

    /** Os 100 eventos pendentes mais antigos, na ordem em que aconteceram. */
    List<OutboxEvento> findTop100ByPublicadoEmIsNullOrderByCriadoEmAsc();
}
