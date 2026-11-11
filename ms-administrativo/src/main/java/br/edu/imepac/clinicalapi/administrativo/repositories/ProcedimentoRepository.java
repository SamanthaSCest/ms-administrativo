package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {

    boolean existsByCodigoTussIgnoreCase(String codigoTuss);

    boolean existsByCodigoTussIgnoreCaseAndIdNot(String codigoTuss, Long id);

    /** Algum procedimento usa o insumo? (navega por insumos.insumo) */
    boolean existsByInsumosInsumoId(Long insumoId);

    boolean existsByEspecialidadeId(Long especialidadeId);
}
