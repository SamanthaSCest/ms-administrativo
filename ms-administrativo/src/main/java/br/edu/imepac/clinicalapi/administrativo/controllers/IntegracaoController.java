package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.AtendeResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.ConvenioResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.EspecialidadeResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.MedicoResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SalaResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoSala;
import br.edu.imepac.clinicalapi.administrativo.services.IntegracaoService;
import br.edu.imepac.clinicalapi.administrativo.services.PacienteIntegracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de integração para outros microserviços: somente leitura (RN18), acesso com X-Api-Key (perfil SERVICO)
 * ou ADMIN. As rotas do paciente por id ficam no PacienteIntegracaoController.
 * Contrato: static/contract/openapi-administrativo.yaml (tag Integração).
 */
@RestController
@RequestMapping("/integracao")
@RequiredArgsConstructor
public class IntegracaoController {

    private final IntegracaoService service;
    private final PacienteIntegracaoService pacienteService;

    /** Busca em lote: GET /integracao/pacientes?ids=1,2,3 (até 100). */
    @GetMapping("/pacientes")
    public List<PacienteResumo> pacientes(@RequestParam List<Long> ids) {
        return pacienteService.buscarLote(ids);
    }

    @GetMapping("/especialidades")
    public List<EspecialidadeResumo> especialidades() {
        return service.especialidadesAtivas();
    }

    /** GET /integracao/medicos?ids=1,2 e/ou ?especialidadeId=3 */
    @GetMapping("/medicos")
    public List<MedicoResumo> medicos(@RequestParam(required = false) List<Long> ids,
                                      @RequestParam(required = false) Long especialidadeId) {
        return service.medicos(ids, especialidadeId);
    }

    @GetMapping("/medicos/{id}")
    public MedicoResumo medico(@PathVariable Long id) {
        return service.medico(id);
    }

    @GetMapping("/medicos/{id}/situacao")
    public SituacaoResponse situacaoMedico(@PathVariable Long id) {
        return service.situacaoMedico(id);
    }

    @GetMapping("/medicos/{id}/atende")
    public AtendeResponse atende(@PathVariable Long id, @RequestParam Long especialidadeId) {
        return service.atende(id, especialidadeId);
    }

    /** Salas ativas; ?tipo=CONSULTORIO|CIRURGIA|PROCEDIMENTO filtra. */
    @GetMapping("/salas")
    public List<SalaResumo> salas(@RequestParam(required = false) TipoSala tipo) {
        return service.salasAtivas(tipo);
    }

    @GetMapping("/salas/{id}")
    public SalaResumo sala(@PathVariable Long id) {
        return service.sala(id);
    }

    @GetMapping("/convenios")
    public List<ConvenioResumo> convenios() {
        return service.conveniosAtivos();
    }

    @GetMapping("/convenios/{id}")
    public ConvenioResumo convenio(@PathVariable Long id) {
        return service.convenio(id);
    }
}
