package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Vínculo do paciente com um convênio (RF06, HU04): número da carteirinha e validade.
 * O plano fica com outro grupo, então o vínculo é direto com o convênio.
 * Desvincular = inativar (EntidadeBase): o histórico continua consultável.
 */
@Getter
@Setter
@Entity
@Table(name = "pacientes_convenios")
public class PacienteConvenio extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false, updatable = false)
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "convenio_id", nullable = false)
    private Convenio convenio;

    @Column(name = "numero_carteirinha", nullable = false, length = 30)
    private String numeroCarteirinha;

    /** Validade da carteirinha; sem validade = não vence. */
    private LocalDate validade;

    /** A carteirinha vale na data? (sem validade, sempre vale) */
    public boolean carteirinhaValidaEm(LocalDate data) {
        return validade == null || !data.isAfter(validade);
    }

    /**
     * Vigente na data (RN16): vínculo ativo, convênio ativo e carteirinha válida.
     * Inativar o convênio não mexe no vínculo, mas ele deixa de ser vigente.
     */
    public boolean isVigente(LocalDate data) {
        return isAtivo() && convenio.isAtivo() && carteirinhaValidaEm(data);
    }
}
