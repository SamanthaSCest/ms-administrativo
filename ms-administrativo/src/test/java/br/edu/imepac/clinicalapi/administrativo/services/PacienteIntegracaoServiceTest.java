package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteIntegracaoServiceTest {

    @Mock
    private PacienteRepository repository;

    private PacienteIntegracaoService service;

    @BeforeEach
    void setUp() {
        service = new PacienteIntegracaoService(repository);
    }

    private Paciente paciente() {
        Paciente p = new Paciente();
        p.setId(7L);
        p.setNome("Maria Aparecida Souza");
        p.setCpf("12345678909");
        p.setEmail("maria@email.com");
        p.setTelefone("34999998888");
        p.setDataNascimento(LocalDate.of(1988, 4, 12));
        return p;
    }

    @Test
    void buscarResumo_deveMascararCpf() {
        when(repository.findById(7L)).thenReturn(Optional.of(paciente()));

        PacienteResumo resumo = service.buscarResumo(7L);

        assertThat(resumo.id()).isEqualTo(7L);
        assertThat(resumo.nome()).isEqualTo("Maria Aparecida Souza");
        assertThat(resumo.cpfMascarado()).isEqualTo("***.456.789-**");
        assertThat(resumo.dataNascimento()).isEqualTo(LocalDate.of(1988, 4, 12));
    }

    @Test
    void buscarContato_deveDevolverNomeEmailTelefone() {
        when(repository.findById(7L)).thenReturn(Optional.of(paciente()));

        PacienteContato contato = service.buscarContato(7L);

        assertThat(contato.nome()).isEqualTo("Maria Aparecida Souza");
        assertThat(contato.email()).isEqualTo("maria@email.com");
        assertThat(contato.telefone()).isEqualTo("34999998888");
    }

    @Test
    void buscarResumo_inexistente_deveLancarNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarResumo(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buscarSituacao_existente() {
        when(repository.existsById(7L)).thenReturn(true);

        assertThat(service.buscarSituacao(7L)).isEqualTo(new SituacaoResponse(true, true));
    }

    @Test
    void buscarSituacao_inexistente_naoLancaErro() {
        when(repository.existsById(1L)).thenReturn(false);

        assertThat(service.buscarSituacao(1L)).isEqualTo(new SituacaoResponse(false, false));
    }
}
