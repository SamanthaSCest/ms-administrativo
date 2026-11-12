package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.ConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.SalaRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API de integração (somente leitura) chamada por outro microserviço com X-Api-Key. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class IntegracaoApiTest {

    private static final String API_KEY = "dev-integracao-key";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private ConvenioRepository convenioRepository;

    private long cardio;
    private long pediatria;

    @BeforeEach
    void preparar() throws Exception {
        limpar();
        cardio = criar("/especialidades", "{\"nome\":\"Cardiologia\"}");
        pediatria = criar("/especialidades", "{\"nome\":\"Pediatria\"}");
    }

    @AfterEach
    void limpar() {
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        especialidadeRepository.deleteAll();
        salaRepository.deleteAll();
        convenioRepository.deleteAll();
    }

    private long criar(String url, String body) throws Exception {
        MvcResult r = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long medico(String nome, String cpf, String crm, long... especialidades) throws Exception {
        String ids = LongStream.of(especialidades).mapToObj(String::valueOf).collect(Collectors.joining(","));
        return criar("/medicos", """
                {"nome":"%s","cpf":"%s","dataNascimento":"1980-01-01","crm":"%s","ufCrm":"MG",
                 "especialidadeIds":[%s],"senha":"senhaForte1"}
                """.formatted(nome, cpf, crm, ids));
    }

    private void inativar(String recurso, long id) throws Exception {
        mockMvc.perform(patch("/{r}/{id}/status", recurso, id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
    }

    /** Requisição como o outro microserviço faria: só com a X-Api-Key, sem usuário. */
    private MockHttpServletRequestBuilder servico(String url, Object... vars) {
        return get(url, vars).header("X-Api-Key", API_KEY).with(anonymous());
    }

    @Test
    void especialidades_soAtivas() throws Exception {
        inativar("especialidades", pediatria);

        mockMvc.perform(servico("/integracao/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Cardiologia"));
    }

    @Test
    void medico_resumoSituacaoEAtende() throws Exception {
        long m = medico("Dr. Ana", "12345678901", "1001", cardio);

        mockMvc.perform(servico("/integracao/medicos/{id}", m))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crm").value("1001"))
                .andExpect(jsonPath("$.especialidades[0].nome").value("Cardiologia"))
                .andExpect(jsonPath("$.cpf").doesNotExist());
        mockMvc.perform(servico("/integracao/medicos/{id}", 999999)).andExpect(status().isNotFound());

        mockMvc.perform(servico("/integracao/medicos/{id}/situacao", m)).andExpect(jsonPath("$.ativo").value(true));
        mockMvc.perform(servico("/integracao/medicos/{id}/situacao", 999999))
                .andExpect(status().isOk()).andExpect(jsonPath("$.existe").value(false));

        mockMvc.perform(servico("/integracao/medicos/{id}/atende", m).param("especialidadeId", String.valueOf(cardio)))
                .andExpect(jsonPath("$.atende").value(true));
        mockMvc.perform(servico("/integracao/medicos/{id}/atende", m).param("especialidadeId", String.valueOf(pediatria)))
                .andExpect(jsonPath("$.atende").value(false));

        // Médico inativo não atende
        inativar("medicos", m);
        mockMvc.perform(servico("/integracao/medicos/{id}/atende", m).param("especialidadeId", String.valueOf(cardio)))
                .andExpect(jsonPath("$.atende").value(false));
    }

    @Test
    void medicos_emLoteEPorEspecialidade() throws Exception {
        long ana = medico("Dr. Ana", "12345678901", "1001", cardio);
        long bia = medico("Dra. Bia", "10987654321", "1002", cardio, pediatria);
        long caio = medico("Dr. Caio", "11122233344", "1003", pediatria);
        inativar("medicos", bia);

        // Por ids: qualquer status, ids inexistentes ignorados
        mockMvc.perform(servico("/integracao/medicos").param("ids", ana + "," + bia + ",999999"))
                .andExpect(jsonPath("$", hasSize(2)));
        // Por especialidade: só ativos
        mockMvc.perform(servico("/integracao/medicos").param("especialidadeId", String.valueOf(pediatria)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(caio));
        // Sem filtro -> 422; mais de 100 ids -> 422
        mockMvc.perform(servico("/integracao/medicos")).andExpect(status().isUnprocessableEntity());
        String muitos = LongStream.rangeClosed(1, 101).mapToObj(String::valueOf).collect(Collectors.joining(","));
        mockMvc.perform(servico("/integracao/medicos").param("ids", muitos)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void pacientes_emLote_comCpfMascarado() throws Exception {
        long p1 = criar("/pacientes", "{\"nome\":\"Ana\",\"cpf\":\"12345678909\",\"dataNascimento\":\"1990-01-01\"}");
        long p2 = criar("/pacientes", "{\"nome\":\"Bia\",\"cpf\":\"98765432100\",\"dataNascimento\":\"1991-01-01\"}");

        mockMvc.perform(servico("/integracao/pacientes").param("ids", p2 + "," + p1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(p1))
                .andExpect(jsonPath("$[0].cpfMascarado").value("***.456.789-**"))
                .andExpect(jsonPath("$[0].cpf").doesNotExist());
    }

    @Test
    void salasAtivasPorTipo_eConvenios() throws Exception {
        long c1 = criar("/salas", "{\"numero\":\"101\",\"tipo\":\"CONSULTORIO\",\"andar\":\"1º\"}");
        criar("/salas", "{\"numero\":\"201\",\"tipo\":\"CIRURGIA\",\"observacoes\":\"Foco duplo\"}");
        long c2 = criar("/salas", "{\"numero\":\"102\",\"tipo\":\"CONSULTORIO\"}");
        inativar("salas", c2);

        mockMvc.perform(servico("/integracao/salas").param("tipo", "CONSULTORIO"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero").value("101"));
        mockMvc.perform(servico("/integracao/salas")).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(servico("/integracao/salas/{id}", c1)).andExpect(jsonPath("$.andar").value("1º"));

        long conv = criar("/convenios", "{\"nome\":\"Unimed\",\"registroAns\":\"123456\",\"cnpj\":\"11222333000181\"}");
        mockMvc.perform(servico("/integracao/convenios")).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(servico("/integracao/convenios/{id}", conv)).andExpect(jsonPath("$.registroAns").value("123456"));
    }

    @Test
    void integracao_exigeChaveOuAdmin_eNaoAceitaEscrita() throws Exception {
        // Sem chave e sem usuário: 401; perfil FUNCIONARIO: 403
        mockMvc.perform(get("/integracao/especialidades").with(anonymous())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/integracao/especialidades").with(user("recepcao").roles("FUNCIONARIO")))
                .andExpect(status().isForbidden());
        // Somente leitura (RN18): não existe rota de escrita
        mockMvc.perform(post("/integracao/especialidades").header("X-Api-Key", API_KEY).with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
