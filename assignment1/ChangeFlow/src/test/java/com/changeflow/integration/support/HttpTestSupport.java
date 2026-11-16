package com.changeflow.integration.support;

import com.changeflow.application.*;
import com.changeflow.application.port.ChangeRequestStore;
import com.changeflow.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestDatabaseConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class HttpTestSupport {
    @LocalServerPort protected int port;
    @Autowired protected ObjectMapper json;
    @Autowired protected LocalAdministrationService administration;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PlatformTransactionManager transactions;
    @Autowired protected ChangeRequestStore requests;
    protected final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    protected LocalAdministrationService.Provisioned user(UserRole... roles) { return administration.provision(Set.of(roles)); }
    protected HttpRequest.Builder request(String path, String credential) {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        if (credential != null) builder.header("Authorization", "Bearer " + credential);
        return builder;
    }
    protected HttpResponse<String> call(String method, String path, String credential, String body, String contentType, String accept) throws Exception {
        var builder = request(path, credential).header("Accept", accept);
        if (contentType != null) builder.header("Content-Type", contentType);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    protected RequestSnapshot seed(UUID owner, RequestState state, String explanation) {
        Instant at = Instant.parse("2026-11-16T10:00:00.123456789Z");
        var value = new ChangeRequest(UUID.randomUUID(), "Seed proposal", "Seed description", "Seed reason", owner, state, explanation, at, at);
        return new TransactionTemplate(transactions).execute(status -> requests.insert(value));
    }
    protected RequestSnapshot seed(UUID owner, RequestState state) {
        return seed(owner, state, state == RequestState.ChangesRequested ? "Seed feedback" : null);
    }
    public record DecisionSnapshot(UUID id, UUID reviewerId, String action, Instant decidedAt, long revision) { }
    public record StoredSnapshot(RequestSnapshot parent, List<DecisionSnapshot> decisions) {
        public StoredSnapshot { decisions = List.copyOf(decisions); }
    }
    protected StoredSnapshot stored(UUID id) {
        var verification = new TransactionTemplate(transactions);
        verification.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verification.setReadOnly(true);
        return verification.execute(status -> new StoredSnapshot(requests.find(id).orElseThrow(), jdbc.query(
                "SELECT id, reviewer_id, action, decided_at, resulting_request_version FROM review_decision WHERE request_id=? ORDER BY resulting_request_version",
                (row, number) -> new DecisionSnapshot(row.getObject(1, UUID.class), row.getObject(2, UUID.class), row.getString(3), row.getObject(4, OffsetDateTime.class).toInstant(), row.getLong(5)), id)));
    }
}
