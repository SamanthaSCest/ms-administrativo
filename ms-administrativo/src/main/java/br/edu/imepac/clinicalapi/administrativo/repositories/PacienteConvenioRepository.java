package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.PacienteConvenio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Vínculos paciente–convênio. */
public interface PacienteConvenioRepository extends JpaRepository<PacienteConvenio, Long> {

    /** Ativos primeiro, depois do mais antigo para o mais novo. */
    List<PacienteConvenio> findByPacienteIdOrderByAtivoDescIdAsc(Long pacienteId);

    Optional<PacienteConvenio> findByIdAndPacienteId(Long id, Long pacienteId);

    /** RN05: já existe vínculo ativo do paciente com o convênio? */
    boolean existsByPacienteIdAndConvenioIdAndAtivoTrue(Long pacienteId, Long convenioId);

    boolean existsByPacienteIdAndConvenioIdAndAtivoTrueAndIdNot(Long pacienteId, Long convenioId, Long id);

    List<PacienteConvenio> findByPacienteIdAndAtivoTrueOrderByIdAsc(Long pacienteId);
}
