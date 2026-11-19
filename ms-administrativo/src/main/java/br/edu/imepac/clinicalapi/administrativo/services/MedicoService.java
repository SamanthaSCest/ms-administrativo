package br.edu.imepac.clinicalapi.administrativo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoBuscaRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import br.edu.imepac.clinicalapi.administrativo.mapper.MedicoMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.Filtros;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.utils.CpfMascara;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Regras de negócio de Médico: unicidade de CPF e de CRM por UF, e existência das especialidades.
 */
@Service
@RequiredArgsConstructor
public class MedicoService {

    // Logger do SLF4J (API de logs) - o Spring Boot usa o Logback como implementação.
    // Níveis: debug (detalhe), info (fluxo normal), warn (erro do cliente), error (falha nossa).
    // Use sempre placeholders {} em vez de concatenar strings.
    private static final Logger log = LoggerFactory.getLogger(MedicoService.class);

    /** Grupo do Keycloak em que cada médico entra (keycloak/realm-clinical.json). */
    static final String GRUPO_MEDICO = "MEDICO";

    private final MedicoRepository repository;
    private final EspecialidadeRepository especialidadeRepository;
    private final MedicoMapper mapper;
    private final UsuarioService usuarioService;
    private final EventoService eventos;

    @Transactional
    public MedicoResponse criar(MedicoRequest request) {
        log.info("Criando médico CRM {}/{}", request.getCrm(), request.getUfCrm());
        // Regras de unicidade -> 409 Conflict
        if (repository.existsByCpf(request.getCpf())) {
            log.warn("CPF de médico já cadastrado");
            throw new ConflictException("Já existe médico com o CPF informado");
        }
        if (repository.existsByCrmAndUfCrm(request.getCrm(), request.getUfCrm())) {
            throw new ConflictException("Já existe médico com o CRM: " + request.getCrm() + "/" + request.getUfCrm());
        }
        Medico medico = mapper.toEntity(request);
        // O mapper não preenche as especialidades: buscamos pelos ids informados (422 se algum não existir)
        medico.setEspecialidades(buscarEspecialidades(request.getEspecialidadeIds(), Set.of()));
        // Usuário de acesso no Keycloak (login = CPF, grupo MEDICO); o vínculo é salvo em cascata com o médico
        medico.setUsuario(usuarioService.novoUsuario(medico, request.getSenha(), Set.of(GRUPO_MEDICO)));
        log.debug("Salvando médico {}", medico.getNome());
        Medico salvo = repository.save(medico);
        eventos.criado(salvo);
        return comGrupos(salvo);
    }

    /** Listagem simples: só a situação (sem dado pessoal na URL). Sem situação, só os ativos. */
    @Transactional(readOnly = true)
    public Page<MedicoResponse> listar(Boolean ativo, Pageable pageable) {
        return pesquisar(new MedicoBuscaRequest(null, null, null, null, ativo), pageable);
    }

    /**
     * Busca por nome (parte, sem diferenciar maiúsculas), CRM, UF do CRM, especialidade e situação.
     * Listagens sempre devolvem o CPF mascarado (RN20); o CPF completo só sai no GET por id.
     */
    @Transactional(readOnly = true)
    public Page<MedicoResponse> buscar(MedicoBuscaRequest filtro, Pageable pageable) {
        return pesquisar(filtro, pageable);
    }

    private Page<MedicoResponse> pesquisar(MedicoBuscaRequest filtro, Pageable pageable) {
        Specification<Medico> spec = Specification.allOf(
                Filtros.ativo(filtro.ativo()),
                Filtros.nomeContem(filtro.nome()),
                Filtros.crmIgual(filtro.crm()),
                Filtros.ufCrmIgual(filtro.ufCrm()),
                Filtros.atendeEspecialidade(filtro.especialidadeId()));
        return repository.findAll(spec, pageable).map(mapper::toResponse).map(r -> {
            r.setCpf(CpfMascara.mascarar(r.getCpf()));
            return r;
        });
    }

    @Transactional(readOnly = true)
    public MedicoResponse buscar(Long id) {
        return comGrupos(buscarEntidade(id));
    }

    @Transactional
    public MedicoResponse atualizar(Long id, MedicoRequest request) {
        log.info("Atualizando médico {}", id);
        Medico medico = buscarEntidade(id);
        if (repository.existsByCpfAndIdNot(request.getCpf(), id)) {
            throw new ConflictException("Já existe médico com o CPF informado");
        }
        if (repository.existsByCrmAndUfCrmAndIdNot(request.getCrm(), request.getUfCrm(), id)) {
            throw new ConflictException("Já existe médico com o CRM: " + request.getCrm() + "/" + request.getUfCrm());
        }
        var antes = eventos.retrato(mapper.toResponse(medico));
        // Copia os campos do request para a entidade já existente (mantém o id)
        mapper.updateEntity(request, medico);
        medico.setEspecialidades(buscarEspecialidades(request.getEspecialidadeIds(), medico.getEspecialidades()));
        usuarioService.sincronizar(medico, request.getSenha(), Set.of(GRUPO_MEDICO));
        Medico salvo = repository.save(medico);
        MedicoResponse depois = mapper.toResponse(salvo);
        eventos.atualizado(salvo, antes, depois);
        return comGrupos(salvo);
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo médico {}", id);
        Medico medico = buscarEntidade(id);
        // Custos e disponibilidades saem junto (ON DELETE CASCADE); o evento medico.removido cobre os três
        eventos.removido(medico);
        usuarioService.removerDepoisDoCommit(medico.getUsuario());
        repository.delete(medico);
    }

    /** Para outros services (custos, disponibilidade): médico do corpo inexistente ou inativo -> 422. */
    @Transactional(readOnly = true)
    public Medico buscarReferencia(Long id) {
        Medico medico = buscarExistente(id);
        if (!medico.isAtivo()) {
            throw new BusinessRuleException("Médico inativo: " + id);
        }
        return medico;
    }

    /** Médico informado existe (422 se não)? Não exige que esteja ativo. */
    @Transactional(readOnly = true)
    public Medico buscarExistente(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessRuleException("Médico não encontrado: " + id));
    }

    private Medico buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Médico", id));
    }

    /**
     * Resolve as especialidades pelos ids (422 se alguma não existir). Especialidade inativa só é aceita
     * se o médico já a tinha (não cria vínculo novo com cadastro inativo).
     */
    private Set<Especialidade> buscarEspecialidades(Set<Long> ids, Set<Especialidade> atuais) {
        Set<Long> idsAtuais = new HashSet<>();
        atuais.forEach(e -> idsAtuais.add(e.getId()));
        Set<Especialidade> especialidades = new HashSet<>();
        for (Long id : ids) {
            Especialidade especialidade = especialidadeRepository.findById(id)
                    // 422 (e não 404): o recurso da URL existe, mas um dado do corpo referencia algo inválido
                    .orElseThrow(() -> new BusinessRuleException("Especialidade não encontrada: " + id));
            if (!especialidade.isAtivo() && !idsAtuais.contains(id)) {
                throw new BusinessRuleException("Especialidade inativa: " + especialidade.getNome());
            }
            especialidades.add(especialidade);
        }
        return especialidades;
    }

    /** Detalhe: o usuário vem com os grupos atuais do Keycloak. */
    private MedicoResponse comGrupos(Medico medico) {
        MedicoResponse r = mapper.toResponse(medico);
        if (r.getUsuario() != null) {
            r.getUsuario().setGrupos(usuarioService.grupos(medico.getUsuario()));
        }
        return r;
    }
}
