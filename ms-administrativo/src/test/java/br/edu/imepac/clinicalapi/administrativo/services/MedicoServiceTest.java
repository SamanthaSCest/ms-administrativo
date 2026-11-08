package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.ModelMapperConfig;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import br.edu.imepac.clinicalapi.administrativo.mapper.MedicoMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicoServiceTest {

    @Mock
    private MedicoRepository repository;
    @Mock
    private EspecialidadeRepository especialidadeRepository;

    private MedicoService service;

    @BeforeEach
    void setUp() {
        MedicoMapper mapper = new MedicoMapper(new ModelMapperConfig().modelMapper());
        service = new MedicoService(repository, especialidadeRepository, mapper);
    }

    private MedicoRequest request() {
        MedicoRequest r = new MedicoRequest();
        r.setNome("Maria Souza");
        r.setCpf("12345678901");
        r.setEmail("maria@clinica.com");
        r.setDataNascimento(LocalDate.of(1985, 4, 20));
        r.setCrm("CRM-123");
        r.setEspecialidadeId(5L);
        return r;
    }

    private Especialidade especialidade() {
        Especialidade e = new Especialidade();
        e.setId(5L);
        e.setNome("Cardiologia");
        return e;
    }

    @Test
    void criar_deveMapearERetornarComEspecialidade() {
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.existsByCrm("CRM-123")).thenReturn(false);
        when(especialidadeRepository.findById(5L)).thenReturn(Optional.of(especialidade()));
        when(repository.save(any(Medico.class))).thenAnswer(inv -> {
            Medico m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        MedicoResponse response = service.criar(request());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNome()).isEqualTo("Maria Souza");
        assertThat(response.getCrm()).isEqualTo("CRM-123");
        assertThat(response.getEspecialidade().getId()).isEqualTo(5L);
        assertThat(response.getEspecialidade().getNome()).isEqualTo("Cardiologia");
    }

    @Test
    void criar_cpfDuplicado_deveLancarConflict() {
        when(repository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void criar_crmDuplicado_deveLancarConflict() {
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.existsByCrm("CRM-123")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(ConflictException.class);
    }

    @Test
    void criar_especialidadeInexistente_deveLancarBusinessRule() {
        when(especialidadeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void buscar_inexistente_deveLancarNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizar_deveManterIdEAlterarDados() {
        Medico existente = new Medico();
        existente.setId(10L);
        existente.setNome("Antigo");
        when(repository.findById(10L)).thenReturn(Optional.of(existente));
        when(especialidadeRepository.findById(5L)).thenReturn(Optional.of(especialidade()));
        when(repository.save(existente)).thenReturn(existente);

        MedicoResponse response = service.atualizar(10L, request());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getNome()).isEqualTo("Maria Souza");
        assertThat(response.getEspecialidade().getId()).isEqualTo(5L);
    }
}
