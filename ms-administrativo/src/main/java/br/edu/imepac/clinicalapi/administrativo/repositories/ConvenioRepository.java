package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Convenio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConvenioRepository extends JpaRepository<Convenio, Long> {

    boolean existsByRegistroAns(String registroAns);

    boolean existsByRegistroAnsAndIdNot(String registroAns, Long id);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    /** Listagem paginada por situação. */
    Page<Convenio> findByAtivo(boolean ativo, Pageable pageable);

    /** Integração: convênios ativos. */
    List<Convenio> findByAtivoTrueOrderByNomeAsc();
}
