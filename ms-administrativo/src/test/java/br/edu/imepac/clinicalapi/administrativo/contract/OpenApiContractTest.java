package br.edu.imepac.clinicalapi.administrativo.contract;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Garante que o contrato OpenAPI é um YAML válido (evita quebrar o Swagger UI com erro de sintaxe). */
class OpenApiContractTest {

    @Test
    void contratoDeveSerYamlValidoComPathsEComponents() throws Exception {
        try (InputStream in = new ClassPathResource("static/contract/openapi-administrativo.yaml").getInputStream()) {
            Map<String, Object> spec = new Yaml().load(in);

            assertThat(spec).containsKeys("openapi", "info", "paths", "components");
        }
    }
}
