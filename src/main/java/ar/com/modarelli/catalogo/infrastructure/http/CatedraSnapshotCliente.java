package ar.com.modarelli.catalogo.infrastructure.http;

import ar.com.modarelli.catalogo.domain.model.SnapshotCatalogo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class CatedraSnapshotCliente
        implements ar.com.modarelli.catalogo.domain.ports.out.DescargarSnapshotPort {
    private final String baseUrl;
    private final String token;
    private final RestClient cliente;

    @Autowired
    public CatedraSnapshotCliente(
            @Value("${catalogo.catedra.base-url:}") String url,
            @Value("${catalogo.catedra.token:}") String token) {
        this(url, token, crearBuilder());
    }

    CatedraSnapshotCliente(String url, String token, RestClient.Builder builder) {
        baseUrl = url;
        this.token = token;
        cliente =
                builder.messageConverters(
                                converters -> {
                                    converters.removeIf(
                                            c ->
                                                    c
                                                            instanceof
                                                            org.springframework.http.converter.json
                                                                    .JacksonJsonHttpMessageConverter);
                                    converters.add(
                                            new org.springframework.http.converter.json
                                                    .JacksonJsonHttpMessageConverter(
                                                    tools.jackson.databind.json.JsonMapper.builder()
                                                            .disable(
                                                                    tools.jackson.databind
                                                                            .DeserializationFeature
                                                                            .FAIL_ON_UNKNOWN_PROPERTIES)
                                                            .build()));
                                })
                        .build();
    }

    private static RestClient.Builder crearBuilder() {
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(factory);
    }

    public SnapshotCatalogo descargar() {
        if (baseUrl.isBlank() || token.isBlank())
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar la integracion con catedra");
        try {
            var uri = URI.create(baseUrl.replaceAll("/+$", "") + "/api/synchronization/snapshot");
            SnapshotCatalogo s =
                    cliente.get()
                            .uri(uri)
                            .headers(h -> h.setBearerAuth(token))
                            .retrieve()
                            .body(SnapshotCatalogo.class);
            if (s == null)
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Catedra devolvio un snapshot vacio");
            return s;
        } catch (RestClientException | IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "No se pudo obtener el snapshot de catedra");
        }
    }
}
