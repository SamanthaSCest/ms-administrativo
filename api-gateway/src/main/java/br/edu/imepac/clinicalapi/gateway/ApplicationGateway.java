package br.edu.imepac.clinicalapi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: porta de entrada única. Não tem regra de negócio; apenas roteia
 * as requisições (rotas definidas em application.yml).
 */
@SpringBootApplication
public class ApplicationGateway {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationGateway.class, args);
    }
}
