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
class ReviewConcurrencyHttpTest extends HttpTestSupport {
    @Autowired Control control;
    private Provisioned owner, first, second;
    private ExecutorService workers;
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> completed = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> mixedWinners = new ConcurrentHashMap<>();
    @BeforeAll void actors() { owner = user(UserRole.Requester); first = user(UserRole.Reviewer); second = user(UserRole.Reviewer); workers = Executors.newFixedThreadPool(2); }
    @AfterEach void clear() { control.reset(); }
    @AfterAll void close() throws Exception {
        workers.shutdownNow(); assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        if (!completed.isEmpty()) for (String pair : List.of("approve/approve", "request-changes/request-changes", "approve/request-changes")) {
            assertThat(completed.get(pair).get()).isEqualTo(100); System.out.println("REVIEW_PAIR " + pair + " completed=" + completed.get(pair).get());
        }
        mixedWinners.forEach((action, count) -> System.out.println("MIXED_WINNER " + action + " count=" + count.get()));
    }
    static Stream<Arguments> pairs() {
        return IntStream.range(0, 100).boxed().flatMap(repetition -> Stream.of(Arguments.of("approve", "approve", repetition), Arguments.of("request-changes", "request-changes", repetition), Arguments.of("approve", "request-changes", repetition)));
    }
    @Test void coordinationUsesDistinctRealHttpTransactionsAndContexts() throws Exception { race("approve", "request-changes", -1); }
    @ParameterizedTest(name = "{0}/{1} repetition {2}") @MethodSource("pairs")
    void overlappingReviewPair(String left, String right, int repetition) throws Exception { race(left, right, repetition); }
    private HttpResponse<String> action(UUID id, long revision, Provisioned actor, String action, String feedback) throws Exception {
        return client.send(request("/api/change-requests/" + id + "/" + action, actor.value()).header("Content-Type", "application/json")
                .header("If-Match", "\"cr-" + id + "-v" + revision + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(action.equals("approve") ? "{}" : json.writeValueAsString(Map.of("explanation", feedback)))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void race(String left, String right, int repetition) throws Exception {
        control.reset();
        var seed = seed(owner.userId(), RequestState.PendingReview, "Earlier feedback"); UUID id = seed.request().id();
        var barrier = new Overlap("BEFORE_UPDATE", id); control.hook = barrier::accept;
        String leftFeedback = "Left explanation " + repetition, rightFeedback = "Right explanation " + repetition;
        var a = workers.submit(() -> action(id, seed.revision(), first, left, leftFeedback));
        var b = workers.submit(() -> action(id, seed.revision(), second, right, rightFeedback));
        var ra = a.get(15, TimeUnit.SECONDS); var rb = b.get(15, TimeUnit.SECONDS);
        control.hook = event -> { };
        assertThat(List.of(ra.statusCode(), rb.statusCode())).containsExactlyInAnyOrder(200, 412);
        assertThat(barrier.arrivals).hasSize(2);
        var one = barrier.arrivals.get(0); var two = barrier.arrivals.get(1);
        assertThat(one.context()).isNotSameAs(two.context()); assertThat(one.thread()).isNotEqualTo(two.thread());
        assertThat(one.revision()).isEqualTo(seed.revision()); assertThat(two.revision()).isEqualTo(seed.revision());
        for (var event : barrier.arrivals) assertThat(control.events.stream().anyMatch(load -> load.stage().equals("LOADED") && load.id().equals(id) && load.context() == event.context() && load.revision() == seed.revision())).isTrue();
        boolean leftWon = ra.statusCode() == 200;
        var winner = leftWon ? ra : rb; var loser = leftWon ? rb : ra;
        String action = leftWon ? left : right; var actor = leftWon ? first : second;
        String feedback = action.equals("approve") ? "Earlier feedback" : leftWon ? leftFeedback : rightFeedback;
        var after = stored(id); var old = seed.request();
        assertThat(after.parent().request()).isEqualTo(new ChangeRequest(id, old.title(), old.description(), old.reason(), owner.userId(), action.equals("approve") ? RequestState.Approved : RequestState.ChangesRequested, feedback, old.createdAt(), after.parent().request().updatedAt()));
        assertThat(after.parent().revision()).isEqualTo(seed.revision() + 1);
        assertThat(after.decisions()).hasSize(1); var row = after.decisions().get(0);
        assertThat(row.reviewerId()).isEqualTo(actor.userId()); assertThat(row.action()).isEqualTo(action.equals("approve") ? "Approve" : "RequestChanges");
        assertThat(row.revision()).isEqualTo(after.parent().revision()); assertThat(row.decidedAt()).isEqualTo(after.parent().request().updatedAt());
        assertThat(json.readTree(loser.body()).path("code").asText()).isEqualTo("STALE_VERSION");
        assertThat(winner.headers().firstValue("ETag")).hasValue("\"cr-" + id + "-v" + after.parent().revision() + "\"");
        assertThat(winner.body()).isEqualTo(call("GET", "/api/change-requests/" + id, owner.value(), null, null, "application/json").body());
        assertThat(stored(id)).isEqualTo(after);
        if (repetition >= 0) { completed.computeIfAbsent(left + "/" + right, ignored -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet(); if (!left.equals(right)) mixedWinners.computeIfAbsent(action, ignored -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet(); }
    }
}
