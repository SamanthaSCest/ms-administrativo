package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.ModelMapperConfig;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.GrupoResumo;
import br.edu.imepac.clinicalapi.administrativo.entities.Cargo;
import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private static Grupo grupo(long id, String nome) {
        Grupo g = new Grupo();
        g.setId(id);
        g.setNome(nome);
        g.setPadrao(true);
        return g;
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
    void criar_recepcionista_criaUsuarioNoGrupoDoCargo() {
        Grupo recepcionista = grupo(2L, "RECEPCIONISTA");
        when(usuarioService.grupoPadrao("RECEPCIONISTA")).thenReturn(recepcionista);
        Usuario usuario = new Usuario();
        usuario.setLogin("11122233396");
        usuario.adicionarGrupo(recepcionista);
        when(usuarioService.novoUsuario("11122233396", "funcSenha123", recepcionista)).thenReturn(usuario);
        when(repository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(7L);
            return f;
        });

        FuncionarioResponse response = service.criar(request());

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getMatricula()).isEqualTo("F-001");
        assertThat(response.getCargo()).isEqualTo(Cargo.RECEPCIONISTA);
        assertThat(response.getUsuario().getGrupos()).containsExactly(new GrupoResumo(2L, "RECEPCIONISTA"));
    }

    @Test
    void criar_administrador_criaUsuarioNoGrupoAdministrador() {
        FuncionarioRequest r = request();
        r.setCargo(Cargo.ADMINISTRADOR);
        Grupo administrador = grupo(1L, "ADMINISTRADOR");
        when(usuarioService.grupoPadrao("ADMINISTRADOR")).thenReturn(administrador);
        when(repository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(r);

        verify(usuarioService).novoUsuario("11122233396", "funcSenha123", administrador);
    }

    @Test
    void atualizar_trocandoCargo_trocaOGrupoDoCargo() {
        Grupo recepcionista = grupo(2L, "RECEPCIONISTA");
        Grupo estoquista = grupo(3L, "ESTOQUISTA");
        Usuario usuario = new Usuario();
        usuario.setLogin("11122233396");
        usuario.adicionarGrupo(recepcionista);
        Funcionario atual = new Funcionario();
        atual.setId(7L);
        atual.setCpf("11122233396");
        atual.setCargo(Cargo.RECEPCIONISTA);
        atual.setUsuario(usuario);
        when(repository.findById(7L)).thenReturn(Optional.of(atual));
        when(usuarioService.grupoPadrao("RECEPCIONISTA")).thenReturn(recepcionista);
        when(usuarioService.grupoPadrao("ESTOQUISTA")).thenReturn(estoquista);
        when(repository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        FuncionarioRequest r = request();
        r.setCargo(Cargo.ESTOQUISTA);
        service.atualizar(7L, r);

        verify(usuarioService).trocarGrupo(eq(usuario), eq(recepcionista), eq(estoquista));
    }

    @Test
    void criar_matriculaDuplicada_deveLancarConflict() {
        when(repository.existsByCpf("11122233396")).thenReturn(false);
        when(repository.existsByMatricula("F-001")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(request())).isInstanceOf(ConflictException.class);
        verify(usuarioService, never()).novoUsuario(any(), any(), any());
    }
}
