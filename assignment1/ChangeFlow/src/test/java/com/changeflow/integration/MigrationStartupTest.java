package com.changeflow.integration;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/** T008: real file databases; Flyway is the only schema creator. */
class MigrationStartupTest {
    @TempDir Path directory;
    private final String username = UUID.randomUUID().toString();
    private final String password = UUID.randomUUID().toString();

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
    @EntityScan("com.changeflow.infrastructure.persistence")
    @EnableJpaRepositories("com.changeflow.infrastructure.persistence")
    static class Bootstrap { }

    private ConfigurableApplicationContext start(String... additional) {
        var args = new java.util.ArrayList<>(java.util.List.of(
                "--spring.datasource.url=" + url(), "--spring.datasource.username=" + username,
                "--spring.datasource.password=" + password, "--spring.main.banner-mode=off",
                "--logging.level.root=ERROR"));
        args.addAll(java.util.List.of(additional));
        return new SpringApplicationBuilder(Bootstrap.class).web(WebApplicationType.NONE)
                .run(args.toArray(String[]::new));
    }

    private String url() { return "jdbc:h2:file:" + directory.resolve("database").toAbsolutePath(); }

    @Test void freshFileMigratesBeforeValidatingAllFourMappings() {
        try (var context = start()) {
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(context.getBean(EntityManagerFactory.class).getMetamodel().getEntities()).hasSize(4);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = TRUE AND \"version\" = '1'", Integer.class)).isEqualTo(1);
            for (String table : java.util.List.of("USER_ACCESS", "CREDENTIAL", "CHANGE_REQUEST", "REVIEW_DECISION")) {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isZero();
            }
            assertThat(jdbc.queryForList("SELECT DATETIME_PRECISION FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CHANGE_REQUEST' AND COLUMN_NAME IN ('CREATED_AT','UPDATED_AT')", Integer.class))
                    .containsExactly(9, 9);
            assertThat(jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='REVIEW_DECISION'", String.class))
                    .doesNotContain("EXPLANATION", "LATEST_EXPLANATION");
            var owner = UUID.randomUUID();
            jdbc.update("INSERT INTO user_access VALUES (?, TRUE, TRUE, ?)", owner, Instant.now());
            byte[] digest = new byte[32]; new java.security.SecureRandom().nextBytes(digest);
            jdbc.update("INSERT INTO credential VALUES (?, ?, ?, ?, NULL)", UUID.randomUUID(), owner, digest, Instant.now());
            assertThatThrownBy(() -> jdbc.update("INSERT INTO credential VALUES (?, ?, ?, ?, NULL)", UUID.randomUUID(), owner, digest, Instant.now()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("INSERT INTO credential VALUES (?, ?, ?, ?, NULL)", UUID.randomUUID(), UUID.randomUUID(), new byte[32], Instant.now()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            UUID request = UUID.randomUUID(), reviewer = UUID.randomUUID();
            jdbc.update("INSERT INTO user_access VALUES (?, FALSE, TRUE, ?)", reviewer, Instant.now());
            String insert = "INSERT INTO change_request VALUES (?, 'Title', 'Description', 'Reason', ?, ?, 'Feedback', ?, ?, 0)";
            assertThatThrownBy(() -> jdbc.update(insert, request, owner, "Unknown", Instant.now(), Instant.now()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            jdbc.update(insert, request, owner, "ChangesRequested", Instant.now(), Instant.now());
            jdbc.update("INSERT INTO review_decision VALUES (?, ?, ?, 'RequestChanges', ?, 0)", UUID.randomUUID(), request, reviewer, Instant.now());
            assertThatThrownBy(() -> jdbc.update("INSERT INTO review_decision VALUES (?, ?, ?, 'Approve', ?, 0)", UUID.randomUUID(), request, reviewer, Instant.now()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("INSERT INTO review_decision VALUES (?, ?, ?, 'Unknown', ?, 1)", UUID.randomUUID(), request, reviewer, Instant.now()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("INSERT INTO review_decision VALUES (?, ?, ?, 'Approve', ?, 1)", UUID.randomUUID(), UUID.randomUUID(), reviewer, Instant.now()))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
    }

    @Test void repeatStartupDoesNotResetStoredRows() {
        UUID id = UUID.randomUUID();
        try (var first = start()) {
            first.getBean(JdbcTemplate.class).update("INSERT INTO user_access VALUES (?, FALSE, FALSE, ?)", id, Instant.now());
        }
        try (var second = start()) {
            assertThat(second.getBean(JdbcTemplate.class).queryForObject("SELECT user_id FROM user_access", UUID.class)).isEqualTo(id);
        }
    }

    @Test void brokenMigrationFailsStartup() throws Exception {
        Path migrations = Files.createDirectory(directory.resolve("broken"));
        Files.writeString(migrations.resolve("V1__broken.sql"), "THIS IS NOT VALID SQL;");
        assertThatThrownBy(() -> start("--spring.flyway.locations=filesystem:" + migrations.toAbsolutePath()))
                .hasStackTraceContaining("FlywayMigrateException");
    }

    @Test void schemaMismatchFailsValidationRatherThanRepairingIt() throws Exception {
        try (var ignored = start()) { /* migrate and close pool before direct inspection */ }
        try (var connection = DriverManager.getConnection(url(), username, password);
             var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE change_request DROP COLUMN title");
        }
        assertThatThrownBy(this::start).hasStackTraceContaining("Schema-validation");
    }
}
