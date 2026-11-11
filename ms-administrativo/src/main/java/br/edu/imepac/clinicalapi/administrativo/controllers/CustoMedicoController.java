package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.CustoMedicoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.CustoMedicoResponse;
import br.edu.imepac.clinicalapi.administrativo.services.CustoMedicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;

/** Custos dos médicos (escrita só ADMIN). Contrato: static/contract/openapi-administrativo.yaml */
@RestController
@RequestMapping("/custos-medicos")
@RequiredArgsConstructor
public class CustoMedicoController {

    private final CustoMedicoService service;

    @PostMapping
    public ResponseEntity<CustoMedicoResponse> criar(@Valid @RequestBody CustoMedicoRequest request) {
        CustoMedicoResponse criado = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.id()).toUri()).body(criado);
    }

    /** Lista todos ou só os do médico informado. */
    @GetMapping
    public Page<CustoMedicoResponse> listar(@RequestParam(required = false) Long medicoId, Pageable pageable) {
        return service.listar(medicoId, pageable);
    }

    /** Custo vigente na data (padrão: hoje) para uma especialidade (consulta) ou um procedimento. */
    @GetMapping("/vigente")
    public CustoMedicoResponse vigente(@RequestParam Long medicoId,
                                       @RequestParam(required = false) Long especialidadeId,
                                       @RequestParam(required = false) Long procedimentoId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.buscarVigente(medicoId, especialidadeId, procedimentoId, data == null ? LocalDate.now() : data);
    }

    @GetMapping("/{id}")
    public CustoMedicoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public CustoMedicoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CustoMedicoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
