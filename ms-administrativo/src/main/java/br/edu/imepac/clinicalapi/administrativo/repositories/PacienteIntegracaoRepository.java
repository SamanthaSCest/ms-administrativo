package br.edu.imepac.clinicalapi.administrativo.repositories;

import br.edu.imepac.clinicalapi.administrativo.dto.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.SituacaoResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PacienteIntegracaoRepository {

    private final JdbcTemplate jdbcTemplate;

    public PacienteIntegracaoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PacienteResumo> buscarResumo(UUID id) {
        return jdbcTemplate.query(
                """
                        SELECT id, nome, cpf, data_nascimento, sexo, ativo
                        FROM paciente
                        WHERE id = ?
                        """,
                (rs, rowNum) -> mapearResumo(rs),
                id
        ).stream().findFirst();
    }

    public Optional<PacienteContato> buscarContato(UUID id) {
        return jdbcTemplate.query(
                """
                        SELECT nome, email, telefone_celular
                        FROM paciente
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new PacienteContato(
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("telefone_celular")
                ),
                id
        ).stream().findFirst();
    }

    public SituacaoResponse buscarSituacao(UUID id) {
        return jdbcTemplate.query(
                "SELECT ativo FROM paciente WHERE id = ?",
                (rs, rowNum) -> new SituacaoResponse(true, rs.getBoolean("ativo")),
                id
        ).stream().findFirst().orElseGet(() -> new SituacaoResponse(false, false));
    }

    private PacienteResumo mapearResumo(ResultSet rs) throws SQLException {
        return new PacienteResumo(
                rs.getObject("id", UUID.class),
                rs.getString("nome"),
                mascararCpf(rs.getString("cpf")),
                rs.getObject("data_nascimento", LocalDate.class),
                rs.getString("sexo"),
                rs.getBoolean("ativo")
        );
    }

    private String mascararCpf(String cpf) {
        String digitos = cpf.trim();
        return "***.%s.%s-**".formatted(digitos.substring(3, 6), digitos.substring(6, 9));
    }
}
