package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.ResponsavelLegal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponsavelLegalRepository extends JpaRepository<ResponsavelLegal, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);
}
