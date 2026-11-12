package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.StatusResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.EntidadeBase;
import br.edu.imepac.clinicalapi.administrativo.entities.PossuiUsuario;
import br.edu.imepac.clinicalapi.administrativo.repositories.ConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.CustoMedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.DisponibilidadeMedicaRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.InsumoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ProcedimentoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ResponsavelLegalRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.SalaRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Ativa/inativa qualquer cadastro que estende EntidadeBase (inativar() / reativar()).
 * Inativar não apaga: o registro continua consultável, mas não pode ser usado em novos vínculos.
 * Para médico e funcionário, o usuário de acesso acompanha o status (inativo não faz login).
 */
@Service
public class AtivacaoService {

    private static final Logger log = LoggerFactory.getLogger(AtivacaoService.class);

    /** Nome do recurso na URL -> repositório. Os mesmos nomes do PATCH /{recurso}/{id}/status. */
    private final Map<String, JpaRepository<? extends EntidadeBase, Long>> repositorios;
    private final EventoService eventos;

    public AtivacaoService(EspecialidadeRepository especialidades, MedicoRepository medicos,
                           PacienteRepository pacientes, FuncionarioRepository funcionarios,
                           ConvenioRepository convenios, SalaRepository salas,
                           ResponsavelLegalRepository responsaveis, InsumoRepository insumos,
                           ProcedimentoRepository procedimentos, CustoMedicoRepository custos,
                           DisponibilidadeMedicaRepository disponibilidades, EventoService eventos) {
        this.eventos = eventos;
        this.repositorios = Map.ofEntries(
                Map.entry("especialidades", especialidades),
                Map.entry("medicos", medicos),
                Map.entry("pacientes", pacientes),
                Map.entry("funcionarios", funcionarios),
                Map.entry("convenios", convenios),
                Map.entry("salas", salas),
                Map.entry("responsaveis", responsaveis),
                Map.entry("insumos", insumos),
                Map.entry("procedimentos", procedimentos),
                Map.entry("custos-medicos", custos),
                Map.entry("disponibilidades", disponibilidades));
    }

    @Transactional
    public StatusResponse alterarStatus(String recurso, Long id, boolean ativo, String loginAutenticado) {
        JpaRepository<? extends EntidadeBase, Long> repositorio = repositorios.get(recurso);
        if (repositorio == null) {
            throw new ResourceNotFoundException("Recurso não encontrado: " + recurso);
        }
        EntidadeBase entidade = repositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de " + recurso + " não encontrado: " + id));

        if (entidade instanceof PossuiUsuario pessoa && pessoa.getUsuario() != null) {
            // Evita que o administrador se tranque fora inativando o próprio cadastro
            if (!ativo && pessoa.getUsuario().getLogin().equals(loginAutenticado)) {
                throw new BusinessRuleException("Não é possível inativar o próprio cadastro");
            }
            pessoa.getUsuario().setAtivo(ativo);
        }
        boolean mudou = entidade.isAtivo() != ativo;
        if (ativo) {
            entidade.reativar();
        } else {
            entidade.inativar();
        }
        log.info("{} {} {}", recurso, id, ativo ? "reativado" : "inativado");
        // saveAndFlush: grava agora para a resposta já trazer atualizadoPor/dataModificacao
        EntidadeBase salva = salvar(repositorio, entidade);
        // Evento <agregado>.inativado/reativado só quando o status muda de fato (PATCH repetido não publica)
        if (mudou) {
            eventos.statusAlterado(salva);
        }
        return new StatusResponse(recurso, salva.getId(), salva.getUuid(), salva.isAtivo(), salva.getAtualizadoPor(),
                salva.getDataModificacao());
    }

    @SuppressWarnings("unchecked")
    private static <T extends EntidadeBase> T salvar(JpaRepository<? extends EntidadeBase, Long> repositorio, T entidade) {
        return ((JpaRepository<T, Long>) repositorio).saveAndFlush(entidade);
    }
}
