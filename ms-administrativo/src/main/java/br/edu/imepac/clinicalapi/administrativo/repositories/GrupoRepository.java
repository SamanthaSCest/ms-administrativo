package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Repository de Grupo de acesso (RBAC). */
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    Optional<Grupo> findByNome(String nome);

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, Long id);

    /** Quantos usuários estão no grupo (impede remover grupo em uso). */
    @Query("select count(u) from Usuario u join u.grupos g where g.id = :grupoId")
    long contarUsuarios(@Param("grupoId") Long grupoId);
}
