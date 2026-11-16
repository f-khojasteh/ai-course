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
class RevocationConcurrencyHttpTest extends HttpTestSupport {
    @Autowired Control control;
    @AfterEach void clear() { control.reset(); }
    static Stream<Arguments> orderings() {
        return Stream.of("create", "edit", "submit", "approve", "request-changes").flatMap(operation -> Stream.of(false, true).flatMap(credential -> Stream.of(false, true).map(writeFirst -> Arguments.of(operation, credential, writeFirst))));
    }
    private HttpResponse<String> write(StoredSnapshot before, Provisioned actor, String operation) throws Exception {
        UUID id = before.parent().request().id();
        String path = "/api/change-requests" + (operation.equals("create") ? "" : "/" + id + (operation.equals("edit") ? "" : "/" + operation));
        String body = switch (operation) { case "create" -> "{\"title\":\"Created during race\",\"description\":\"Details\",\"reason\":\"Reason\"}"; case "edit" -> "{\"title\":\"Edited during race\"}"; case "request-changes" -> "{\"explanation\":\"Winning feedback\"}"; default -> "{}"; };
        var builder = request(path, actor.value()).header("Content-Type", "application/json");
        if (!operation.equals("create")) builder.header("If-Match", "\"cr-" + id + "-v" + before.parent().revision() + "\"");
        return client.send(builder.method(operation.equals("edit") ? "PATCH" : "POST", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @ParameterizedTest(name = "{0}, credential={1}, writeFirst={2}") @MethodSource("orderings")
    void realAdministrationAndHttpWriteHonorBothLockWinningOrders(String operation, boolean credential, boolean writeFirst) throws Exception {
        var actor = user(UserRole.Requester, UserRole.Reviewer); var owner = user(UserRole.Requester);
        boolean review = operation.equals("approve") || operation.equals("request-changes");
        var seed = seed(review ? owner.userId() : actor.userId(), review ? RequestState.PendingReview : RequestState.ChangesRequested, "Earlier feedback");
        var before = stored(seed.request().id()); long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        UserRole required = review ? UserRole.Reviewer : UserRole.Requester;
        String heldStage = writeFirst ? "AFTER_ACCESS" : credential ? "REVOKE_WRITTEN" : "ROLES_WRITTEN";
        UUID heldId = writeFirst || !credential ? actor.userId() : actor.credentialId();
        String blockedStage = credential ? "BEFORE_CREDENTIAL_LOCK" : "BEFORE_USER_LOCK";
        UUID blockedId = credential ? actor.credentialId() : actor.userId();
        var attempted = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        try (var pause = new Pause(heldStage, heldId)) {
            control.hook = event -> {
                pause.accept(event);
                if (event.stage().equals(blockedStage) && event.id().equals(blockedId) && !pause.arrivals.isEmpty() && !event.thread().equals(pause.arrivals.get(0).thread())) attempted.countDown();
            };
            Callable<Void> remove = () -> { if (credential) administration.revokeCredential(actor.credentialId()); else administration.removeRole(actor.userId(), required); return null; };
            Future<HttpResponse<String>> request;
            Future<Void> removal;
            if (writeFirst) {
                request = pool.submit(() -> write(before, actor, operation)); pause.awaitArrival(); removal = pool.submit(remove);
            } else {
                removal = pool.submit(remove); pause.awaitArrival(); request = pool.submit(() -> write(before, actor, operation));
            }
            ConcurrencyTestConfiguration.await(attempted, 8);
            Future<?> blocked = writeFirst ? removal : request;
            assertThatThrownBy(() -> blocked.get(30, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            pause.close();
            var response = request.get(15, TimeUnit.SECONDS); removal.get(15, TimeUnit.SECONDS); control.hook = event -> { };
            var holderContext = pause.arrivals.get(0).context();
            var contender = control.events.stream().filter(event -> event.stage().equals(blockedStage) && event.id().equals(blockedId) && !event.thread().equals(pause.arrivals.get(0).thread())).findFirst().orElseThrow();
            assertThat(contender.context()).isNotSameAs(holderContext);
            for (var event : control.events) {
                if (event.stage().equals("AFTER_ACCESS") || event.stage().equals("REVOKE_WRITTEN")) {
                    int credentialLock = java.util.stream.IntStream.range(0, control.events.size()).filter(index -> control.events.get(index).context() == event.context() && control.events.get(index).stage().equals("CREDENTIAL_LOCKED")).findFirst().orElseThrow();
                    int userLock = java.util.stream.IntStream.range(0, control.events.size()).filter(index -> control.events.get(index).context() == event.context() && control.events.get(index).stage().equals("USER_LOCKED")).findFirst().orElseThrow();
                    assertThat(credentialLock).isLessThan(userLock); assertThat(userLock).isLessThan(control.events.indexOf(event));
                }
                if (event.stage().equals("ROLES_WRITTEN")) assertThat(control.events.stream().noneMatch(otherEvent -> otherEvent.context() == event.context() && otherEvent.stage().equals("CREDENTIAL_LOCKED"))).isTrue();
            }
            int denied = credential ? 401 : operation.equals("create") || !review ? 403 : 404;
            assertThat(response.statusCode()).isEqualTo(writeFirst ? operation.equals("create") ? 201 : 200 : denied);
            var revokedAt = jdbc.queryForMap("SELECT revoked_at FROM credential WHERE credential_id=?", actor.credentialId()).get("REVOKED_AT");
            assertThat(revokedAt != null).isEqualTo(credential);
            var flags = jdbc.queryForMap("SELECT requester, reviewer FROM user_access WHERE user_id=?", actor.userId());
            assertThat(flags.get("REQUESTER")).isEqualTo(credential || required != UserRole.Requester);
            assertThat(flags.get("REVIEWER")).isEqualTo(credential || required != UserRole.Reviewer);
            if (!writeFirst) {
                assertThat(stored(seed.request().id())).isEqualTo(before);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
            } else {
                if (operation.equals("create")) {
                    UUID createdId = UUID.fromString(json.readTree(response.body()).path("id").asText()); var created = stored(createdId); var value = created.parent().request();
                    assertThat(value).isEqualTo(new ChangeRequest(createdId, "Created during race", "Details", "Reason", actor.userId(), RequestState.Draft, null, value.createdAt(), value.createdAt()));
                    assertThat(created.decisions()).isEmpty(); assertThat(stored(seed.request().id())).isEqualTo(before);
                    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count + 1);
                } else {
                    var after = stored(seed.request().id()); var old = seed.request();
                    RequestState state = switch (operation) { case "edit" -> old.state(); case "submit" -> RequestState.PendingReview; case "approve" -> RequestState.Approved; default -> RequestState.ChangesRequested; };
                    assertThat(after.parent().request()).isEqualTo(new ChangeRequest(old.id(), operation.equals("edit") ? "Edited during race" : old.title(), old.description(), old.reason(), old.requesterId(), state, operation.equals("request-changes") ? "Winning feedback" : old.latestExplanation(), old.createdAt(), after.parent().request().updatedAt()));
                    assertThat(after.parent().revision()).isEqualTo(before.parent().revision() + 1);
                    assertThat(after.decisions()).hasSize(review ? 1 : 0);
                    if (review) { var decision = after.decisions().get(0); assertThat(decision.reviewerId()).isEqualTo(actor.userId()); assertThat(decision.action()).isEqualTo(operation.equals("approve") ? "Approve" : "RequestChanges"); assertThat(decision.revision()).isEqualTo(after.parent().revision()); assertThat(decision.decidedAt()).isEqualTo(after.parent().request().updatedAt()); }
                    assertThat(write(before, actor, operation).statusCode()).isEqualTo(denied); assertThat(stored(old.id())).isEqualTo(after);
                }
            }
        } finally { pool.shutdownNow(); assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue(); }
    }
}
