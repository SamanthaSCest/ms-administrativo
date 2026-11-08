package br.edu.imepac.clinicalapi.agendamento.repositories;

import br.edu.imepac.clinicalapi.agendamento.entities.Consulta;
import br.edu.imepac.clinicalapi.agendamento.entities.StatusConsulta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Repository de Consulta. O método {@code buscar} usa JPQL com filtros opcionais:
 * quando o parâmetro é null a condição "(:x is null or ...)" é ignorada.
 */
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    @Query("""
            select c from Consulta c
            where (:medicoId is null or c.medicoId = :medicoId)
              and (:pacienteId is null or c.pacienteId = :pacienteId)
              and (:status is null or c.status = :status)
            """)
    Page<Consulta> buscar(@Param("medicoId") Long medicoId,
                          @Param("pacienteId") Long pacienteId,
                          @Param("status") StatusConsulta status,
                          Pageable pageable);

    boolean existsByMedicoIdAndDataAgendamentoAndStatusIn(Long medicoId, LocalDateTime dataAgendamento,
                                                          Collection<StatusConsulta> status);

    boolean existsByMedicoIdAndDataAgendamentoAndStatusInAndIdNot(Long medicoId, LocalDateTime dataAgendamento,
                                                                  Collection<StatusConsulta> status, Long id);
}
