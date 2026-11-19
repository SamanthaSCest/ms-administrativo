package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioBuscaRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Cargo;
import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
import br.edu.imepac.clinicalapi.administrativo.mapper.FuncionarioMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.Filtros;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.administrativo.utils.CpfMascara;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de funcionários: unicidade de CPF/matrícula e criação do usuário de acesso no grupo padrão
 * do cargo (RBAC). Mudar o cargo troca esse grupo; os demais acessos ficam como o administrador definiu.
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
        if (repository.existsByCpf(request.getCpf())) {
            throw new ConflictException("Já existe funcionário com o CPF informado");
        }
        if (repository.existsByMatricula(request.getMatricula())) {
            throw new ConflictException("Já existe funcionário com a matrícula: " + request.getMatricula());
        }
        Funcionario funcionario = mapper.toEntity(request);
        // Usuário de acesso (login = CPF) no grupo padrão do cargo (+ os grupos extras informados)
        Grupo grupoDoCargo = grupo(request.getCargo());
        funcionario.setUsuario(usuarioService.novoUsuario(request.getCpf(), request.getSenha(), grupoDoCargo));
        if (request.getGrupoIds() != null) {
            usuarioService.definirGrupos(funcionario.getUsuario(), grupoDoCargo, request.getGrupoIds());
        }
        Funcionario salvo = repository.save(funcionario);
        eventos.criado(salvo);
        return mapper.toResponse(salvo);
    }

    /** Listagem simples: só a situação (sem dado pessoal na URL). Sem situação, só os ativos. */
    @Transactional(readOnly = true)
    public Page<FuncionarioResponse> listar(Boolean ativo, Pageable pageable) {
        return pesquisar(new FuncionarioBuscaRequest(null, null, ativo), pageable);
    }

    /**
     * Busca por nome (parte, sem diferenciar maiúsculas), CPF completo e situação.
     * Listagens sempre devolvem o CPF mascarado (RN20); o CPF completo só sai no GET por id.
     */
    @Transactional(readOnly = true)
    public Page<FuncionarioResponse> buscar(FuncionarioBuscaRequest filtro, Pageable pageable) {
        return pesquisar(filtro, pageable);
    }

    private Page<FuncionarioResponse> pesquisar(FuncionarioBuscaRequest filtro, Pageable pageable) {
        Specification<Funcionario> spec = Specification.allOf(
                Filtros.ativo(filtro.ativo()),
                Filtros.nomeContem(filtro.nome()),
                Filtros.cpfIgual(filtro.cpf()));
        return repository.findAll(spec, pageable).map(mapper::toResponse).map(r -> {
            r.setCpf(CpfMascara.mascarar(r.getCpf()));
            return r;
        });
    }

    @Transactional(readOnly = true)
    public FuncionarioResponse buscar(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public FuncionarioResponse atualizar(Long id, FuncionarioRequest request) {
        log.info("Atualizando funcionário {}", id);
        Funcionario funcionario = buscarEntidade(id);
        Cargo cargoAnterior = funcionario.getCargo();
        if (repository.existsByCpfAndIdNot(request.getCpf(), id)) {
            throw new ConflictException("Já existe funcionário com o CPF informado");
        }
        if (repository.existsByMatriculaAndIdNot(request.getMatricula(), id)) {
            throw new ConflictException("Já existe funcionário com a matrícula: " + request.getMatricula());
        }
        var antes = eventos.retrato(mapper.toResponse(funcionario));
        mapper.updateEntity(request, funcionario);
        Grupo grupoNovo = grupo(request.getCargo());
        usuarioService.sincronizar(funcionario, request.getSenha(), grupoNovo);
        if (request.getGrupoIds() != null && funcionario.getUsuario() != null) {
            // Grupos informados: o do cargo (novo) + os extras, no lugar dos atuais
            usuarioService.definirGrupos(funcionario.getUsuario(), grupoNovo, request.getGrupoIds());
        } else if (cargoAnterior != request.getCargo()) {
            usuarioService.trocarGrupo(funcionario.getUsuario(), grupo(cargoAnterior), grupoNovo);
        }
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

    /** Cada cargo tem um grupo padrão com o mesmo nome (migration V14). */
    private Grupo grupo(Cargo cargo) {
        return usuarioService.grupoPadrao(cargo.name());
    }

    private Funcionario buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Funcionário", id));
    }
}
