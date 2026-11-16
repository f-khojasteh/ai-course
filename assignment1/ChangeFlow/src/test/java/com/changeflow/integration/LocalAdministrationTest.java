package com.changeflow.integration;

import com.changeflow.application.*;
import com.changeflow.application.port.*;
import com.changeflow.domain.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import com.changeflow.integration.support.TestDatabaseConfiguration;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {"logging.level.root=ERROR", "spring.main.banner-mode=off"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = TestDatabaseConfiguration.class)
class LocalAdministrationTest {
    @TempDir static Path directory;
    @Autowired LocalAdministrationService administration;
    @Autowired JdbcTemplate jdbc;
    @Autowired CredentialCodec codec;
    @Autowired CredentialStore credentials;
    @Autowired CurrentAccessGuard guard;
    @Autowired PlatformTransactionManager transactions;
    @Autowired org.springframework.core.env.Environment environment;
    @Autowired com.zaxxer.hikari.HikariDataSource pool;
    @Autowired ChangeRequestStore requests;
    @Autowired ReviewDecisionLog decisions;
    @Autowired UserAccessStore access;

    @Test void provisionsBothRolesAndPersistsOnlyDigestBeforeReturning() {
        var issued = administration.provision(Set.of(UserRole.Requester, UserRole.Reviewer));
        assertThat(jdbc.queryForObject("SELECT requester AND reviewer FROM user_access WHERE user_id=?", Boolean.class, issued.userId())).isTrue();
        byte[] stored = jdbc.queryForObject("SELECT secret_hash FROM credential WHERE credential_id=?", byte[].class, issued.credentialId());
        assertThat(codec.verifies(issued.value(), stored)).isTrue();
        assertThat(stored.length).isEqualTo(32);
        assertThat(issued.toString().contains(issued.value())).isFalse();
        assertThat(jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='CREDENTIAL'", String.class))
                .containsExactlyInAnyOrder("CREDENTIAL_ID", "USER_ID", "SECRET_HASH", "CREATED_AT", "REVOKED_AT");
        var extra = administration.issueCredential(issued.userId());
        assertThat(extra.userId()).isEqualTo(issued.userId());
        assertThat(extra.credentialId()).isNotEqualTo(issued.credentialId());
    }

    @Test void revocationIsIrreversibleAndCurrentAccessRechecksIt() {
        var issued = administration.provision(Set.of(UserRole.Requester));
        var caller = new AuthenticatedCaller(issued.credentialId(), issued.userId());
        var tx = new TransactionTemplate(transactions);
        Boolean permitted = tx.execute(status -> guard.lockAndVerify(caller).requester());
        assertThat(permitted).isTrue();
        administration.revokeCredential(issued.credentialId());
        Instant first = jdbc.queryForObject("SELECT revoked_at FROM credential WHERE credential_id=?", java.time.OffsetDateTime.class, issued.credentialId()).toInstant();
        administration.revokeCredential(issued.credentialId());
        assertThat(jdbc.queryForObject("SELECT revoked_at FROM credential WHERE credential_id=?", java.time.OffsetDateTime.class, issued.credentialId()).toInstant()).isEqualTo(first);
        assertThatThrownBy(() -> tx.execute(status -> guard.lockAndVerify(caller))).isInstanceOf(ApplicationFailure.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credential WHERE credential_id=?", Integer.class, issued.credentialId())).isEqualTo(1);
    }

    @Test void removingOneRolePreservesIdentityAndOtherRole() {
        var issued = administration.provision(Set.of(UserRole.Requester, UserRole.Reviewer));
        administration.removeRole(issued.userId(), UserRole.Requester);
        var current = new TransactionTemplate(transactions).execute(s -> guard.lockAndVerify(new AuthenticatedCaller(issued.credentialId(), issued.userId())));
        assertThat(current.requester()).isFalse(); assertThat(current.reviewer()).isTrue();
        administration.removeRole(issued.userId(), UserRole.Reviewer);
        assertThat(jdbc.queryForObject("SELECT requester OR reviewer FROM user_access WHERE user_id=?", Boolean.class, issued.userId())).isFalse();
    }

    @Test void failureAfterProvisioningRollsBackBothRows() {
        int users = jdbc.queryForObject("SELECT COUNT(*) FROM user_access", Integer.class);
        int count = jdbc.queryForObject("SELECT COUNT(*) FROM credential", Integer.class);
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> {
            administration.provision(Set.of(UserRole.Requester));
            throw new IllegalStateException("Controlled failure before commit.");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_access", Integer.class)).isEqualTo(users);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credential", Integer.class)).isEqualTo(count);
        assertThatThrownBy(() -> administration.issueCredential(UUID.randomUUID())).isInstanceOf(ApplicationFailure.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credential", Integer.class)).isEqualTo(count);
    }

    @Test void actualTimeoutsAndPublicProxyTransactionPolicy() throws Exception {
        assertThat(org.springframework.aop.support.AopUtils.isAopProxy(administration)).isTrue();
        var attributes = new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()
                .getTransactionAttribute(LocalAdministrationService.class.getMethod("provision", Set.class), LocalAdministrationService.class);
        assertThat(attributes.getIsolationLevel()).isEqualTo(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(attributes.getTimeout()).isEqualTo(10);
        assertThat(attributes.rollbackOn(new Exception("Controlled checked failure."))).isTrue();
        assertThat(jdbc.queryForObject("CALL LOCK_TIMEOUT()", Integer.class)).isEqualTo(2000);
        assertThat(pool.getConnectionTimeout()).isEqualTo(3000);
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.jpa.defer-datasource-initialization", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @Test void actualAdminEntryPointHasNoHttpPortDisplaysOnceAndClosesFile() throws Exception {
        String url = "jdbc:h2:file:" + directory.resolve("command").toAbsolutePath();
        String username = "u" + UUID.randomUUID(), password = UUID.randomUUID().toString();
        try (var occupied = new java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
            var output = new java.io.ByteArrayOutputStream();
            var error = new java.io.ByteArrayOutputStream();
            var common = List.of("--changeflow.mode=admin", "--spring.datasource.url=" + url,
                    "--spring.datasource.username=" + username, "--spring.datasource.password=" + password,
                    "--server.port=" + occupied.getLocalPort(), "--spring.main.web-application-type=servlet",
                    "--spring.main.banner-mode=off", "--logging.level.root=ERROR");
            var args = new ArrayList<>(common); args.add("--changeflow.action=provision"); args.add("--changeflow.roles=Requester,Reviewer");
            int status = com.changeflow.ChangeFlowApplication.execute(args.toArray(String[]::new), new java.io.PrintStream(output), new java.io.PrintStream(error));
            assertThat(status).isZero(); assertThat(error.size()).isZero();
            var lines = output.toString(java.nio.charset.StandardCharsets.UTF_8).lines().toList();
            assertThat(lines.stream().filter(line -> line.startsWith("credential=")).count()).isEqualTo(1);
            String value = lines.stream().filter(line -> line.startsWith("credential=")).findFirst().orElseThrow().substring(11);
            UUID id = codec.lookupId(value).orElseThrow();
            // Inspect committed storage after the entry point's try-with-resources closes its context.
            try (var connection = java.sql.DriverManager.getConnection(url, username, password);
                 var query = connection.prepareStatement("SELECT secret_hash FROM credential WHERE credential_id=?")) {
                query.setObject(1, id);
                try (var rows = query.executeQuery()) { assertThat(rows.next()).isTrue(); assertThat(codec.verifies(value, rows.getBytes(1))).isTrue(); }
            }
            output.reset(); args = new ArrayList<>(common); args.add("--changeflow.action=revoke-credential"); args.add("--changeflow.credential-id=" + id);
            assertThat(com.changeflow.ChangeFlowApplication.execute(args.toArray(String[]::new), new java.io.PrintStream(output), new java.io.PrintStream(error))).isZero();
            assertThat(output.toString().contains("credential=")).isFalse();
            try (var connection = java.sql.DriverManager.getConnection(url, username, password);
                 var query = connection.createStatement(); var rows = query.executeQuery("SELECT revoked_at FROM credential")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getObject(1) != null).isTrue();
            }
            UUID userId = UUID.fromString(lines.stream().filter(line -> line.startsWith("userId=")).findFirst().orElseThrow().substring(7));
            output.reset(); args = new ArrayList<>(common); args.add("--changeflow.action=issue-credential"); args.add("--changeflow.user-id=" + userId);
            assertThat(com.changeflow.ChangeFlowApplication.execute(args.toArray(String[]::new), new java.io.PrintStream(output), new java.io.PrintStream(error))).isZero();
            assertThat(output.toString().lines().filter(line -> line.startsWith("credential=")).count()).isEqualTo(1);
            output.reset(); args = new ArrayList<>(common); args.add("--changeflow.action=remove-role"); args.add("--changeflow.user-id=" + userId); args.add("--changeflow.role=Requester");
            assertThat(com.changeflow.ChangeFlowApplication.execute(args.toArray(String[]::new), new java.io.PrintStream(output), new java.io.PrintStream(error))).isZero();
            assertThat(output.toString().contains("credential=")).isFalse();
            try (var connection = java.sql.DriverManager.getConnection(url, username, password);
                 var query = connection.createStatement(); var rows = query.executeQuery("SELECT requester, reviewer FROM user_access")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getBoolean(1)).isFalse(); assertThat(rows.getBoolean(2)).isTrue();
            }
            output.reset(); args = new ArrayList<>(common); args.add("--changeflow.action=issue-credential"); args.add("--changeflow.user-id=" + UUID.randomUUID());
            assertThat(com.changeflow.ChangeFlowApplication.execute(args.toArray(String[]::new), new java.io.PrintStream(output), new java.io.PrintStream(error))).isEqualTo(1);
            assertThat(output.size()).isZero();
        }
    }

    @Test void invalidAdminArgumentsFailBeforeDatabaseOrHttpStartup() {
        var output = new java.io.ByteArrayOutputStream();
        var error = new java.io.ByteArrayOutputStream();
        for (String[] args : new String[][]{
                {"--changeflow.mode=admin"}, {"--changeflow.mode=unknown"},
                {"--changeflow.mode=admin", "--changeflow.action=provision", "--changeflow.roles=Reviewer,Unknown"},
                {"--changeflow.mode=admin", "--changeflow.action=remove-role", "--changeflow.user-id=invalid"},
                {"--changeflow.action=provision"}, {"--changeflow.mode=admin", "--changeflow.mode=admin"}}) {
            assertThat(com.changeflow.ChangeFlowApplication.execute(args, new java.io.PrintStream(output), new java.io.PrintStream(error))).isEqualTo(1);
            assertThat(output.size()).isZero();
        }
    }

    @Test void mappedSnapshotsPreserveUnicodeNanosecondsAndHibernateVersions() {
        var owner = administration.provision(Set.of(UserRole.Requester));
        var reviewer = administration.provision(Set.of(UserRole.Reviewer));
        Instant at = Instant.parse("2026-11-16T10:00:00.123456789Z");
        var initial = new com.changeflow.domain.ChangeRequest(UUID.randomUUID(), "\uD83D\uDE80".repeat(200),
                "\uD83D\uDE80".repeat(5000), "\uD83D\uDE80".repeat(2000), owner.userId(),
                com.changeflow.domain.RequestState.PendingReview, "Earlier feedback", at, at);
        var tx = new TransactionTemplate(transactions);
        var inserted = tx.execute(status -> requests.insert(initial));
        var loaded = tx.execute(status -> requests.find(initial.id()).orElseThrow());
        assertThat(loaded).isEqualTo(inserted);
        assertThat(loaded.request()).isEqualTo(initial);
        var unchanged = tx.execute(status -> requests.verifyUnchanged(initial.id(), loaded.revision()));
        RequestSnapshot afterNoOp = tx.execute(status -> requests.find(initial.id()).orElseThrow());
        assertThat(afterNoOp).isEqualTo(unchanged);
        var changed = new com.changeflow.domain.ChangeRequest(initial.id(), initial.title(), initial.description(), initial.reason(),
                owner.userId(), com.changeflow.domain.RequestState.ChangesRequested, "Latest feedback", at, at.plusNanos(1));
        var updated = tx.execute(status -> {
            var result = requests.update(changed, loaded.revision());
            decisions.append(initial.id(), reviewer.userId(), com.changeflow.domain.ReviewAction.RequestChanges, changed.updatedAt(), result.revision());
            return result;
        });
        assertThat(updated.revision()).isEqualTo(loaded.revision() + 1);
        RequestSnapshot afterUpdate = tx.execute(status -> requests.find(initial.id()).orElseThrow());
        assertThat(afterUpdate).isEqualTo(updated);
        assertThat(jdbc.queryForObject("SELECT resulting_request_version FROM review_decision WHERE request_id=?", Long.class, initial.id())).isEqualTo(updated.revision());
        assertThatThrownBy(() -> tx.execute(status -> requests.update(changed, loaded.revision()))).isInstanceOf(ApplicationFailure.class);
        assertThatThrownBy(() -> tx.execute(status -> {
            var value = new com.changeflow.domain.ChangeRequest(initial.id(), "Changed title", initial.description(), initial.reason(),
                    owner.userId(), changed.state(), "Failed feedback", at, at.plusSeconds(1));
            var result = requests.update(value, updated.revision());
            decisions.append(initial.id(), reviewer.userId(), com.changeflow.domain.ReviewAction.RequestChanges, value.updatedAt(), result.revision());
            throw new IllegalStateException("Controlled failure after metadata flush.");
        })).isInstanceOf(IllegalStateException.class);
        RequestSnapshot afterRollback = tx.execute(status -> requests.find(initial.id()).orElseThrow());
        assertThat(afterRollback).isEqualTo(updated);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM review_decision WHERE request_id=?", Integer.class, initial.id())).isEqualTo(1);
    }

    @Test void accessLockRefreshesRowsAlreadyReadInTheTransaction() {
        var issued = administration.provision(Set.of(UserRole.Requester, UserRole.Reviewer));
        var caller = new AuthenticatedCaller(issued.credentialId(), issued.userId());
        var tx = new TransactionTemplate(transactions);
        var independent = new TransactionTemplate(transactions);
        independent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> {
            assertThat(access.find(issued.userId()).orElseThrow().requester()).isTrue();
            credentials.find(issued.credentialId()).orElseThrow();
            independent.executeWithoutResult(other -> administration.removeRole(issued.userId(), UserRole.Requester));
            var fresh = guard.lockAndVerify(caller);
            assertThat(fresh.requester()).isFalse(); assertThat(fresh.reviewer()).isTrue();
        });
        tx.executeWithoutResult(status -> {
            credentials.find(issued.credentialId()).orElseThrow();
            independent.executeWithoutResult(other -> administration.revokeCredential(issued.credentialId()));
            assertThatThrownBy(() -> guard.lockAndVerify(caller)).isInstanceOf(ApplicationFailure.class);
            status.setRollbackOnly();
        });
    }

    @Test void storageVisibilityPredicateRunsBeforeArbitraryOffsetPagination() {
        var owner = administration.provision(Set.of(UserRole.Requester));
        var other = administration.provision(Set.of(UserRole.Requester));
        Instant at = Instant.parse("2026-11-16T10:00:00Z");
        var tx = new TransactionTemplate(transactions);
        var own = new java.util.ArrayList<UUID>();
        var publicIds = new java.util.ArrayList<UUID>();
        for (int i = 0; i < 8; i++) {
            UUID user = i % 2 == 0 ? owner.userId() : other.userId();
            var state = i < 4 ? com.changeflow.domain.RequestState.Draft : com.changeflow.domain.RequestState.PendingReview;
            var value = new com.changeflow.domain.ChangeRequest(UUID.randomUUID(), "Title", "Description", "Reason", user, state, null, at.plusSeconds(i), at.plusSeconds(i));
            tx.executeWithoutResult(status -> requests.insert(value));
            if (user.equals(owner.userId())) own.add(value.id());
            if (state != com.changeflow.domain.RequestState.Draft) publicIds.add(value.id());
        }
        // A newly provisioned owner keeps these assertions independent of JUnit method order.
        var ownResults = tx.execute(status -> requests.listVisible(owner.userId(), true, false, 1, 2));
        assertThat(ownResults).extracting(row -> row.request().id()).containsExactly(own.get(1), own.get(2));
        assertThat(tx.execute(status -> requests.listVisible(owner.userId(), false, false, 0, 50)).isEmpty()).isTrue();
        var reviewResults = tx.execute(status -> requests.listVisible(owner.userId(), false, true, 0, 101));
        assertThat(reviewResults).allMatch(row -> row.request().state() != com.changeflow.domain.RequestState.Draft);
        assertThat(reviewResults).extracting(row -> row.request().id()).containsAll(publicIds);
    }
}
