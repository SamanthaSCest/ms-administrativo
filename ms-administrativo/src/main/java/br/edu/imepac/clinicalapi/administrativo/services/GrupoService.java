package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.GrupoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.GrupoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
import br.edu.imepac.clinicalapi.administrativo.repositories.GrupoRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
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
 * Grupos de acesso do RBAC. Regras:
 * <ul>
 *   <li>nome único (409);</li>
 *   <li>grupo padrão (de cargo ou MEDICO) não pode ser renomeado nem removido: o cadastro de funcionário e de
 *       médico o procura pelo nome (422);</li>
 *   <li>o grupo ADMINISTRADOR não pode ser alterado: mantém todas as permissões, para ninguém se trancar fora (422);</li>
 *   <li>grupo com usuários não pode ser removido (409).</li>
 * </ul>
 * Mudanças nas permissões valem a partir do próximo login de cada usuário do grupo.
 */
@Service
@RequiredArgsConstructor
public class GrupoService {

    private static final Logger log = LoggerFactory.getLogger(GrupoService.class);

    private final GrupoRepository repository;

    @Transactional
    public GrupoResponse criar(GrupoRequest request) {
        String nome = request.nome().trim();
        if (repository.existsByNome(nome)) {
            throw new ConflictException("Já existe grupo com o nome: " + nome);
        }
        Grupo grupo = new Grupo();
        grupo.setNome(nome);
        grupo.setDescricao(request.descricao());
        grupo.setPadrao(false);
        grupo.definirPermissoes(request.permissoes());
        log.info("Criando grupo de acesso {}", nome);
        return GrupoResponse.de(repository.save(grupo));
    }

    @Transactional(readOnly = true)
    public Page<GrupoResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(GrupoResponse::de);
    }

    @Transactional(readOnly = true)
    public GrupoResponse buscar(Long id) {
        return GrupoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public GrupoResponse atualizar(Long id, GrupoRequest request) {
        Grupo grupo = buscarEntidade(id);
        String nome = request.nome().trim();
        if (Grupo.ADMINISTRADOR.equals(grupo.getNome())) {
            throw new BusinessRuleException("O grupo ADMINISTRADOR não pode ser alterado");
        }
        if (grupo.isPadrao() && !grupo.getNome().equals(nome)) {
            throw new BusinessRuleException("Grupo padrão não pode ser renomeado");
        }
        if (repository.existsByNomeAndIdNot(nome, id)) {
            throw new ConflictException("Já existe grupo com o nome: " + nome);
        }
        grupo.setNome(nome);
        grupo.setDescricao(request.descricao());
        grupo.definirPermissoes(request.permissoes());
        log.info("Atualizando grupo de acesso {}: permissões {}", id, request.permissoes());
        return GrupoResponse.de(repository.save(grupo));
    }

    @Transactional
    public void remover(Long id) {
        Grupo grupo = buscarEntidade(id);
        if (grupo.isPadrao()) {
            throw new BusinessRuleException("Grupo padrão não pode ser removido");
        }
        long usuarios = repository.contarUsuarios(id);
        if (usuarios > 0) {
            throw new ConflictException("Grupo com " + usuarios + " usuário(s) não pode ser removido");
        }
        log.info("Removendo grupo de acesso {}", id);
        repository.delete(grupo);
    }

    private Grupo buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Grupo", id));
    }
}
