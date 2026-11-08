package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.ModelMapperConfig;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.mapper.PacienteMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
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
class PacienteServiceTest {

    @Mock
    private PacienteRepository repository;

    private PacienteService service;

    @BeforeEach
    void setUp() {
        service = new PacienteService(repository, new PacienteMapper(new ModelMapperConfig().modelMapper()));
    }

    private PacienteRequest request() {
        PacienteRequest r = new PacienteRequest();
        r.setNome("João Pereira");
        r.setCpf("98765432100");
        r.setDataNascimento(LocalDate.of(1990, 1, 15));
        return r;
    }

    @Test
    void criar_deveSalvar() {
        when(repository.existsByCpf("98765432100")).thenReturn(false);
        when(repository.save(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(3L);
            return p;
        });

        PacienteResponse response = service.criar(request());

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getNome()).isEqualTo("João Pereira");
        assertThat(response.getDataNascimento()).isEqualTo(LocalDate.of(1990, 1, 15));
    }

    @Test
    void criar_cpfDuplicado_deveLancarConflict() {
        when(repository.existsByCpf("98765432100")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void buscar_inexistente_deveLancarNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void remover_deveExcluir() {
        Paciente p = new Paciente();
        when(repository.findById(1L)).thenReturn(Optional.of(p));

        service.remover(1L);

        verify(repository).delete(p);
    }
}
