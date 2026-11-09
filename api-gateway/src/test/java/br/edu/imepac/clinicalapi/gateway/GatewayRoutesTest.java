package br.edu.imepac.clinicalapi.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void deveExporRotasDosMicroservicos() {
        List<Route> rotas = routeLocator.getRoutes().collectList().block();

        assertThat(rotas).extracting(Route::getId).containsExactlyInAnyOrder("ms-administrativo", "ms-agendamento");
        assertThat(rotas).filteredOn(r -> r.getId().equals("ms-administrativo"))
                .singleElement().satisfies(r -> assertThat(r.getUri().toString()).isEqualTo("http://localhost:8081"));
        assertThat(rotas).filteredOn(r -> r.getId().equals("ms-agendamento"))
                .singleElement().satisfies(r -> assertThat(r.getUri().toString()).isEqualTo("http://localhost:8082"));
    }
}
