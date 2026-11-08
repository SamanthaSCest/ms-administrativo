package br.edu.imepac.clinicalapi.agendamento.config;

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
        // STRICT: só mapeia propriedades com nomes idênticos (evita ambiguidades como ex.: nome vs aninhado.nome)
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }
}
