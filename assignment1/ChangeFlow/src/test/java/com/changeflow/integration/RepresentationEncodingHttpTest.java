package com.changeflow.integration;

import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.MediaType;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

/** T083: a parent-version strong validator must identify one UTF-8 byte representation. */
class RepresentationEncodingHttpTest extends HttpTestSupport {
    private static final String ROOT = "/api/change-requests";
    private static final String CONTENT = "{\"title\":\"Résumé Δ 🚀\",\"description\":\"Details\",\"reason\":\"Reason\"}";
    private HttpResponse<byte[]> bytes(String method, String path, String token, String accept, String body, String tag) throws Exception {
        var builder = request(path, token).header("Accept", accept);
        if (body != null) builder.header("Content-Type", "application/json");
        if (tag != null) builder.header("If-Match", tag);
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }
    private String tag(HttpResponse<byte[]> response) { return response.headers().firstValue("ETag").orElseThrow(); }
    private void canonical(HttpResponse<byte[]> response) {
        var type = MediaType.parseMediaType(response.headers().firstValue("Content-Type").orElseThrow());
        assertThat(type.getType()).isEqualTo("application"); assertThat(type.getSubtype()).isEqualTo("json");
        assertThat(type.getCharset() == null || type.getCharset().equals(StandardCharsets.UTF_8)).isTrue();
        assertThat(new String(response.body(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)).containsExactly(response.body());
    }
    private void sameAsDefaultGet(HttpResponse<byte[]> response, String path, String token) throws Exception {
        assertThat(response.statusCode()).isIn(200, 201); canonical(response);
        var read = bytes("GET", path, token, "application/json", null, null);
        assertThat(read.statusCode()).isEqualTo(200); canonical(read);
        assertThat(tag(response)).isEqualTo(tag(read)); assertThat(response.body()).containsExactly(read.body());
    }
    static Stream<Arguments> incompatible() {
        return Stream.of("UTF-16LE", "UTF-16BE").flatMap(charset -> Stream.of("create", "PATCH", "submit", "approve", "request-changes").map(operation -> Arguments.of(charset, operation)));
    }
    @ParameterizedTest(name = "reject {0} before {1}") @MethodSource("incompatible")
    void incompatibleEncodingRejectsBeforeAnyMutation(String charset, String operation) throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        boolean review = Set.of("approve", "request-changes").contains(operation);
        var seed = seed(owner.userId(), review ? RequestState.PendingReview : RequestState.Draft);
        String path = ROOT + "/" + seed.request().id(), accept = "application/json;charset=" + charset;
        var before = stored(seed.request().id()); long count = jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class);
        var read = bytes("GET", path, owner.value(), "application/json", null, null);
        var deniedRead = bytes("GET", path, owner.value(), accept, null, null);
        assertThat(deniedRead.statusCode()).isEqualTo(406);
        String body = operation.equals("create") ? CONTENT : operation.equals("PATCH") ? "{\"title\":\"Changed\"}" : operation.equals("request-changes") ? "{\"explanation\":\"Feedback\"}" : "{}";
        String route = operation.equals("create") ? ROOT : path + (operation.equals("PATCH") ? "" : "/" + operation);
        var response = bytes(operation.equals("PATCH") ? "PATCH" : "POST", route, review ? reviewer.value() : owner.value(), accept, body, operation.equals("create") ? null : tag(read));
        assertThat(response.statusCode()).isEqualTo(406);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("NOT_ACCEPTABLE");
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertThat(stored(seed.request().id())).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM change_request", Long.class)).isEqualTo(count);
        // Authentication still precedes MVC negotiation, with the shared JSON failure shape.
        assertThat(bytes("GET", path, null, accept, null, null).statusCode()).isEqualTo(401);
    }

    @ParameterizedTest @ValueSource(strings = {"application/json", "application/json;charset=UTF-8", "application/json;charset=utf-8",
            "application/*", "*/*", "application/json;charset=UTF-16LE;q=1, application/json;q=0.5",
            "application/json;charset=UTF-16BE;q=1, application/json;charset=UTF-8;q=0.5", "application/json;charset=UTF-16LE;q=1, */*;q=0.2"})
    void supportedNegotiationAndFallbackKeepIdenticalBytesThroughTheLifecycle(String accept) throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer);
        var created = bytes("POST", ROOT, owner.value(), accept, CONTENT, null);
        assertThat(created.statusCode()).isEqualTo(201);
        String path = created.headers().firstValue("Location").orElseThrow();
        var id = UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        sameAsDefaultGet(created, path, owner.value());
        assertThat(json.readTree(created.body()).path("title").asText()).isEqualTo("Résumé Δ 🚀");
        var before = stored(id);
        var noOp = bytes("PATCH", path, owner.value(), accept, "{}", tag(created));
        sameAsDefaultGet(noOp, path, owner.value()); assertThat(noOp.body()).containsExactly(created.body());
        assertThat(tag(noOp)).isEqualTo(tag(created)); assertThat(stored(id)).isEqualTo(before);
        var edit = bytes("PATCH", path, owner.value(), accept, "{\"title\":\"Edited Δ 🚀\"}", tag(noOp));
        sameAsDefaultGet(edit, path, owner.value()); assertThat(tag(edit)).isNotEqualTo(tag(created));
        var submit = bytes("POST", path + "/submit", owner.value(), accept, null, tag(edit)); sameAsDefaultGet(submit, path, owner.value());
        var returned = bytes("POST", path + "/request-changes", reviewer.value(), accept, "{\"explanation\":\"Review Δ\"}", tag(submit)); sameAsDefaultGet(returned, path, owner.value());
        var resubmit = bytes("POST", path + "/submit", owner.value(), accept, "{}", tag(returned)); sameAsDefaultGet(resubmit, path, owner.value());
        var approved = bytes("POST", path + "/approve", reviewer.value(), accept, null, tag(resubmit)); sameAsDefaultGet(approved, path, owner.value());
        var after = stored(id);
        assertThat(after.parent().request().state()).isEqualTo(RequestState.Approved);
        assertThat(after.parent().request().requesterId()).isEqualTo(owner.userId());
        assertThat(after.parent().request().latestExplanation()).isEqualTo("Review Δ");
        assertThat(after.parent().revision()).isEqualTo(before.parent().revision() + 5);
        assertThat(after.decisions()).hasSize(2);
    }
}
