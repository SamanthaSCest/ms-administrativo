package br.com.clinica.ms_administrativo.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvento, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_evento
            WHERE publicado_em IS NULL
            ORDER BY criado_em ASC
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvento> buscarPendentesParaPublicacao();
}
