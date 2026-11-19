package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Procedimento;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoProcedimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, Long> {

    /** Listagem filtrada: só procedimentos ou só exames. */
    Page<Procedimento> findByTipo(TipoProcedimento tipo, Pageable pageable);

    boolean existsByCodigoTussIgnoreCase(String codigoTuss);

    boolean existsByCodigoTussIgnoreCaseAndIdNot(String codigoTuss, Long id);

    /** Algum procedimento usa o insumo? (navega por insumos.insumo) */
    boolean existsByInsumosInsumoId(Long insumoId);

    boolean existsByEspecialidadeId(Long especialidadeId);
}
