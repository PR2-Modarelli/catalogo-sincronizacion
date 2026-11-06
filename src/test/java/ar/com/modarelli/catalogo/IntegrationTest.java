package ar.com.modarelli.catalogo;

import ar.com.modarelli.catalogo.config.AsyncSyncConfiguration;
import ar.com.modarelli.catalogo.config.DatabaseTestcontainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    properties = {"catalogo.sync.enabled=false"},
    classes = {
        CatalogoApp.class,
        AsyncSyncConfiguration.class,
        ar.com.modarelli.catalogo.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
    }
)
public @interface IntegrationTest {}
