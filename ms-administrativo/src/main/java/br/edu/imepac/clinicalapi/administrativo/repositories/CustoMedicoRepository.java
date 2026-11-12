package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.CustoMedico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustoMedicoRepository extends JpaRepository<CustoMedico, Long> {

    Page<CustoMedico> findByMedicoId(Long medicoId, Pageable pageable);

    List<CustoMedico> findAllByMedicoId(Long medicoId);

    // procedimento só existe na subclasse: consulta JPQL direto nela
    @Query("select count(c) > 0 from CustoMedicoProcedimento c where c.procedimento.id = :procedimentoId")
    boolean existsByProcedimentoId(@Param("procedimentoId") Long procedimentoId);
}
