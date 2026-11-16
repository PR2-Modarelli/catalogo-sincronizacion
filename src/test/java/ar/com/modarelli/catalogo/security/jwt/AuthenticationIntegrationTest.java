package ar.com.modarelli.catalogo.security.jwt;

import ar.com.modarelli.catalogo.infrastructure.config.SecurityConfiguration;
import ar.com.modarelli.catalogo.infrastructure.config.SecurityJwtConfiguration;
import ar.com.modarelli.catalogo.infrastructure.config.WebConfigurer;
import ar.com.modarelli.catalogo.infrastructure.management.SecurityMetersService;

import org.springframework.boot.test.context.SpringBootTest;

import tech.jhipster.config.JHipsterProperties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        classes = {
            JHipsterProperties.class,
            WebConfigurer.class,
            SecurityConfiguration.class,
            SecurityJwtConfiguration.class,
            SecurityMetersService.class,
            JwtAuthenticationTestUtils.class,
        })
public @interface AuthenticationIntegrationTest {}
