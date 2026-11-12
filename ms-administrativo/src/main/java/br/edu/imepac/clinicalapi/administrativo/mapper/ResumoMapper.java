package br.edu.imepac.clinicalapi.administrativo.mapper;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.ConvenioResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.CustoMedicoResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.DisponibilidadeResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.EspecialidadeResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.FuncionarioResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.InsumoResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.MedicoResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.ProcedimentoResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.ResponsavelLegalResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SalaResumo;
import br.edu.imepac.clinicalapi.administrativo.entities.Convenio;
import br.edu.imepac.clinicalapi.administrativo.entities.CustoMedico;
import br.edu.imepac.clinicalapi.administrativo.entities.CustoMedicoConsulta;
import br.edu.imepac.clinicalapi.administrativo.entities.CustoMedicoProcedimento;
import br.edu.imepac.clinicalapi.administrativo.entities.DisponibilidadeDiaria;
import br.edu.imepac.clinicalapi.administrativo.entities.DisponibilidadeExcecao;
import br.edu.imepac.clinicalapi.administrativo.entities.DisponibilidadeMedica;
import br.edu.imepac.clinicalapi.administrativo.entities.EntidadeBase;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import br.edu.imepac.clinicalapi.administrativo.entities.Insumo;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.entities.Procedimento;
import br.edu.imepac.clinicalapi.administrativo.entities.ResponsavelLegal;
import br.edu.imepac.clinicalapi.administrativo.entities.Sala;
import br.edu.imepac.clinicalapi.administrativo.utils.CpfMascara;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Converte entidades no DTO Resumo, usado na API de integração e no campo "dados" dos eventos.
 * Resumos nunca levam CPF completo (LGPD): paciente e responsável têm CPF mascarado; médico e funcionário, nenhum.
 */
@Component
public class ResumoMapper {

    /** Nome do agregado no tipo do evento (ex.: "paciente" em paciente.criado) e se o nome é feminino. */
    public record Agregado(String nome, boolean feminino) {
    }

    public Agregado agregado(EntidadeBase e) {
        return switch (e) {
            case Especialidade x -> new Agregado("especialidade", true);
            case Medico x -> new Agregado("medico", false);
            case Paciente x -> new Agregado("paciente", false);
            case Funcionario x -> new Agregado("funcionario", false);
            case Convenio x -> new Agregado("convenio", false);
            case Sala x -> new Agregado("sala", true);
            case ResponsavelLegal x -> new Agregado("responsavel-legal", false);
            case Insumo x -> new Agregado("insumo", false);
            case Procedimento x -> new Agregado("procedimento", false);
            case CustoMedico x -> new Agregado("custo-medico", false);
            case DisponibilidadeMedica x -> new Agregado("disponibilidade", true);
            default -> throw new IllegalArgumentException("Entidade sem evento: " + e.getClass().getSimpleName());
        };
    }

    public Object resumo(EntidadeBase e) {
        return switch (e) {
            case Especialidade x -> especialidade(x);
            case Medico x -> medico(x);
            case Paciente x -> paciente(x);
            case Funcionario x -> new FuncionarioResumo(x.getId(), x.getUuid(), x.getNome(), x.getMatricula(),
                    x.getCargo(), x.getSetor(), x.isAtivo());
            case Convenio x -> convenio(x);
            case Sala x -> sala(x);
            case ResponsavelLegal x -> new ResponsavelLegalResumo(x.getId(), x.getUuid(), x.getNome(),
                    CpfMascara.mascarar(x.getCpf()), x.getParentesco(), x.isAtivo());
            case Insumo x -> new InsumoResumo(x.getId(), x.getUuid(), x.getCodigo(), x.getNome(), x.getUnidadeMedida(),
                    x.getCustoUnitario(), x.isAtivo());
            case Procedimento x -> new ProcedimentoResumo(x.getId(), x.getUuid(), x.getCodigoTuss(), x.getNome(),
                    x.getEspecialidade().getId(), x.getDuracaoMinutos(), x.getValorBase(), x.isAtivo());
            case CustoMedico x -> custo(x);
            case DisponibilidadeMedica x -> disponibilidade(x);
            default -> throw new IllegalArgumentException("Entidade sem resumo: " + e.getClass().getSimpleName());
        };
    }

    public EspecialidadeResumo especialidade(Especialidade e) {
        return new EspecialidadeResumo(e.getId(), e.getUuid(), e.getNome(), e.isAtivo());
    }

    public MedicoResumo medico(Medico m) {
        var especialidades = m.getEspecialidades().stream()
                .map(this::especialidade)
                .sorted(Comparator.comparing(EspecialidadeResumo::nome))
                .toList();
        return new MedicoResumo(m.getId(), m.getUuid(), m.getNome(), m.getCrm(), m.getUfCrm(), especialidades, m.isAtivo());
    }

    public PacienteResumo paciente(Paciente p) {
        return new PacienteResumo(p.getId(), p.getNome(), CpfMascara.mascarar(p.getCpf()), p.getDataNascimento(),
                p.getUuid(), p.isAtivo());
    }

    public ConvenioResumo convenio(Convenio c) {
        return new ConvenioResumo(c.getId(), c.getUuid(), c.getNome(), c.getRegistroAns(), c.getCnpj(), c.isAtivo());
    }

    public SalaResumo sala(Sala s) {
        return new SalaResumo(s.getId(), s.getUuid(), s.getNumero(), s.getTipo(), s.getAndar(), s.getObservacoes(),
                s.isAtivo());
    }

    private CustoMedicoResumo custo(CustoMedico c) {
        Long especialidadeId = c instanceof CustoMedicoConsulta cc ? cc.getEspecialidade().getId() : null;
        Long procedimentoId = c instanceof CustoMedicoProcedimento cp ? cp.getProcedimento().getId() : null;
        return new CustoMedicoResumo(c.getId(), c.getUuid(), c.getTipo(), c.getMedico().getId(), especialidadeId,
                procedimentoId, c.getValor(), c.getVigenciaInicio(), c.getVigenciaFim(), c.isAtivo());
    }

    private DisponibilidadeResumo disponibilidade(DisponibilidadeMedica d) {
        var diaSemana = d instanceof DisponibilidadeDiaria diaria ? diaria.getDiaSemana() : null;
        DisponibilidadeExcecao excecao = d instanceof DisponibilidadeExcecao e ? e : null;
        return new DisponibilidadeResumo(d.getId(), d.getUuid(), d.getTipo(), d.getMedico().getId(),
                d.getSala() == null ? null : d.getSala().getId(), diaSemana,
                excecao == null ? null : excecao.getData(), d.getHoraInicial(), d.getHoraFinal(),
                excecao == null ? null : excecao.getDisponivel(), d.isAtivo());
    }
}
