package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Médico = Pessoa + CRM/UF + especialidades (muitos para muitos, tabela medico_especialidades).
 * Tem usuário de acesso com perfil MEDICO.
 */
@Getter
@Setter
@Entity
@Table(name = "medicos")
public class Medico extends Pessoa implements PossuiUsuario {

    // CRM é único por UF (o mesmo número pode existir em estados diferentes)
    @Column(nullable = false, length = 20)
    private String crm;

    @Enumerated(EnumType.STRING)
    @Column(name = "uf_crm", length = 2)
    private UF ufCrm;

    // Diagrama: Set<Especialidade> especialidades (1..*). O médico é o lado dono da relação.
    @ManyToMany
    @JoinTable(name = "medico_especialidades",
            joinColumns = @JoinColumn(name = "medico_id"),
            inverseJoinColumns = @JoinColumn(name = "especialidade_id"))
    private Set<Especialidade> especialidades = new HashSet<>();

    // Credenciais de acesso. cascade ALL + orphanRemoval: o usuário nasce e morre junto com o médico.
    // Pode ser null apenas em registros anteriores à migration V3.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    public void adicionarEspecialidade(Especialidade especialidade) {
        especialidades.add(especialidade);
    }

    public void removerEspecialidade(Especialidade especialidade) {
        especialidades.removeIf(e -> e.getId() != null && e.getId().equals(especialidade.getId()));
    }

    /** true se o médico atende a especialidade (comparação por id). */
    public boolean atende(Especialidade especialidade) {
        return especialidade != null && especialidades.stream()
                .anyMatch(e -> e.getId() != null && e.getId().equals(especialidade.getId()));
    }
}
