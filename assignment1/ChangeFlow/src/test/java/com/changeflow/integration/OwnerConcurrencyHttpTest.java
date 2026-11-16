package com.changeflow.integration;

import com.changeflow.application.LocalAdministrationService.Provisioned;
import com.changeflow.domain.*;
import com.changeflow.integration.support.*;
import com.changeflow.integration.support.ConcurrencyTestConfiguration.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import static org.assertj.core.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OwnerConcurrencyHttpTest extends HttpTestSupport {
    @Autowired Control control;
    private Provisioned first, second;
    private ExecutorService workers;
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> completed = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger submissionWinners = new java.util.concurrent.atomic.AtomicInteger();
    @BeforeAll void actors() { first = user(UserRole.Requester); second = administration.issueCredential(first.userId()); workers = Executors.newFixedThreadPool(2); }
    @AfterEach void clear() { control.reset(); }
    @AfterAll void close() throws Exception {
        workers.shutdownNow(); assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        for (String pair : List.of("edit/edit", "edit/submit")) { assertThat(completed.get(pair).get()).isEqualTo(100); System.out.println("OWNER_PAIR " + pair + " completed=" + completed.get(pair).get()); }
        assertThat(submissionWinners.get()).isEqualTo(50); System.out.println("OWNER_SUBMISSION_WINNERS " + submissionWinners.get());
    }
    static Stream<Arguments> races() { return IntStream.range(0, 100).boxed().flatMap(rep -> Stream.of(Arguments.of(false, rep), Arguments.of(true, rep))); }
    private HttpResponse<String> write(UUID id, long revision, Provisioned actor, boolean submit, String title) throws Exception {
        return client.send(request("/api/change-requests/" + id + (submit ? "/submit" : ""), actor.value()).header("Content-Type", "application/json")
                .header("If-Match", "\"cr-" + id + "-v" + revision + "\"").method(submit ? "POST" : "PATCH", HttpRequest.BodyPublishers.ofString(submit ? "{}" : json.writeValueAsString(Map.of("title", title)))).build(), HttpResponse.BodyHandlers.ofString());
    }
    @ParameterizedTest(name = "edit/{0} repetition {1}") @MethodSource("races")
    void ownerPairSerializesOnCurrentAccessWithoutLostChanges(boolean editSubmit, int repetition) throws Exception {
        control.reset(); var seed = seed(first.userId(), RequestState.Draft); UUID id = seed.request().id();
        boolean firstSubmit = editSubmit && repetition % 2 == 0;
        boolean secondSubmit = editSubmit && !firstSubmit;
        var secondAtLock = new CountDownLatch(1);
        try (var pause = new Pause("AFTER_ACCESS", first.userId())) {
            control.hook = event -> {
                pause.accept(event);
                if (event.stage().equals("BEFORE_USER_LOCK") && event.id().equals(first.userId()) && !pause.arrivals.isEmpty()
                        && !event.thread().equals(pause.arrivals.get(0).thread())) secondAtLock.countDown();
            };
            var a = workers.submit(() -> write(id, seed.revision(), first, firstSubmit, "First title " + repetition));
            pause.awaitArrival();
            var b = workers.submit(() -> write(id, seed.revision(), second, secondSubmit, "Second title " + repetition));
            ConcurrencyTestConfiguration.await(secondAtLock, 8);
            assertThatThrownBy(() -> b.get(20, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            var firstContext = pause.arrivals.get(0).context();
            var secondCredential = control.events.stream().filter(event -> event.stage().equals("CREDENTIAL_LOCKED") && event.id().equals(second.credentialId())).findFirst().orElseThrow();
            assertThat(secondCredential.context()).isNotSameAs(firstContext);
            assertThat(control.events.stream().anyMatch(event -> event.stage().equals("USER_LOCKED") && event.context() == secondCredential.context())).isFalse();
            pause.close();
            var winner = a.get(15, TimeUnit.SECONDS); var loser = b.get(15, TimeUnit.SECONDS);
            control.hook = event -> { };
            assertThat(winner.statusCode()).isEqualTo(200); assertThat(loser.statusCode()).isEqualTo(412);
            assertThat(json.readTree(loser.body()).path("code").asText()).isEqualTo("STALE_VERSION");
            var after = stored(id); var old = seed.request();
            assertThat(after.parent().request()).isEqualTo(new ChangeRequest(id, firstSubmit ? old.title() : "First title " + repetition, old.description(), old.reason(), first.userId(), firstSubmit ? RequestState.PendingReview : RequestState.Draft, old.latestExplanation(), old.createdAt(), after.parent().request().updatedAt()));
            assertThat(after.parent().revision()).isEqualTo(seed.revision() + 1); assertThat(after.decisions()).isEmpty();
            assertThat(control.events.stream().anyMatch(event -> event.stage().equals("LOADED") && event.id().equals(id) && event.context() == secondCredential.context() && event.revision() == after.parent().revision())).isTrue();
            assertThat(call("GET", "/api/change-requests/" + id, first.value(), null, null, "application/json").body()).isEqualTo(winner.body());
            if (firstSubmit) {
                assertThat(write(id, after.parent().revision(), first, false, "Post-submit edit").statusCode()).isEqualTo(409);
                assertThat(stored(id)).isEqualTo(after);
            }
            completed.computeIfAbsent(editSubmit ? "edit/submit" : "edit/edit", ignored -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet(); if (firstSubmit) submissionWinners.incrementAndGet();
        }
    }
}
