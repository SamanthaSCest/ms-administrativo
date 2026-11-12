package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.entities.EntidadeBase;
import br.edu.imepac.clinicalapi.administrativo.mapper.ResumoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Publica os eventos de domínio dos cadastros na outbox (RF23, RN19), sempre na mesma transação da alteração.
 * Tipos: &lt;agregado&gt;.criado|atualizado|inativado|reativado|removido (concordando em gênero:
 * especialidade.criada, sala.inativada...). O campo "dados" é o Resumo da entidade (ver ResumoMapper).
 */
@Service
public class EventoService {

    /** Campos calculados ou de auditoria: não contam como "alterados". */
    private static final Set<String> IGNORADOS = Set.of("id", "uuid", "dataCriacao", "dataModificacao", "criadoPor",
            "atualizadoPor", "idade", "menorDeIdade", "vigente", "custoInsumos", "usuario");

    private final OutboxService outboxService;
    private final ResumoMapper resumoMapper;
    private final ObjectMapper objectMapper;

    public EventoService(OutboxService outboxService, ResumoMapper resumoMapper, ObjectMapper objectMapper) {
        this.outboxService = outboxService;
        this.resumoMapper = resumoMapper;
        this.objectMapper = objectMapper;
    }

    public void criado(EntidadeBase entidade) {
        registrar(entidade, "criado", List.of());
    }

    /**
     * @param antes  representação completa (DTO de resposta) antes da alteração
     * @param depois representação completa depois da alteração
     *               Os nomes dos campos que mudaram vão em camposAlterados. Sem mudança, não publica nada.
     */
    public void atualizado(EntidadeBase entidade, Object antes, Object depois) {
        List<String> alterados = camposAlterados(antes, depois);
        if (!alterados.isEmpty()) {
            registrar(entidade, "atualizado", alterados);
        }
    }

    public void statusAlterado(EntidadeBase entidade) {
        registrar(entidade, entidade.isAtivo() ? "reativado" : "inativado", List.of("ativo"));
    }

    /** Chamar ANTES de apagar (o resumo é montado a partir da entidade ainda carregada). */
    public void removido(EntidadeBase entidade) {
        registrar(entidade, "removido", List.of());
    }

    /** Captura o estado atual como JSON (use antes de alterar a entidade). */
    public JsonNode retrato(Object representacao) {
        return objectMapper.valueToTree(representacao);
    }

    List<String> camposAlterados(Object antes, Object depois) {
        JsonNode a = antes instanceof JsonNode n ? n : objectMapper.valueToTree(antes);
        JsonNode d = depois instanceof JsonNode n ? n : objectMapper.valueToTree(depois);
        Set<String> nomes = new TreeSet<>();
        a.fieldNames().forEachRemaining(nomes::add);
        d.fieldNames().forEachRemaining(nomes::add);
        List<String> alterados = new ArrayList<>();
        for (String nome : nomes) {
            if (!IGNORADOS.contains(nome) && !iguais(a.path(nome), d.path(nome))) {
                alterados.add(nome);
            }
        }
        return alterados;
    }

    /** Números comparados pelo valor (1.0 == 1.0000, escala do banco x do request); o resto, por igualdade. */
    private static boolean iguais(JsonNode a, JsonNode b) {
        if (a.isNumber() && b.isNumber()) {
            return a.decimalValue().compareTo(b.decimalValue()) == 0;
        }
        return a.equals(b);
    }

    private void registrar(EntidadeBase entidade, String acao, List<String> camposAlterados) {
        ResumoMapper.Agregado agregado = resumoMapper.agregado(entidade);
        String tipo = agregado.nome() + "." + (agregado.feminino() ? feminino(acao) : acao);
        outboxService.registrar(tipo, entidade.getId(), resumoMapper.resumo(entidade), camposAlterados);
    }

    /** criado -> criada, atualizado -> atualizada... (todas as ações terminam em "o"). */
    private static String feminino(String acao) {
        return acao.substring(0, acao.length() - 1) + "a";
    }
}
