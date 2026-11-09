package br.edu.imepac.clinicalapi.agendamento.mapper;

import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaRequest;
import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaResponse;
import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaUpdateRequest;
import br.edu.imepac.clinicalapi.agendamento.entities.Consulta;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Mapper de Consulta (DTO <-> entidade) com ModelMapper.
 */
@Component
public class ConsultaMapper {

    private final ModelMapper modelMapper;

    public ConsultaMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Consulta toEntity(ConsultaRequest request) {
        return modelMapper.map(request, Consulta.class);
    }

    /** Copia só os campos editáveis; médico, paciente e status não mudam por aqui. */
    public void updateEntity(ConsultaUpdateRequest request, Consulta entity) {
        modelMapper.map(request, entity);
    }

    public ConsultaResponse toResponse(Consulta entity) {
        return modelMapper.map(entity, ConsultaResponse.class);
    }
}
