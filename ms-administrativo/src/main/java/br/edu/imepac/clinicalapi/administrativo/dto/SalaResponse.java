package br.edu.imepac.clinicalapi.administrativo.dto;

import br.edu.imepac.clinicalapi.administrativo.entities.TipoSala;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SalaResponse {

    private Long id;
    private String identificacao;
    private TipoSala tipo;
    private String andar;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
