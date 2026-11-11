package br.edu.imepac.clinicalapi.administrativo.mapper;

import br.edu.imepac.clinicalapi.administrativo.dto.MedicoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Mapper de Médico. Atenção: especialidadeIds (request) vira o conjunto de Especialidade no service,
 * por isso o campo "especialidades" é ignorado aqui (skip).
 */
@Component
public class MedicoMapper {

    private final ModelMapper modelMapper;

    public MedicoMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        if (this.modelMapper.getTypeMap(MedicoRequest.class, Medico.class) == null) {
            this.modelMapper.emptyTypeMap(MedicoRequest.class, Medico.class)
                    .addMappings(m -> m.skip(Medico::setEspecialidades))
                    .implicitMappings();
        }
    }

    public Medico toEntity(MedicoRequest request) {
        return modelMapper.map(request, Medico.class);
    }

    public void updateEntity(MedicoRequest request, Medico entity) {
        modelMapper.map(request, entity);
    }

    public MedicoResponse toResponse(Medico entity) {
        MedicoResponse response = modelMapper.map(entity, MedicoResponse.class);
        // Ordem estável na resposta (o Set da entidade não tem ordem)
        if (response.getEspecialidades() != null) {
            response.getEspecialidades().sort(Comparator.comparing(e -> e.getNome()));
        }
        return response;
    }
}
