package br.edu.imepac.clinicalapi.administrativo.mapper;

import br.edu.imepac.clinicalapi.administrativo.dto.MedicoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Mapper de Médico. Atenção: especialidadeId (request) vira o objeto Especialidade no service,
 * por isso o campo "especialidade" é ignorado aqui (skip).
 */
@Component
public class MedicoMapper {

    private final ModelMapper modelMapper;

    public MedicoMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        // especialidade é resolvida pelo service a partir de especialidadeId (não pelo mapper)
        this.modelMapper.typeMap(MedicoRequest.class, Medico.class)
                .addMappings(m -> m.skip(Medico::setEspecialidade));
    }

    public Medico toEntity(MedicoRequest request) {
        return modelMapper.map(request, Medico.class);
    }

    public void updateEntity(MedicoRequest request, Medico entity) {
        modelMapper.map(request, entity);
    }

    public MedicoResponse toResponse(Medico entity) {
        return modelMapper.map(entity, MedicoResponse.class);
    }
}
