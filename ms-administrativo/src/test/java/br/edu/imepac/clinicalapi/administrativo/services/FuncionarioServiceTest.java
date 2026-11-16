package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.ModelMapperConfig;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Cargo;
import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import br.edu.imepac.clinicalapi.administrativo.entities.Perfil;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import br.edu.imepac.clinicalapi.administrativo.mapper.FuncionarioMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock
    private EventoService eventos;

    @Mock
    private FuncionarioRepository repository;
    @Mock
    private UsuarioService usuarioService;

    private FuncionarioService service;

    @BeforeEach
    void setUp() {
        service = new FuncionarioService(repository, new FuncionarioMapper(new ModelMapperConfig().modelMapper()),
                usuarioService, eventos);
    }

    private FuncionarioRequest request() {
        FuncionarioRequest r = new FuncionarioRequest();
        r.setNome("Ana Lima");
        r.setCpf("11122233396");
        r.setDataNascimento(LocalDate.of(1992, 3, 10));
        r.setMatricula("F-001");
        r.setCargo(Cargo.RECEPCIONISTA);
        r.setDataAdmissao(LocalDate.of(2024, 2, 1));
        r.setSenha("funcSenha123");
        return r;
    }

    @Test
    void criar_recepcionista_criaUsuarioFuncionario() {
        Usuario usuario = new Usuario();
        usuario.setLogin("11122233396");
        usuario.setPerfil(Perfil.FUNCIONARIO);
        when(usuarioService.novoUsuario("11122233396", "funcSenha123", Perfil.FUNCIONARIO)).thenReturn(usuario);
        when(repository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(7L);
            return f;
        });

        FuncionarioResponse response = service.criar(request());

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getMatricula()).isEqualTo("F-001");
        assertThat(response.getCargo()).isEqualTo(Cargo.RECEPCIONISTA);
        assertThat(response.getUsuario().getPerfil()).isEqualTo(Perfil.FUNCIONARIO);
    }

    @Test
    void criar_administrador_criaUsuarioAdmin() {
        FuncionarioRequest r = request();
        r.setCargo(Cargo.ADMINISTRADOR);
        when(repository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(r);

        verify(usuarioService).novoUsuario("11122233396", "funcSenha123", Perfil.ADMIN);
    }

    @Test
    void criar_matriculaDuplicada_deveLancarConflict() {
        when(repository.existsByCpf("11122233396")).thenReturn(false);
        when(repository.existsByMatricula("F-001")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(ConflictException.class);
        verify(usuarioService, never()).novoUsuario(any(), any(), any());
    }
}
