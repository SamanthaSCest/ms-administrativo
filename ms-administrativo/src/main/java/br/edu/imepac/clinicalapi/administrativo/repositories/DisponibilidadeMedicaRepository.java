package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.DisponibilidadeMedica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadeMedicaRepository extends JpaRepository<DisponibilidadeMedica, Long> {

    Page<DisponibilidadeMedica> findByMedicoId(Long medicoId, Pageable pageable);

    List<DisponibilidadeMedica> findAllByMedicoId(Long medicoId);

    List<DisponibilidadeMedica> findAllBySalaId(Long salaId);

    boolean existsBySalaId(Long salaId);
}
