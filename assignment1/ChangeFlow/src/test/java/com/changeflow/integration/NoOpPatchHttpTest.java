package com.changeflow.integration;

import com.changeflow.application.RequestSnapshot;
import com.changeflow.application.port.ChangeRequestStore;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoOpPatchHttpTest extends HttpTestSupport {
    @MockitoSpyBean ChangeRequestStore observedStore;
    private HttpResponse<String> patch(RequestSnapshot seed, String token, String body, String tag) throws Exception {
        var builder = request("/api/change-requests/" + seed.request().id(), token).header("Content-Type", "application/json");
        if (tag != null) builder.header("If-Match", tag);
        return client.send(builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private String tag(RequestSnapshot seed) { return "\"cr-" + seed.request().id() + "-v" + seed.revision() + "\""; }
    @Test void emptyAndNormalizedIdenticalPatchesKeepNanosecondsRevisionAndRepresentation() throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        var read = call("GET", "/api/change-requests/" + seed.request().id(), owner.value(), null, null, "application/json");
        assertThat(read.statusCode()).isEqualTo(200);
        for (String body : new String[]{"{}", "{\"title\":\"  Seed proposal  \"}", "{\"title\":\"Seed proposal\",\"description\":\"Seed description\",\"reason\":\"Seed reason\"}"}) {
            var response = patch(seed, owner.value(), body, tag(seed));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("ETag")).hasValue(tag(seed));
            assertThat(response.body()).isEqualTo(read.body());
            assertThat(json.readTree(response.body()).path("updatedAt").asText()).isEqualTo("2026-11-16T10:00:00.123456789Z");
            assertThat(stored(seed.request().id())).isEqualTo(before);
            assertThat(call("GET", "/api/change-requests/" + seed.request().id(), owner.value(), null, null, "application/json").body()).isEqualTo(response.body());
        }
        verify(observedStore, times(3)).verifyUnchanged(seed.request().id(), seed.revision());
        verify(observedStore, never()).update(any(), anyLong());
    }
    @Test void noOpStillRequiresCurrentAccessVersionAndEditableState() throws Exception {
        var owner = user(UserRole.Requester);
        var reviewer = user(UserRole.Reviewer);
        var seed = seed(owner.userId(), RequestState.Draft);
        var before = stored(seed.request().id());
        assertThat(patch(seed, owner.value(), "{}", null).statusCode()).isEqualTo(428);
        assertThat(patch(seed, owner.value(), "{}", "\"cr-" + seed.request().id() + "-v99\"").statusCode()).isEqualTo(412);
        assertThat(patch(seed, reviewer.value(), "{}", tag(seed)).statusCode()).isEqualTo(404);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.removeRole(owner.userId(), UserRole.Requester);
        assertThat(patch(seed, owner.value(), "{}", tag(seed)).statusCode()).isEqualTo(404);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        administration.revokeCredential(owner.credentialId());
        assertThat(patch(seed, owner.value(), "{}", tag(seed)).statusCode()).isEqualTo(401);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        var activeOwner = user(UserRole.Requester);
        for (RequestState state : new RequestState[]{RequestState.PendingReview, RequestState.Approved}) {
            var item = seed(activeOwner.userId(), state);
            var original = stored(item.request().id());
            assertThat(patch(item, activeOwner.value(), "{}", tag(item)).statusCode()).isEqualTo(409);
            assertThat(stored(item.request().id())).isEqualTo(original);
        }
    }
    @Test void staleNoOpCannotConcealAnInterveningChangedWrite() throws Exception {
        var owner = user(UserRole.Requester);
        var seed = seed(owner.userId(), RequestState.Draft);
        var changed = patch(seed, owner.value(), "{\"title\":\"Changed\"}", tag(seed));
        assertThat(changed.statusCode()).isEqualTo(200);
        var before = stored(seed.request().id());
        assertThat(before.parent().revision()).isEqualTo(seed.revision() + 1);
        assertThat(patch(seed, owner.value(), "{}", tag(seed)).statusCode()).isEqualTo(412);
        assertThat(stored(seed.request().id())).isEqualTo(before);
        var current = patch(before.parent(), owner.value(), "{\"title\":\" Changed \"}", tag(before.parent()));
        assertThat(current.statusCode()).isEqualTo(200);
        assertThat(current.body()).isEqualTo(changed.body());
        assertThat(stored(seed.request().id())).isEqualTo(before);
    }
}
