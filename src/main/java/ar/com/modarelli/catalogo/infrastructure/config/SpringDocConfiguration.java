package ar.com.modarelli.catalogo.infrastructure.config;

import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfiguration {

    @Bean
    public ServerBaseUrlCustomizer serverBaseUrlRequestCustomizer() {
        return (serverBaseUrl, request) -> {
            List<String> forwardedPrefix = request.getHeaders().get("X-Forwarded-Prefix");
            if (forwardedPrefix != null && !forwardedPrefix.isEmpty()) {
                return forwardedPrefix.getFirst();
            }
            return serverBaseUrl;
        };
    }
}
