package br.edu.imepac.clinicalapi.administrativo.mapper;

import br.edu.imepac.clinicalapi.administrativo.dto.EspecialidadeRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.EspecialidadeResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Converte DTO <-> entidade com ModelMapper, para o service não ficar cheio de getters/setters.
 * Nunca exponha a entidade JPA diretamente na API: use sempre DTOs.
 */
@Component
public class EspecialidadeMapper {

    private final ModelMapper modelMapper;

    public EspecialidadeMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Especialidade toEntity(EspecialidadeRequest request) {
        return modelMapper.map(request, Especialidade.class);
    }

    public void updateEntity(EspecialidadeRequest request, Especialidade entity) {
        modelMapper.map(request, entity);
    }

    public EspecialidadeResponse toResponse(Especialidade entity) {
        return modelMapper.map(entity, EspecialidadeResponse.class);
    }
}
