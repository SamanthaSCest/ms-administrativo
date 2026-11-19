package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import br.edu.imepac.clinicalapi.administrativo.support.Acessos;
import br.edu.imepac.clinicalapi.administrativo.support.ComGrupo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Listagem e busca de médicos por nome, CRM, especialidade e situação (RNF08, RN20). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ComGrupo(username = "admin", value = "ADMINISTRADOR")
class MedicoBuscaApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MedicoRepository medicoRepository;

    private long cardio;
    private long pediatria;
    private long carlos;
    private long carla;
    private long paulo;

    // Os usuários criados têm o CPF como login: limpar evita conflito com outros testes
    @AfterEach
    void limpar() {
        medicoRepository.deleteAll();
    }

    @BeforeEach
    void preparar() throws Exception {
        medicoRepository.deleteAll();
        // Nomes únicos: especialidades de outros testes podem continuar no banco
        long sufixo = System.nanoTime();
        cardio = id(mockMvc.perform(post("/especialidades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Cardio " + sufixo + "\"}")).andExpect(status().isCreated()).andReturn());
        pediatria = id(mockMvc.perform(post("/especialidades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Pediatria " + sufixo + "\"}")).andExpect(status().isCreated()).andReturn());

        carlos = criar("Carlos Mendes", "33345678934", "1001", "MG", cardio, pediatria);
        carla = criar("Carla Nunes", "44445678996", "1002", "SP", cardio);
        paulo = criar("Paulo Rocha", "55545678948", "1001", "SP", pediatria);
        mockMvc.perform(patch("/medicos/{id}/status", carla).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
    }

    private long criar(String nome, String cpf, String crm, String uf, long... especialidades) throws Exception {
        String ids = java.util.Arrays.stream(especialidades).mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return id(mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","cpf":"%s","dataNascimento":"1980-01-01","crm":"%s","ufCrm":"%s",
                                 "especialidadeIds":[%s],"senha":"senhaForte1"}
                                """.formatted(nome, cpf, crm, uf, ids)))
                .andExpect(status().isCreated()).andReturn());
    }

    private long id(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions buscar(String filtro) throws Exception {
        return mockMvc.perform(post("/medicos/busca").contentType(MediaType.APPLICATION_JSON).content(filtro));
    }

    @Test
    void listar_semSituacao_trazSoAtivos_comCpfMascarado() throws Exception {
        mockMvc.perform(get("/medicos").param("sort", "nome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(carlos))
                .andExpect(jsonPath("$.content[0].cpf").value("***.456.789-**"))
                .andExpect(jsonPath("$.content[1].id").value(paulo));
    }

    @Test
    void listar_inativos() throws Exception {
        mockMvc.perform(get("/medicos").param("ativo", "false"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(carla));
    }

    @Test
    void getPorId_continuaComCpfCompleto() throws Exception {
        mockMvc.perform(get("/medicos/{id}", carlos)).andExpect(jsonPath("$.cpf").value("33345678934"));
    }

    @Test
    void busca_porParteDoNome_semDiferenciarMaiusculas() throws Exception {
        buscar("{\"nome\":\"CARL\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(carlos));

        buscar("{\"nome\":\"carl\",\"ativo\":false}")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(carla));
    }

    @Test
    void busca_porCrm_comESemUf() throws Exception {
        buscar("{\"crm\":\"1001\"}").andExpect(jsonPath("$.content", hasSize(2)));

        buscar("{\"crm\":\"1001\",\"ufCrm\":\"SP\"}")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(paulo));
    }

    @Test
    void busca_porEspecialidade_semRepetirMedico() throws Exception {
        // Carlos atende as duas: o join não pode duplicá-lo
        buscar("{\"especialidadeId\":%d}".formatted(pediatria))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        buscar("{\"especialidadeId\":%d,\"nome\":\"paulo\"}".formatted(pediatria))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(paulo));
    }

    @Test
    void busca_semResultado_devolvePaginaVazia() throws Exception {
        buscar("{\"crm\":\"9999\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void busca_ufInvalida_retorna400() throws Exception {
        buscar("{\"ufCrm\":\"XX\"}").andExpect(status().isBadRequest());
    }

    @Test
    void busca_funcionarioPodeConsultar() throws Exception {
        // A busca é leitura: quem consulta médicos também busca, mesmo sendo POST
        mockMvc.perform(post("/medicos/busca").with(Acessos.grupo("recepcao", "RECEPCIONISTA"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"paulo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}
