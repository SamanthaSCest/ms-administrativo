package br.edu.imepac.clinicalapi.administrativo.mapper;

import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Mapper de Funcionário (DTO <-> entidade). senha e perfil do request são tratados pelo UsuarioService.
 */
@Component
public class FuncionarioMapper {

    private final ModelMapper modelMapper;

    public FuncionarioMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Funcionario toEntity(FuncionarioRequest request) {
        return modelMapper.map(request, Funcionario.class);
    }

    public void updateEntity(FuncionarioRequest request, Funcionario entity) {
        modelMapper.map(request, entity);
    }

    public FuncionarioResponse toResponse(Funcionario entity) {
        return modelMapper.map(entity, FuncionarioResponse.class);
    }
}
