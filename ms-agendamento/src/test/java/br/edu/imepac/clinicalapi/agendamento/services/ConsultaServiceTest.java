package br.edu.imepac.clinicalapi.agendamento.services;

import br.edu.imepac.clinicalapi.agendamento.client.AdministrativoClient;
import br.edu.imepac.clinicalapi.agendamento.client.dto.MedicoResumo;
import br.edu.imepac.clinicalapi.agendamento.client.dto.PacienteResumo;
import br.edu.imepac.clinicalapi.agendamento.config.ModelMapperConfig;
import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaRequest;
import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaResponse;
import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaUpdateRequest;
import br.edu.imepac.clinicalapi.agendamento.entities.Consulta;
import br.edu.imepac.clinicalapi.agendamento.entities.StatusConsulta;
import br.edu.imepac.clinicalapi.agendamento.mapper.ConsultaMapper;
import br.edu.imepac.clinicalapi.agendamento.repositories.ConsultaRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ExternalServiceException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    private static final LocalDateTime DATA = LocalDateTime.now().plusDays(3).withNano(0);

    @Mock
    private ConsultaRepository repository;
    @Mock
    private AdministrativoClient client;

    private ConsultaService service;

    @BeforeEach
    void setUp() {
        service = new ConsultaService(repository, client, new ConsultaMapper(new ModelMapperConfig().modelMapper()));
    }

    private ConsultaRequest request() {
        ConsultaRequest r = new ConsultaRequest();
        r.setMedicoId(1L);
        r.setPacienteId(2L);
        r.setDataAgendamento(DATA);
        r.setSintomas("dor de cabeça");
        return r;
    }

    private Consulta consulta(StatusConsulta status) {
        Consulta c = new Consulta();
        c.setId(7L);
        c.setMedicoId(1L);
        c.setPacienteId(2L);
        c.setDataAgendamento(DATA);
        c.setStatus(status);
        return c;
    }

    @Test
    void agendar_deveValidarNoAdministrativoESalvarComoAgendado() {
        when(client.buscarMedico(1L)).thenReturn(Optional.of(new MedicoResumo(1L, "Maria", "CRM-1")));
        when(client.buscarPaciente(2L)).thenReturn(Optional.of(new PacienteResumo(2L, "João")));
        when(repository.existsByMedicoIdAndDataAgendamentoAndStatusIn(1L, DATA, StatusConsulta.ATIVOS)).thenReturn(false);
        when(repository.save(any(Consulta.class))).thenAnswer(inv -> {
            Consulta c = inv.getArgument(0);
            c.setId(7L);
            return c;
        });

        ConsultaResponse response = service.agendar(request());

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(StatusConsulta.AGENDADO);
        assertThat(response.getMedicoId()).isEqualTo(1L);
        assertThat(response.getPacienteId()).isEqualTo(2L);
        assertThat(response.getSintomas()).isEqualTo("dor de cabeça");
    }

    @Test
    void agendar_medicoInexistente_deveLancarBusinessRule() {
        when(client.buscarMedico(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agendar(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Médico");
        verify(repository, never()).save(any());
    }

    @Test
    void agendar_pacienteInexistente_deveLancarBusinessRule() {
        when(client.buscarMedico(1L)).thenReturn(Optional.of(new MedicoResumo(1L, "Maria", "CRM-1")));
        when(client.buscarPaciente(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agendar(request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Paciente");
    }

    @Test
    void agendar_administrativoIndisponivel_propagaExternalService() {
        when(client.buscarMedico(1L)).thenThrow(new ExternalServiceException("fora do ar", new RuntimeException()));

        assertThatThrownBy(() -> service.agendar(request())).isInstanceOf(ExternalServiceException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void agendar_horarioOcupado_deveLancarConflict() {
        when(client.buscarMedico(1L)).thenReturn(Optional.of(new MedicoResumo(1L, "Maria", "CRM-1")));
        when(client.buscarPaciente(2L)).thenReturn(Optional.of(new PacienteResumo(2L, "João")));
        when(repository.existsByMedicoIdAndDataAgendamentoAndStatusIn(1L, DATA, StatusConsulta.ATIVOS)).thenReturn(true);

        assertThatThrownBy(() -> service.agendar(request())).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void buscar_inexistente_deveLancarNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void remarcar_comNovaData_deveMudarParaRemarcado() {
        Consulta existente = consulta(StatusConsulta.AGENDADO);
        LocalDateTime novaData = DATA.plusDays(1);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.existsByMedicoIdAndDataAgendamentoAndStatusInAndIdNot(1L, novaData, StatusConsulta.ATIVOS, 7L))
                .thenReturn(false);
        when(repository.save(existente)).thenReturn(existente);
        ConsultaUpdateRequest update = new ConsultaUpdateRequest();
        update.setDataAgendamento(novaData);
        update.setObservacoes("trazer exames");

        ConsultaResponse response = service.remarcar(7L, update);

        assertThat(response.getStatus()).isEqualTo(StatusConsulta.REMARCADO);
        assertThat(response.getDataAgendamento()).isEqualTo(novaData);
        assertThat(response.getObservacoes()).isEqualTo("trazer exames");
        assertThat(response.getMedicoId()).isEqualTo(1L);
    }

    @Test
    void remarcar_mesmaData_naoMudaStatus() {
        Consulta existente = consulta(StatusConsulta.AGENDADO);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        ConsultaUpdateRequest update = new ConsultaUpdateRequest();
        update.setDataAgendamento(DATA);

        ConsultaResponse response = service.remarcar(7L, update);

        assertThat(response.getStatus()).isEqualTo(StatusConsulta.AGENDADO);
        verify(repository, never()).existsByMedicoIdAndDataAgendamentoAndStatusInAndIdNot(
                anyLong(), any(), any(), anyLong());
    }

    @Test
    void remarcar_consultaCancelada_deveLancarBusinessRule() {
        when(repository.findById(7L)).thenReturn(Optional.of(consulta(StatusConsulta.CANCELADO)));
        ConsultaUpdateRequest update = new ConsultaUpdateRequest();
        update.setDataAgendamento(DATA.plusDays(1));

        assertThatThrownBy(() -> service.remarcar(7L, update)).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void cancelar_deveMudarStatus() {
        Consulta existente = consulta(StatusConsulta.REMARCADO);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        assertThat(service.cancelar(7L).getStatus()).isEqualTo(StatusConsulta.CANCELADO);
    }

    @Test
    void realizar_consultaJaRealizada_deveLancarBusinessRule() {
        when(repository.findById(7L)).thenReturn(Optional.of(consulta(StatusConsulta.REALIZADO)));

        assertThatThrownBy(() -> service.realizar(7L)).isInstanceOf(BusinessRuleException.class);
    }
}
