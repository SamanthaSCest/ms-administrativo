package br.edu.imepac.clinicalapi.administrativo.mapper;

import br.edu.imepac.clinicalapi.administrativo.dto.PacienteRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Mapper de Paciente (DTO <-> entidade) com ModelMapper.
 */
@Component
public class PacienteMapper {

    private final ModelMapper modelMapper;

    public PacienteMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Paciente toEntity(PacienteRequest request) {
        return modelMapper.map(request, Paciente.class);
    }

    public void updateEntity(PacienteRequest request, Paciente entity) {
        modelMapper.map(request, entity);
    }

    public PacienteResponse toResponse(Paciente entity) {
        return modelMapper.map(entity, PacienteResponse.class);
    }
}
