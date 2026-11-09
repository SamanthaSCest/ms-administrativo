package br.edu.imepac.clinicalapi.agendamento.client;

import br.edu.imepac.clinicalapi.agendamento.client.dto.MedicoResumo;
import br.edu.imepac.clinicalapi.common.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AdministrativoClientTest {

    private static final String BASE = "http://administrativo.test";

    private MockRestServiceServer server;
    private AdministrativoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AdministrativoClient(builder, BASE);
    }

    @Test
    void buscarMedico_200_deveDesserializar() {
        server.expect(requestTo(BASE + "/medicos/1")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":1,"nome":"Maria","crm":"CRM-1","cpf":"123","especialidade":{"id":5,"nome":"Cardio"}}
                        """, MediaType.APPLICATION_JSON));

        Optional<MedicoResumo> medico = client.buscarMedico(1L);

        assertThat(medico).contains(new MedicoResumo(1L, "Maria", "CRM-1"));
        server.verify();
    }

    @Test
    void buscarPaciente_404_deveRetornarVazio() {
        server.expect(requestTo(BASE + "/pacientes/9")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.buscarPaciente(9L)).isEmpty();
    }

    @Test
    void buscarMedico_500_deveLancarExternalService() {
        server.expect(requestTo(BASE + "/medicos/1")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.buscarMedico(1L)).isInstanceOf(ExternalServiceException.class);
    }
}
