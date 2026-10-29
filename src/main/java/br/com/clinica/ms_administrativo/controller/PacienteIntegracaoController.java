package br.com.clinica.ms_administrativo.controller;

import br.com.clinica.ms_administrativo.dto.PacienteResumo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import br.com.clinica.ms_administrativo.dto.SituacaoResponse;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integracao/pacientes")
public class PacienteIntegracaoController {

    private static final UUID ID_EXEMPLO =
            UUID.fromString("8f14e45f-ceea-4a7b-9b1e-0c7d2f1a9e11");

    @GetMapping("/{id}")
    public PacienteResumo buscarPorId(@PathVariable("id") UUID id) {
        if (!ID_EXEMPLO.equals(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Registro não encontrado"
            );
        }

        return new PacienteResumo(
                ID_EXEMPLO,
                "Maria Aparecida Souza",
                "***.456.789-**",
                LocalDate.of(1988, 4, 12),
                "FEMININO",
                true
        );
    }

    @GetMapping("/{id}/situacao")
    public SituacaoResponse consultarSituacao(@PathVariable("id") UUID id) {
        boolean existe = ID_EXEMPLO.equals(id);

        return new SituacaoResponse(existe, existe);
    }
}