package com.changeflow.integration;

import com.changeflow.domain.*;
import com.changeflow.integration.support.*;
import com.changeflow.integration.support.ConcurrencyTestConfiguration.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
class LifecycleRollbackHttpTest extends HttpTestSupport {
    @Autowired Control control;
    @AfterEach void clear() { control.reset(); }
    static Stream<Arguments> cases() {
        return Stream.of("create", "edit-draft", "edit-returned", "submit-draft", "submit-returned").flatMap(operation -> Stream.of("runtime", "checked", "commit").map(mode -> Arguments.of(operation, mode)));
    }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void raise(Throwable error) throws E { throw (E) error; }
    @ParameterizedTest(name = "{0}/{1}") @MethodSource("cases")
    void everyKnownFailureAfterRealFlushRollsBackTheCompleteOperation(String operation, String mode) throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), operation.endsWith("returned") ? RequestState.ChangesRequested : RequestState.Draft);
        UUID id = seed.request().id(); var before = stored(id); long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        var reached = new AtomicInteger(); var completion = new AtomicInteger(-1);
        control.hook = event -> {
            if (!event.stage().equals(operation.equals("create") ? "INSERTED" : "UPDATED")) return;
            reached.incrementAndGet();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { completion.set(status); }
                @Override public void beforeCommit(boolean readOnly) { if (mode.equals("commit")) throw new IllegalStateException("Controlled owner commit-phase failure."); }
            });
            if (mode.equals("runtime")) throw new IllegalStateException("Controlled owner runtime failure after real flush.");
            if (mode.equals("checked")) LifecycleRollbackHttpTest.<RuntimeException>raise(new Exception("Controlled checked owner failure after real flush."));
        };
        String path = "/api/change-requests" + (operation.equals("create") ? "" : "/" + id + (operation.startsWith("submit") ? "/submit" : ""));
        String body = operation.equals("create") ? "{\"title\":\"Rolled back creation\",\"description\":\"Details\",\"reason\":\"Reason\"}" : operation.startsWith("submit") ? "{}" : "{\"title\":\"Rolled back edit\"}";
        var builder = request(path, owner.value()).header("Content-Type", "application/json");
        if (!operation.equals("create")) builder.header("If-Match", "\"cr-" + id + "-v" + seed.revision() + "\"");
        var response = client.send(builder.method(operation.startsWith("edit") ? "PATCH" : "POST", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        control.reset();
        assertThat(response.statusCode()).isEqualTo(500); assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.body()).doesNotContain("Controlled", "Rolled back", "SQL", "Exception");
        assertThat(reached.get()).isEqualTo(1); assertThat(completion.get()).isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(stored(id)).isEqualTo(before); assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
    }
}
