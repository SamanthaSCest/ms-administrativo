package br.edu.imepac.clinicalapi.administrativo.config;

import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração única do ModelMapper, compartilhada pelos mappers via injeção de dependência.
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // STRICT: só mapeia propriedades com nomes idênticos (evita ambiguidades como nome vs especialidade.nome)
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        // Usuário (também dentro de médico/funcionário): grupos e permissões efetivas são montados à mão
        modelMapper.createTypeMap(Usuario.class, UsuarioResponse.class)
                .setConverter(ctx -> ctx.getSource() == null ? null : UsuarioResponse.de(ctx.getSource()));
        return modelMapper;
    }
}
