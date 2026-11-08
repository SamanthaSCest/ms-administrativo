package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.ModelMapperConfig;
import br.edu.imepac.clinicalapi.administrativo.dto.EspecialidadeRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.EspecialidadeResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.mapper.EspecialidadeMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspecialidadeServiceTest {

    @Mock
    private EspecialidadeRepository repository;
    @Mock
    private MedicoRepository medicoRepository;

    private EspecialidadeService service;

    @BeforeEach
    void setUp() {
        EspecialidadeMapper mapper = new EspecialidadeMapper(new ModelMapperConfig().modelMapper());
        service = new EspecialidadeService(repository, medicoRepository, mapper);
    }

    private EspecialidadeRequest request(String nome) {
        EspecialidadeRequest r = new EspecialidadeRequest();
        r.setNome(nome);
        r.setDescricao("desc");
        return r;
    }

    @Test
    void criar_deveSalvarERetornarResponse() {
        when(repository.existsByNomeIgnoreCase("Cardiologia")).thenReturn(false);
        when(repository.save(any(Especialidade.class))).thenAnswer(inv -> {
            Especialidade e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EspecialidadeResponse response = service.criar(request("Cardiologia"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Cardiologia");
        assertThat(response.getDescricao()).isEqualTo("desc");
    }

    @Test
    void criar_nomeDuplicado_deveLancarConflict() {
        when(repository.existsByNomeIgnoreCase("Cardiologia")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request("Cardiologia"))).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void buscar_inexistente_deveLancarNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizar_deveAlterarCampos() {
        Especialidade existente = new Especialidade();
        existente.setId(1L);
        existente.setNome("Antigo");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Novo", 1L)).thenReturn(false);
        when(repository.save(existente)).thenReturn(existente);

        EspecialidadeResponse response = service.atualizar(1L, request("Novo"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNome()).isEqualTo("Novo");
    }

    @Test
    void remover_comMedicosVinculados_deveLancarConflict() {
        Especialidade existente = new Especialidade();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(medicoRepository.existsByEspecialidadeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.remover(1L)).isInstanceOf(ConflictException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void remover_semVinculos_deveExcluir() {
        Especialidade existente = new Especialidade();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(medicoRepository.existsByEspecialidadeId(1L)).thenReturn(false);

        service.remover(1L);

        verify(repository).delete(existente);
    }
}
