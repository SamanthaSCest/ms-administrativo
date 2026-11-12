package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import br.edu.imepac.clinicalapi.administrativo.entities.Perfil;
import br.edu.imepac.clinicalapi.administrativo.mapper.FuncionarioMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de funcionários (somente ADMIN): unicidade de CPF/matrícula e criação do usuário de acesso
 * com o perfil definido pelo cargo (ADMINISTRADOR -> ADMIN; demais -> FUNCIONARIO).
 */
@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private static final Logger log = LoggerFactory.getLogger(FuncionarioService.class);

    private final FuncionarioRepository repository;
    private final FuncionarioMapper mapper;
    private final UsuarioService usuarioService;
    private final EventoService eventos;

    @Transactional
    public FuncionarioResponse criar(FuncionarioRequest request) {
        log.info("Criando funcionário matrícula {}", request.getMatricula());
        Perfil perfil = perfil(request);
        if (repository.existsByCpf(request.getCpf())) {
            throw new ConflictException("Já existe funcionário com o CPF informado");
        }
        if (repository.existsByMatricula(request.getMatricula())) {
            throw new ConflictException("Já existe funcionário com a matrícula: " + request.getMatricula());
        }
        Funcionario funcionario = mapper.toEntity(request);
        funcionario.setUsuario(usuarioService.novoUsuario(request.getCpf(), request.getSenha(), perfil));
        Funcionario salvo = repository.save(funcionario);
        eventos.criado(salvo);
        return mapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public Page<FuncionarioResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FuncionarioResponse buscar(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public FuncionarioResponse atualizar(Long id, FuncionarioRequest request) {
        log.info("Atualizando funcionário {}", id);
        Perfil perfil = perfil(request);
        Funcionario funcionario = buscarEntidade(id);
        if (repository.existsByCpfAndIdNot(request.getCpf(), id)) {
            throw new ConflictException("Já existe funcionário com o CPF informado");
        }
        if (repository.existsByMatriculaAndIdNot(request.getMatricula(), id)) {
            throw new ConflictException("Já existe funcionário com a matrícula: " + request.getMatricula());
        }
        var antes = eventos.retrato(mapper.toResponse(funcionario));
        mapper.updateEntity(request, funcionario);
        usuarioService.sincronizar(funcionario, request.getSenha(), perfil);
        Funcionario salvo = repository.save(funcionario);
        FuncionarioResponse depois = mapper.toResponse(salvo);
        eventos.atualizado(salvo, antes, depois);
        return depois;
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo funcionário {}", id);
        Funcionario funcionario = buscarEntidade(id);
        eventos.removido(funcionario);
        // O usuário de acesso é removido em cascata (usuario: orphanRemoval)
        repository.delete(funcionario);
    }

    /** O nível de acesso vem do cargo: ADMINISTRADOR -> ADMIN; demais -> FUNCIONARIO. */
    private Perfil perfil(FuncionarioRequest request) {
        return request.getCargo().perfil();
    }

    private Funcionario buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Funcionário", id));
    }
}
