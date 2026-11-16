package com.changeflow.integration.support;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.*;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.support.TestPropertySourceUtils;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** One generated file database per context, never a shared EntityManager or fixed credential. */
public class TestDatabaseConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override public void initialize(ConfigurableApplicationContext context) {
        try {
            Path root = Path.of("target", "test-databases").toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path directory = Files.createTempDirectory(root, "context-");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "spring.datasource.url=jdbc:h2:file:" + directory.resolve("changeflow").toString().replace('\\', '/'),
                    "spring.datasource.username=u" + UUID.randomUUID(),
                    "spring.datasource.password=" + UUID.randomUUID(),
                    "logging.level.root=ERROR", "spring.main.banner-mode=off");
            context.addApplicationListener(event -> {
                if (event instanceof ContextClosedEvent closed && closed.getApplicationContext() == context) {
                    for (var pool : context.getBeansOfType(HikariDataSource.class).values()) pool.close();
                    try (var paths = Files.walk(directory)) {
                        for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                            if (!path.toAbsolutePath().normalize().startsWith(root)) throw new IllegalStateException("Unexpected test database path.");
                            Files.delete(path);
                        }
                    } catch (IOException error) { throw new IllegalStateException("Could not clean the isolated test database.", error); }
                }
            });
        } catch (IOException error) { throw new IllegalStateException("Could not create the isolated test database.", error); }
    }
}
