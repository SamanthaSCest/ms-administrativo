package br.edu.imepac.clinicalapi.administrativo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Pattern;

@Embeddable
public class Endereco {

    @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos")
    @Column(length = 8)
    private String cep;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 10)
    private String numero;

    @Column(length = 60)
    private String complemento;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    @Enumerated(EnumType.STRING)
    private UF uf;

    protected Endereco() {
    }

    public Endereco(
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            UF uf
    ) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public UF getUf() {
        return uf;
    }

    public void setUf(UF uf) {
        this.uf = uf;
    }

    /** Ex.: "Rua A, 10 - Apto 2 - Centro - Araguari/MG - CEP 38440-000" (partes vazias são omitidas). */
    public String formatado() {
        StringBuilder sb = new StringBuilder();
        if (logradouro != null) {
            sb.append(logradouro);
            if (numero != null) {
                sb.append(", ").append(numero);
            }
        }
        anexar(sb, complemento);
        anexar(sb, bairro);
        if (cidade != null) {
            anexar(sb, uf == null ? cidade : cidade + "/" + uf.name());
        }
        if (cep != null && cep.length() == 8) {
            anexar(sb, "CEP " + cep.substring(0, 5) + "-" + cep.substring(5));
        }
        return sb.toString();
    }

    private static void anexar(StringBuilder sb, String parte) {
        if (parte == null || parte.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(" - ");
        }
        sb.append(parte);
    }
}
