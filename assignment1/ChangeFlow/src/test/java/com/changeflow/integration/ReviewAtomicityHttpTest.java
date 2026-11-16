package com.changeflow.integration;

import com.changeflow.api.ProblemFactory;
import com.changeflow.application.*;
import com.changeflow.application.port.*;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

@Import(ReviewAtomicityHttpTest.FaultConfiguration.class)
class ReviewAtomicityHttpTest extends HttpTestSupport {
    enum Mode { NONE, PARENT_RUNTIME, PARENT_CHECKED, METADATA_RUNTIME, METADATA_CHECKED, DUPLICATE_METADATA, BEFORE_COMMIT, LOST_CONFIRMATION }
    static class Faults {
        volatile Mode mode = Mode.NONE;
        final AtomicInteger parentFlushed = new AtomicInteger(), metadataFlushed = new AtomicInteger();
        volatile int completion = -1;
        volatile boolean commitCallbackReached, confirmationDiscarded;
        void reset() { mode = Mode.NONE; parentFlushed.set(0); metadataFlushed.set(0); completion = -1; commitCallbackReached = false; confirmationDiscarded = false; }
    }
    @Autowired Faults faults;
    @BeforeEach void reset() { faults.reset(); }
    @AfterEach void disable() { faults.mode = Mode.NONE; }

    @TestConfiguration(proxyBeanMethods = false)
    static class FaultConfiguration {
        @Bean Faults reviewFaults() { return new Faults(); }
        @Bean @Primary ChangeRequestStore reviewFaultingRequests(@Qualifier("jpaChangeRequestStore") ChangeRequestStore delegate, Faults faults) {
            return new ChangeRequestStore() {
                public Optional<RequestSnapshot> find(UUID id) { return delegate.find(id); }
                public Optional<RequestSnapshot> findVisible(UUID id, UUID user, boolean requester, boolean reviewer) { return delegate.findVisible(id, user, requester, reviewer); }
                public List<RequestSnapshot> listVisible(UUID id, boolean requester, boolean reviewer, int offset, int limit) { return delegate.listVisible(id, requester, reviewer, offset, limit); }
                public RequestSnapshot insert(ChangeRequest value) { return delegate.insert(value); }
                public RequestSnapshot verifyUnchanged(UUID id, long revision) { return delegate.verifyUnchanged(id, revision); }
                public RequestSnapshot update(ChangeRequest value, long revision) {
                    var result = delegate.update(value, revision); // Real JPA mutation AND flush have occurred.
                    faults.parentFlushed.incrementAndGet();
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCompletion(int status) { faults.completion = status; }
                    });
                    if (faults.mode == Mode.PARENT_RUNTIME) throw new IllegalStateException("Controlled parent-flush failure.");
                    if (faults.mode == Mode.PARENT_CHECKED) ReviewAtomicityHttpTest.<RuntimeException>raise(new Exception("Controlled checked parent-flush failure."));
                    return result;
                }
            };
        }
        @Bean @Primary ReviewDecisionLog reviewFaultingDecisions(@Qualifier("jpaReviewDecisionLog") ReviewDecisionLog delegate, Faults faults) {
            return (id, reviewer, action, at, revision) -> {
                delegate.append(id, reviewer, action, at, revision); // Real INSERT and flush.
                faults.metadataFlushed.incrementAndGet();
                if (faults.mode == Mode.METADATA_RUNTIME) throw new IllegalStateException("Controlled metadata-flush failure.");
                if (faults.mode == Mode.METADATA_CHECKED) ReviewAtomicityHttpTest.<RuntimeException>raise(new Exception("Controlled checked metadata-flush failure."));
                if (faults.mode == Mode.DUPLICATE_METADATA) delegate.append(id, reviewer, action, at, revision); // Real unique-constraint failure.
                if (faults.mode == Mode.BEFORE_COMMIT) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) {
                        faults.commitCallbackReached = true;
                        throw new IllegalStateException("Controlled commit-phase failure before physical commit.");
                    }
                });
            };
        }
        @Bean FilterRegistrationBean<OncePerRequestFilter> discardReviewConfirmation(Faults faults, ProblemFactory problems) {
            var filter = new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                    if (faults.mode != Mode.LOST_CONFIRMATION || !request.getMethod().equals("POST")
                            || !(request.getRequestURI().endsWith("/approve") || request.getRequestURI().endsWith("/request-changes"))) {
                        chain.doFilter(request, response); return;
                    }
                    var capture = new ContentCachingResponseWrapper(response);
                    chain.doFilter(request, capture); // The service proxy and its real commit have returned.
                    if (capture.getStatus() >= 200 && capture.getStatus() < 300) {
                        faults.confirmationDiscarded = true;
                        response.reset();
                        problems.write(request, response, ProblemFactory.Code.STORAGE_UNAVAILABLE);
                    } else capture.copyBodyToResponse();
                }
            };
            var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
            registration.setName("testOnlyReviewConfirmationLoss"); registration.addUrlPatterns("/api/change-requests/*");
            return registration;
        }
    }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void raise(Throwable error) throws E { throw (E) error; }
    private HttpResponse<String> review(StoredSnapshot before, String action, String token) throws Exception {
        var id = before.parent().request().id();
        return client.send(request("/api/change-requests/" + id + "/" + action, token).header("Content-Type", "application/json")
                .header("If-Match", "\"cr-" + id + "-v" + before.parent().revision() + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(action.equals("approve") ? "{}" : "{\"explanation\":\"New feedback must be atomic\"}")).build(), HttpResponse.BodyHandlers.ofString());
    }
    static Stream<Arguments> rollbackCases() {
        return Stream.of(Mode.PARENT_RUNTIME, Mode.PARENT_CHECKED, Mode.METADATA_RUNTIME, Mode.METADATA_CHECKED, Mode.DUPLICATE_METADATA, Mode.BEFORE_COMMIT)
                .flatMap(mode -> Stream.of("approve", "request-changes").map(action -> Arguments.of(mode, action)));
    }
    @ParameterizedTest(name = "{0}: {1}") @MethodSource("rollbackCases")
    void knownFailuresRollBackEveryParentFieldVersionAndDecision(Mode mode, String action) throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.PendingReview, "Original feedback"); var before = stored(seed.request().id());
        faults.mode = mode;
        var response = review(before, action, reviewer.value());
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.body()).doesNotContain("Controlled", "Original feedback", "New feedback", "SQL", "constraint", "Exception");
        assertThat(faults.parentFlushed.get()).isEqualTo(1);
        assertThat(faults.metadataFlushed.get()).isEqualTo(mode == Mode.PARENT_RUNTIME || mode == Mode.PARENT_CHECKED ? 0 : 1);
        assertThat(faults.completion).isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThat(faults.commitCallbackReached).isEqualTo(mode == Mode.BEFORE_COMMIT);
        faults.mode = Mode.NONE;
        assertThat(stored(seed.request().id())).isEqualTo(before); // REQUIRES_NEW, after HTTP rollback.
    }
    @ParameterizedTest @ValueSource(strings = {"approve", "request-changes"})
    void lostConfirmationAfterCommitDoesNotImplyRollback(String action) throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.PendingReview, "Original feedback"); var before = stored(seed.request().id());
        faults.mode = Mode.LOST_CONFIRMATION;
        var response = review(before, action, reviewer.value());
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("STORAGE_UNAVAILABLE");
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertThat(faults.confirmationDiscarded).isTrue();
        assertThat(faults.completion).isEqualTo(TransactionSynchronization.STATUS_COMMITTED);
        faults.mode = Mode.NONE;
        var after = stored(seed.request().id()); var old = seed.request();
        assertThat(after.parent().request()).isEqualTo(new ChangeRequest(old.id(), old.title(), old.description(), old.reason(), old.requesterId(),
                action.equals("approve") ? RequestState.Approved : RequestState.ChangesRequested,
                action.equals("approve") ? "Original feedback" : "New feedback must be atomic", old.createdAt(), after.parent().request().updatedAt()));
        assertThat(after.parent().revision()).isEqualTo(before.parent().revision() + 1);
        assertThat(after.decisions()).hasSize(1);
        assertThat(after.decisions().get(0).revision()).isEqualTo(after.parent().revision());
        assertThat(after.decisions().get(0).reviewerId()).isEqualTo(reviewer.userId());
        assertThat(after.decisions().get(0).decidedAt()).isEqualTo(after.parent().request().updatedAt());
    }
}
