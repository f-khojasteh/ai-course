package com.changeflow.integration;

import com.changeflow.application.*;
import com.changeflow.application.port.ChangeRequestStore;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@Import(OwnerLifecycleHttpTest.FaultConfiguration.class)
class OwnerLifecycleHttpTest extends HttpTestSupport {
    private static final String ROOT = "/api/change-requests";
    @Autowired FaultStore faults;
    @Autowired ChangeRequestService service;
    @Autowired CredentialAuthenticationService authentication;
    @Autowired org.springframework.transaction.interceptor.TransactionAttributeSource transactionAttributes;
    @AfterEach void clearFault() { faults.fail = false; }
    @TestConfiguration(proxyBeanMethods = false) static class FaultConfiguration {
        @Bean @Primary FaultStore ownerFaultStore(@Qualifier("jpaChangeRequestStore") ChangeRequestStore delegate) { return new FaultStore(delegate); }
    }
    static class FaultStore implements ChangeRequestStore {
        final ChangeRequestStore delegate;
        volatile boolean fail;
        FaultStore(ChangeRequestStore delegate) { this.delegate = delegate; }
        public Optional<RequestSnapshot> find(UUID id) { return delegate.find(id); }
        public Optional<RequestSnapshot> findVisible(UUID id, UUID user, boolean requester, boolean reviewer) { return delegate.findVisible(id, user, requester, reviewer); }
        public List<RequestSnapshot> listVisible(UUID id, boolean requester, boolean reviewer, int offset, int limit) { return delegate.listVisible(id, requester, reviewer, offset, limit); }
        public RequestSnapshot insert(ChangeRequest value) { return after(delegate.insert(value)); }
        public RequestSnapshot update(ChangeRequest value, long revision) { return after(delegate.update(value, revision)); }
        public RequestSnapshot verifyUnchanged(UUID id, long revision) { return delegate.verifyUnchanged(id, revision); }
        private RequestSnapshot after(RequestSnapshot value) {
            if (fail) FaultStore.<RuntimeException>raise(new Exception("Controlled checked failure after flush."));
            return value;
        }
        @SuppressWarnings("unchecked") private static <E extends Throwable> void raise(Throwable error) throws E { throw (E) error; }
    }
    private HttpResponse<String> write(String method, UUID id, boolean submit, String token, String body, long revision) throws Exception {
        var builder = request(ROOT + (id == null ? "" : "/" + id) + (submit ? "/submit" : ""), token).header("Content-Type", "application/json");
        if (id != null) builder.header("If-Match", "\"cr-" + id + "-v" + revision + "\"");
        return client.send(builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void createEditSubmitAndRepeatedCreatePreserveIdentityAndProposal() throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer);
        String body = "{\"title\":\"  Proposal  \",\"description\":\"Details\\nwith  spaces\",\"reason\":\"Benefit\"}";
        UUID first = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            var response = write("POST", null, false, owner.value(), body, 0);
            assertThat(response.statusCode()).isEqualTo(201);
            UUID id = UUID.fromString(json.readTree(response.body()).path("id").asText());
            assertThat(id).isNotEqualTo(first); first = id;
            var initial = stored(id);
            assertThat(initial.decisions()).isEmpty();
            var value = initial.parent().request();
            assertThat(value.requesterId()).isEqualTo(owner.userId());
            assertThat(value.title()).isEqualTo("Proposal");
            assertThat(value.description()).isEqualTo("Details\nwith  spaces");
            assertThat(value.reason()).isEqualTo("Benefit");
            assertThat(value.state()).isEqualTo(RequestState.Draft);
            assertThat(value.latestExplanation()).isNull();
            assertThat(value.updatedAt()).isEqualTo(value.createdAt());
            assertThat(write("PATCH", id, false, owner.value(), "{\"title\":\"Changed\",\"description\":\"New details\",\"reason\":\"New reason\"}", initial.parent().revision()).statusCode()).isEqualTo(200);
            var edited = stored(id);
            assertThat(edited.parent().revision()).isEqualTo(initial.parent().revision() + 1);
            assertThat(edited.parent().request()).isEqualTo(new ChangeRequest(id, "Changed", "New details", "New reason", owner.userId(), RequestState.Draft, null, value.createdAt(), edited.parent().request().updatedAt()));
            assertThat(edited.parent().request().updatedAt()).isAfterOrEqualTo(value.updatedAt());
            assertThat(edited.decisions()).isEmpty();
            assertThat(write("POST", id, true, owner.value(), "{}", edited.parent().revision()).statusCode()).isEqualTo(200);
            var submitted = stored(id);
            var changed = edited.parent().request();
            assertThat(submitted.parent().request()).isEqualTo(new ChangeRequest(id, changed.title(), changed.description(), changed.reason(), owner.userId(), RequestState.PendingReview, null, value.createdAt(), submitted.parent().request().updatedAt()));
            assertThat(submitted.parent().revision()).isEqualTo(edited.parent().revision() + 1);
            assertThat(submitted.decisions()).isEmpty();
            assertThat(write("POST", id, true, owner.value(), "{}", edited.parent().revision()).statusCode()).isEqualTo(412);
            assertThat(stored(id)).isEqualTo(submitted);
            assertThat(write("POST", id, true, owner.value(), "{}", submitted.parent().revision()).statusCode()).isEqualTo(409);
            assertThat(stored(id)).isEqualTo(submitted);
        }
    }
    @Test void unauthorizedCreationAndForgedHeadersCannotGrantAuthority() throws Exception {
        String body = "{\"title\":\"Valid\",\"description\":\"Details\",\"reason\":\"Benefit\"}";
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        for (var actor : List.of(user(UserRole.Reviewer), user())) {
            var builder = request(ROOT, actor.value()).header("X-Roles", "Requester").header("X-Requester-Id", actor.userId().toString()).header("Content-Type", "application/json");
            assertThat(client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        }
        assertThat(write("POST", null, false, null, body, 0).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(before);
    }
    @Test void nonOwnersWrongStatesAndCurrentRemovedPermissionsCannotMutate() throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer);
        var other = user(UserRole.Requester, UserRole.Reviewer);
        for (RequestState state : List.of(RequestState.Draft, RequestState.PendingReview, RequestState.Approved)) {
            var seed = seed(owner.userId(), state, state == RequestState.Approved ? "Earlier feedback" : null);
            var before = stored(seed.request().id());
            for (boolean submit : List.of(false, true)) {
                assertThat(write(submit ? "POST" : "PATCH", seed.request().id(), submit, other.value(), "{}", seed.revision()).statusCode()).isEqualTo(state == RequestState.Draft ? 404 : 403);
                assertThat(stored(seed.request().id())).isEqualTo(before);
                if (state != RequestState.Draft) {
                    assertThat(write(submit ? "POST" : "PATCH", seed.request().id(), submit, owner.value(), "{}", seed.revision()).statusCode()).isEqualTo(409);
                    assertThat(stored(seed.request().id())).isEqualTo(before);
                }
            }
        }
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        administration.removeRole(owner.userId(), UserRole.Requester);
        assertThat(write("PATCH", seed.request().id(), false, owner.value(), "{}", seed.revision()).statusCode()).isEqualTo(404);
        assertThat(write("POST", seed.request().id(), true, owner.value(), "{}", seed.revision()).statusCode()).isEqualTo(404);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.revokeCredential(other.credentialId());
        assertThat(write("POST", null, false, other.value(), "{\"title\":\"X\",\"description\":\"Y\",\"reason\":\"Z\"}", 0).statusCode()).isEqualTo(401);
    }
    @Test void checkedFailureAfterFlushRollsBackCreateEditAndSubmit() throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        faults.fail = true;
        assertThat(write("POST", null, false, owner.value(), "{\"title\":\"X\",\"description\":\"Y\",\"reason\":\"Z\"}", 0).statusCode()).isEqualTo(500);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
        assertThat(write("PATCH", seed.request().id(), false, owner.value(), "{\"title\":\"Changed\"}", seed.revision()).statusCode()).isEqualTo(500);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        assertThat(write("POST", seed.request().id(), true, owner.value(), "{}", seed.revision()).statusCode()).isEqualTo(500);
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
    @Test void applicationRechecksPermissionsAndCredentialsAfterAuthentication() {
        var owner = user(UserRole.Requester, UserRole.Reviewer);
        var caller = authentication.authenticate(owner.value());
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        administration.removeRole(owner.userId(), UserRole.Requester);
        assertThatThrownBy(() -> service.create(caller, "Title", "Details", "Reason"))
                .isInstanceOfSatisfying(ApplicationFailure.class, failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.PERMISSION_DENIED));
        assertThatThrownBy(() -> service.edit(caller, seed.request().id(), seed.revision(), new ChangeRequest.Edit(false, null, false, null, false, null)))
                .isInstanceOfSatisfying(ApplicationFailure.class, failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.NOT_FOUND));
        assertThatThrownBy(() -> service.submit(caller, seed.request().id(), seed.revision()))
                .isInstanceOfSatisfying(ApplicationFailure.class, failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.NOT_FOUND));
        administration.revokeCredential(owner.credentialId());
        List<org.assertj.core.api.ThrowableAssert.ThrowingCallable> attempts = List.of(
                () -> service.create(caller, "Title", "Details", "Reason"),
                () -> service.edit(caller, seed.request().id(), seed.revision(), new ChangeRequest.Edit(false, null, false, null, false, null)),
                () -> service.submit(caller, seed.request().id(), seed.revision()),
                () -> service.read(caller, seed.request().id()), () -> service.list(caller, 0, 50));
        for (var attempt : attempts) assertThatThrownBy(attempt).isInstanceOfSatisfying(ApplicationFailure.class,
                failure -> assertThat(failure.code()).isEqualTo(ApplicationFailure.Code.AUTHENTICATION_FAILED));
        assertThat(stored(seed.request().id())).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
    }
    @Test void publicApplicationBoundariesUseTheReviewedTransactionPolicy() {
        assertThat(org.springframework.aop.support.AopUtils.isAopProxy(service)).isTrue();
        for (var method : ChangeRequestService.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
            var attribute = transactionAttributes.getTransactionAttribute(method, ChangeRequestService.class);
            assertThat(attribute).as(method.getName()).isNotNull();
            assertThat(attribute.getIsolationLevel()).isEqualTo(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
            assertThat(attribute.getTimeout()).isEqualTo(10);
            assertThat(attribute.rollbackOn(new Exception("Checked failure"))).isTrue();
            assertThat(attribute.isReadOnly()).isEqualTo(Set.of("read", "list").contains(method.getName()));
        }
    }
}
