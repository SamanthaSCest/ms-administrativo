package br.edu.imepac.clinicalapi.administrativo.mapper;

import br.edu.imepac.clinicalapi.administrativo.dto.EnderecoDto;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Endereco;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Mapper de Paciente (DTO <-> entidade) com ModelMapper.
 * Endereço e responsável são tratados à mão: o responsável vem por id (resolvido no service)
 * e o Endereco é um objeto embutido com construtor próprio.
 */
@Component
public class PacienteMapper {

    private final ModelMapper modelMapper;

    public PacienteMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        // emptyTypeMap + skips + implicitMappings: os skips precisam vir antes do mapeamento automático,
        // senão o ModelMapper já teria mapeado endereco.* campo a campo
        if (this.modelMapper.getTypeMap(PacienteRequest.class, Paciente.class) == null) {
            this.modelMapper.emptyTypeMap(PacienteRequest.class, Paciente.class)
                    .addMappings(m -> {
                        m.skip(Paciente::setEndereco);
                        m.skip(Paciente::setResponsavel);
                        // Consentimento LGPD: tratado em aplicarConsentimento (data registrada pelo servidor)
                        m.skip(Paciente::setConsentimentoLgpd);
                        m.skip(Paciente::setVersaoTermoLgpd);
                        m.skip(Paciente::setDataConsentimentoLgpd);
                    })
                    .implicitMappings();
        }
    }

    public Paciente toEntity(PacienteRequest request) {
        Paciente paciente = modelMapper.map(request, Paciente.class);
        paciente.setEndereco(toEndereco(request.getEndereco()));
        aplicarConsentimento(request, paciente);
        return paciente;
    }

    public void updateEntity(PacienteRequest request, Paciente entity) {
        modelMapper.map(request, entity);
        entity.setEndereco(toEndereco(request.getEndereco()));
        aplicarConsentimento(request, entity);
    }

    private static void aplicarConsentimento(PacienteRequest request, Paciente paciente) {
        paciente.definirConsentimentoLgpd(Boolean.TRUE.equals(request.getConsentimentoLgpd()),
                // Em segundos: a resposta do POST e a lida do banco mostram a mesma data
                request.getVersaoTermoLgpd(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    public PacienteResponse toResponse(Paciente entity) {
        PacienteResponse response = modelMapper.map(entity, PacienteResponse.class);
        if (entity.getEndereco() != null) {
            response.getEndereco().setFormatado(entity.getEndereco().formatado());
        }
        if (entity.getDataNascimento() != null) {
            response.setIdade(entity.calcularIdade());
            response.setMenorDeIdade(entity.ehMenorDeIdade());
        }
        return response;
    }

    private static Endereco toEndereco(EnderecoDto dto) {
        if (dto == null) {
            return null;
        }
        return new Endereco(dto.getCep(), dto.getLogradouro(), dto.getNumero(), dto.getComplemento(),
                dto.getBairro(), dto.getCidade(), dto.getUf());
    }
}
