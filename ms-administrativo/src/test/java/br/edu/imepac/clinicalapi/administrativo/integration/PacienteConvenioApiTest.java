package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.entities.OutboxEvento;
import br.edu.imepac.clinicalapi.administrativo.repositories.ConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.OutboxRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ResponsavelLegalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Vínculo do paciente com convênio (RF06, HU04, RN05 a RN07, RN16) e integração (RF24). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class PacienteConvenioApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PacienteConvenioRepository vinculoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private ConvenioRepository convenioRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private ResponsavelLegalRepository responsavelRepository;
    @Autowired
    private OutboxRepository outboxRepository;

    private long paciente;
    private long unimed;
    private long amil;

    @BeforeEach
    void preparar() throws Exception {
        limpar();
        paciente = id(criar("/pacientes", """
                {"nome":"Ana Lima","cpf":"12345678909","dataNascimento":"1990-01-01","telefone":"34999990000"}
                """));
        unimed = id(criar("/convenios", convenioJson("Unimed", "123456", "11222333000181")));
        amil = id(criar("/convenios", convenioJson("Amil", "654321", "11444777000161")));
    }

    // Os outros testes apagam pacientes e convênios: os vínculos (FK) saem antes
    @AfterEach
    void limpar() {
        vinculoRepository.deleteAll();
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        responsavelRepository.deleteAll();
        convenioRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    private static String convenioJson(String nome, String ans, String cnpj) {
        return """
                {"nome":"%s","registroAns":"%s","cnpj":"%s"}
                """.formatted(nome, ans, cnpj);
    }

    private static String vinculoJson(long convenioId, String carteirinha, LocalDate validade) {
        return """
                {"convenioId":%d,"numeroCarteirinha":"%s","validade":%s}
                """.formatted(convenioId, carteirinha, validade == null ? "null" : "\"" + validade + "\"");
    }

    private MvcResult criar(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
    }

    private long id(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions vincular(long convenioId, String carteirinha, LocalDate validade) throws Exception {
        return mockMvc.perform(post("/pacientes/{id}/convenios", paciente).contentType(MediaType.APPLICATION_JSON)
                .content(vinculoJson(convenioId, carteirinha, validade)));
    }

    private long vincularOk(long convenioId) throws Exception {
        return id(vincular(convenioId, "0001", LocalDate.now().plusYears(1)).andExpect(status().isCreated()).andReturn());
    }

    private void inativarConvenio(long convenioId) throws Exception {
        mockMvc.perform(patch("/convenios/{id}/status", convenioId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
    }

    private ResultActions vigentes(String data) throws Exception {
        var req = get("/integracao/pacientes/{id}/convenios/vigentes", paciente).with(user("agenda").roles("SERVICO"));
        return mockMvc.perform(data == null ? req : req.param("data", data));
    }

    private List<OutboxEvento> eventosDoVinculo() {
        return outboxRepository.findAll().stream()
                .filter(e -> e.getTipo().startsWith("paciente.convenio-"))
                .sorted(Comparator.comparing(OutboxEvento::getCriadoEm))
                .toList();
    }

    @Test
    void vincular_retorna201ComLocation_eVigente() throws Exception {
        LocalDate validade = LocalDate.now().plusYears(1);
        vincular(unimed, " 0001-ABC ", validade)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/pacientes/" + paciente + "/convenios/")))
                .andExpect(jsonPath("$.pacienteId").value(paciente))
                .andExpect(jsonPath("$.convenio.id").value(unimed))
                .andExpect(jsonPath("$.convenio.nome").value("Unimed"))
                .andExpect(jsonPath("$.numeroCarteirinha").value("0001-ABC"))
                .andExpect(jsonPath("$.validade").value(validade.toString()))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.criadoPor").value("admin"));
    }

    @Test
    void rn05_doisVinculosAtivosNoMesmoConvenio_retorna409() throws Exception {
        vincularOk(unimed);
        vincular(unimed, "0002", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ADM-PCV-002"));
        // Outro convênio pode
        vincular(amil, "0003", null).andExpect(status().isCreated());
    }

    @Test
    void rn05_depoisDeDesvincular_podeVincularDeNovo() throws Exception {
        long vinculo = vincularOk(unimed);
        mockMvc.perform(delete("/pacientes/{id}/convenios/{v}", paciente, vinculo)).andExpect(status().isNoContent());
        vincular(unimed, "0002", null).andExpect(status().isCreated());

        // O desvinculado continua no histórico, depois dos ativos
        mockMvc.perform(get("/pacientes/{id}/convenios", paciente))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ativo").value(true))
                .andExpect(jsonPath("$[1].id").value(vinculo))
                .andExpect(jsonPath("$[1].ativo").value(false))
                .andExpect(jsonPath("$[1].vigente").value(false));
    }

    @Test
    void rn06_convenioInativoOuInexistente_retorna422() throws Exception {
        inativarConvenio(unimed);
        vincular(unimed, "0001", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ADM-PCV-001"));
        vincular(999999, "0001", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ADM-PCV-001"));
    }

    @Test
    void rn07_carteirinhaVencida_retorna422_venceHojeAindaVale() throws Exception {
        vincular(unimed, "0001", LocalDate.now().minusDays(1))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ADM-PCV-003"));
        vincular(unimed, "0001", LocalDate.now()).andExpect(status().isCreated());
    }

    @Test
    void pacienteInativo_naoVincula() throws Exception {
        mockMvc.perform(patch("/pacientes/{id}/status", paciente).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
        vincular(unimed, "0001", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ADM-PCV-004"));
    }

    @Test
    void camposObrigatorios_retorna400() throws Exception {
        mockMvc.perform(post("/pacientes/{id}/convenios", paciente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCarteirinha\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos", hasSize(2)));
    }

    @Test
    void pacienteOuVinculoInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/pacientes/{id}/convenios", 999999)).andExpect(status().isNotFound());
        mockMvc.perform(get("/pacientes/{id}/convenios/{v}", paciente, 999999)).andExpect(status().isNotFound());

        // Vínculo de outro paciente também é 404
        long vinculo = vincularOk(unimed);
        long outro = id(criar("/pacientes", """
                {"nome":"Bia","cpf":"11145678920","dataNascimento":"1990-01-01","telefone":"34999990000"}
                """));
        mockMvc.perform(get("/pacientes/{id}/convenios/{v}", outro, vinculo)).andExpect(status().isNotFound());
    }

    @Test
    void atualizar_trocaCarteirinhaEConvenio_eRegrasValem() throws Exception {
        long vinculo = vincularOk(unimed);
        long outro = vincularOk(amil);

        mockMvc.perform(put("/pacientes/{id}/convenios/{v}", paciente, vinculo).contentType(MediaType.APPLICATION_JSON)
                        .content(vinculoJson(unimed, "9999", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCarteirinha").value("9999"))
                .andExpect(jsonPath("$.validade").doesNotExist());

        // Trocar para um convênio que já tem vínculo ativo: 409
        mockMvc.perform(put("/pacientes/{id}/convenios/{v}", paciente, vinculo).contentType(MediaType.APPLICATION_JSON)
                        .content(vinculoJson(amil, "9999", null)))
                .andExpect(status().isConflict());

        // Vínculo desvinculado não muda mais
        mockMvc.perform(delete("/pacientes/{id}/convenios/{v}", paciente, outro)).andExpect(status().isNoContent());
        mockMvc.perform(put("/pacientes/{id}/convenios/{v}", paciente, outro).contentType(MediaType.APPLICATION_JSON)
                        .content(vinculoJson(amil, "1", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ADM-PCV-005"));
    }

    @Test
    void eventos_vinculadoAtualizadoDesvinculado() throws Exception {
        long vinculo = vincularOk(unimed);
        String json = vinculoJson(unimed, "7777", LocalDate.now().plusYears(1));
        mockMvc.perform(put("/pacientes/{id}/convenios/{v}", paciente, vinculo)
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
        // PUT sem mudança não publica
        mockMvc.perform(put("/pacientes/{id}/convenios/{v}", paciente, vinculo)
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
        mockMvc.perform(delete("/pacientes/{id}/convenios/{v}", paciente, vinculo)).andExpect(status().isNoContent());
        // Desvincular de novo não publica
        mockMvc.perform(delete("/pacientes/{id}/convenios/{v}", paciente, vinculo)).andExpect(status().isNoContent());

        List<OutboxEvento> eventos = eventosDoVinculo();
        assertThat(eventos).extracting(OutboxEvento::getTipo).containsExactly(
                "paciente.convenio-vinculado", "paciente.convenio-atualizado", "paciente.convenio-desvinculado");
        assertThat(eventos).extracting(OutboxEvento::getAgregadoId).containsOnly(paciente);

        JsonNode atualizado = eventos.get(1).getPayload();
        assertThat(atualizado.path("camposAlterados")).extracting(JsonNode::asText).containsExactly("numeroCarteirinha");
        assertThat(atualizado.path("dados").path("convenio").path("nome").asText()).isEqualTo("Unimed");
        assertThat(eventos.get(2).getPayload().path("dados").path("ativo").asBoolean()).isFalse();
    }

    @Test
    void rn16_convenioInativado_vinculoContinuaMasDeixaDeSerVigente() throws Exception {
        long vinculo = vincularOk(unimed);
        vincularOk(amil);
        vigentes(null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));

        inativarConvenio(unimed);

        mockMvc.perform(get("/pacientes/{id}/convenios/{v}", paciente, vinculo))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.vigente").value(false));
        vigentes(null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].convenio.id").value(amil));
    }

    @Test
    void rf24_vigentesNaData_consideraValidadeDaCarteirinha() throws Exception {
        LocalDate validade = LocalDate.now().plusDays(10);
        vincular(unimed, "0001", validade).andExpect(status().isCreated());

        vigentes(validade.toString()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroCarteirinha").value("0001"));
        vigentes(validade.plusDays(1).toString()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rf24_pacienteInexistente_retorna404_eSoServicoOuAdminAcessa() throws Exception {
        mockMvc.perform(get("/integracao/pacientes/{id}/convenios/vigentes", 999999)
                .with(user("agenda").roles("SERVICO"))).andExpect(status().isNotFound());
        mockMvc.perform(get("/integracao/pacientes/{id}/convenios/vigentes", paciente)
                .with(user("recepcao").roles("FUNCIONARIO"))).andExpect(status().isForbidden());
    }

    @Test
    void medicoConsulta_masNaoVincula() throws Exception {
        mockMvc.perform(get("/pacientes/{id}/convenios", paciente).with(user("dr").roles("MEDICO")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/pacientes/{id}/convenios", paciente).with(user("dr").roles("MEDICO"))
                        .contentType(MediaType.APPLICATION_JSON).content(vinculoJson(unimed, "1", null)))
                .andExpect(status().isForbidden());
    }
}
