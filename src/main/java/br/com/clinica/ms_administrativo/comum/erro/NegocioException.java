package br.com.clinica.ms_administrativo.comum.erro;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final String slug;
    private final String titulo;

    public NegocioException(HttpStatus status, String codigo, String slug,
                            String titulo, String detalhe) {
        super(detalhe);
        this.status = status;
        this.codigo = codigo;
        this.slug = slug;
        this.titulo = titulo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
    public String getSlug() { return slug; }
    public String getTitulo() { return titulo; }
}