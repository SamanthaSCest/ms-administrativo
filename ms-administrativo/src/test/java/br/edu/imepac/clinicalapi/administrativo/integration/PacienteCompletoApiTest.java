package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ResponsavelLegalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Paciente com sexo, telefones, endereço e responsável legal (menores de idade). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "recepcao", roles = "FUNCIONARIO")
class PacienteCompletoApiTest {

    // CPF válido (dígitos verificadores corretos), exigido pelo @Cpf do responsável
    private static final String CPF_RESPONSAVEL = "52998224725";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private ResponsavelLegalRepository responsavelRepository;

    @BeforeEach
    void limpar() {
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        responsavelRepository.deleteAll();
    }

    private long criarResponsavel() throws Exception {
        MvcResult result = mockMvc.perform(post("/responsaveis").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Souza","cpf":"%s","telefone":"34999990000","parentesco":"MÃE"}
                                """.formatted(CPF_RESPONSAVEL)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String pacienteMenorJson(String responsavel) {
        String nascimento = LocalDate.now().minusYears(8).toString();
        return """
                {"nome":"Pedro Souza","cpf":"12345678909","dataNascimento":"%s","sexo":"MASCULINO",
                 "telefone":"34988887777","telefoneFixo":"3432410000",
                 "endereco":{"cep":"38440000","logradouro":"Rua A","numero":"10","bairro":"Centro",
                             "cidade":"Araguari","uf":"MG"},
                 "responsavelId":%s}
                """.formatted(nascimento, responsavel);
    }

    @Test
    void pacienteMenor_comResponsavel_criaComEnderecoEIdade() throws Exception {
        long responsavelId = criarResponsavel();

        mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content(pacienteMenorJson(String.valueOf(responsavelId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sexo").value("MASCULINO"))
                .andExpect(jsonPath("$.telefoneFixo").value("3432410000"))
                .andExpect(jsonPath("$.idade").value(8))
                .andExpect(jsonPath("$.menorDeIdade").value(true))
                .andExpect(jsonPath("$.endereco.uf").value("MG"))
                .andExpect(jsonPath("$.endereco.formatado").value("Rua A, 10 - Centro - Araguari/MG - CEP 38440-000"))
                .andExpect(jsonPath("$.responsavel.parentesco").value("MÃE"));

        // Responsável com paciente vinculado não pode ser removido
        mockMvc.perform(delete("/responsaveis/{id}", responsavelId)).andExpect(status().isConflict());
    }

    @Test
    void pacienteMenor_semResponsavel_retorna422() throws Exception {
        mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content(pacienteMenorJson("null")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void enderecoIncompleto_retorna400() throws Exception {
        mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana","cpf":"12345678909","dataNascimento":"1990-01-01",
                                 "endereco":{"cep":"123","uf":"MG"}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pacienteAdulto_semEnderecoNemResponsavel_continuaFuncionando() throws Exception {
        mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"cpf\":\"12345678909\",\"dataNascimento\":\"1990-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.menorDeIdade").value(false))
                .andExpect(jsonPath("$.endereco").doesNotExist());
    }

    @Test
    void responsavel_cpfInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/responsaveis").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"cpf\":\"11111111111\",\"parentesco\":\"PAI\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/responsaveis")).andExpect(status().isOk());
    }
}
