package com.changeflow.integration;

import com.changeflow.application.LocalAdministrationService.Provisioned;
import com.changeflow.domain.*;
import com.changeflow.integration.support.HttpTestSupport;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class FeedbackRetentionHttpTest extends HttpTestSupport {
    private String path(UUID id) { return "/api/change-requests/" + id; }
    private String tag(StoredSnapshot value) { return "\"cr-" + value.parent().request().id() + "-v" + value.parent().revision() + "\""; }
    private HttpResponse<String> command(StoredSnapshot value, String action, Provisioned actor, Object body) throws Exception {
        return client.send(request(path(value.parent().request().id()) + (action.equals("edit") ? "" : "/" + action), actor.value())
                .header("Content-Type", "application/json").header("If-Match", tag(value))
                .method(action.equals("edit") ? "PATCH" : "POST", HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private StoredSnapshot success(StoredSnapshot before, String action, Provisioned actor, Object body, Provisioned owner) throws Exception {
        var response = command(before, action, actor, body);
        assertThat(response.statusCode()).isEqualTo(200);
        var after = stored(before.parent().request().id());
        assertThat(response.headers().firstValue("ETag")).hasValue(tag(after));
        assertThat(call("GET", path(before.parent().request().id()), owner.value(), null, null, "application/json").body()).isEqualTo(response.body());
        return after;
    }
    private StoredSnapshot create(Provisioned owner) throws Exception {
        var response = call("POST", "/api/change-requests", owner.value(), "{\"title\":\"Initial title\",\"description\":\"Initial details\",\"reason\":\"Initial reason\"}", "application/json", "application/json");
        assertThat(response.statusCode()).isEqualTo(201);
        UUID id = UUID.fromString(json.readTree(response.body()).path("id").asText()); var value = stored(id); var parent = value.parent().request();
        assertThat(parent).isEqualTo(new ChangeRequest(id, "Initial title", "Initial details", "Initial reason", owner.userId(), RequestState.Draft, null, parent.createdAt(), parent.createdAt()));
        assertThat(value.decisions()).isEmpty();
        return value;
    }
    private void sameProposal(StoredSnapshot before, StoredSnapshot after, RequestState state, String feedback) {
        var old = before.parent().request();
        assertThat(after.parent().request()).isEqualTo(new ChangeRequest(old.id(), old.title(), old.description(), old.reason(), old.requesterId(), state, feedback, old.createdAt(), after.parent().request().updatedAt()));
        assertThat(after.parent().request().updatedAt()).isAfterOrEqualTo(old.updatedAt());
        assertThat(after.parent().revision()).isEqualTo(before.parent().revision() + 1);
    }
    private void decision(StoredSnapshot before, StoredSnapshot after, Provisioned reviewer, String action) {
        assertThat(after.decisions()).hasSize(before.decisions().size() + 1);
        assertThat(after.decisions().subList(0, before.decisions().size())).isEqualTo(before.decisions());
        var row = after.decisions().get(after.decisions().size() - 1);
        assertThat(row.reviewerId()).isEqualTo(reviewer.userId()); assertThat(row.action()).isEqualTo(action);
        assertThat(row.revision()).isEqualTo(after.parent().revision()); assertThat(row.decidedAt()).isEqualTo(after.parent().request().updatedAt());
    }
    @Test void completeLifecyclePreservesFeedbackThroughRevisionResubmissionAndApproval() throws Exception {
        var owner = user(UserRole.Requester, UserRole.Reviewer); var reviewer = user(UserRole.Reviewer); var approver = user(UserRole.Requester, UserRole.Reviewer);
        var created = create(owner);
        var pending = success(created, "submit", owner, Map.of(), owner);
        sameProposal(created, pending, RequestState.PendingReview, null); assertThat(pending.decisions()).isEmpty();
        var returned = success(pending, "request-changes", reviewer, Map.of("explanation", " \tFirst  feedback\nline \u2003"), owner);
        sameProposal(pending, returned, RequestState.ChangesRequested, "First  feedback\nline"); decision(pending, returned, reviewer, "RequestChanges");
        var revised = success(returned, "edit", owner, Map.of("title", " Revised title ", "reason", "Revised reason"), owner);
        var original = returned.parent().request();
        assertThat(revised.parent().request()).isEqualTo(new ChangeRequest(original.id(), "Revised title", original.description(), "Revised reason", original.requesterId(), RequestState.ChangesRequested, original.latestExplanation(), original.createdAt(), revised.parent().request().updatedAt()));
        assertThat(revised.parent().revision()).isEqualTo(returned.parent().revision() + 1); assertThat(revised.decisions()).isEqualTo(returned.decisions());
        var noOp = success(revised, "edit", owner, Map.of(), owner); assertThat(noOp).isEqualTo(revised);
        for (Object feedback : Arrays.asList(null, "Attempted owner replacement", original.latestExplanation())) {
            var body = new LinkedHashMap<String, Object>(); body.put("title", "Valid proposal change"); body.put("latestExplanation", feedback);
            assertThat(command(revised, "edit", owner, body).statusCode()).isEqualTo(400); assertThat(stored(original.id())).isEqualTo(revised);
        }
        var submitted = success(revised, "submit", owner, Map.of(), owner);
        sameProposal(revised, submitted, RequestState.PendingReview, "First  feedback\nline"); assertThat(submitted.decisions()).isEqualTo(revised.decisions());
        for (String action : List.of("approve", "request-changes")) {
            assertThat(command(submitted, action, owner, action.equals("approve") ? Map.of() : Map.of("explanation", "Self feedback")).statusCode()).isEqualTo(403);
            assertThat(stored(original.id())).isEqualTo(submitted);
        }
        var approved = success(submitted, "approve", approver, Map.of(), owner);
        sameProposal(submitted, approved, RequestState.Approved, "First  feedback\nline"); decision(submitted, approved, approver, "Approve");
        assertThat(approved.parent().request().requesterId()).isEqualTo(owner.userId()); assertThat(approved.parent().request().createdAt()).isEqualTo(created.parent().request().createdAt());
        for (String action : List.of("edit", "submit", "approve", "request-changes")) {
            var actor = action.equals("edit") || action.equals("submit") ? owner : approver;
            Object body = action.equals("edit") ? Map.of("reason", "Invalid later edit") : action.equals("request-changes") ? Map.of("explanation", "Later feedback") : Map.of();
            assertThat(command(approved, action, actor, body).statusCode()).isEqualTo(409); assertThat(stored(original.id())).isEqualTo(approved);
        }
    }
    @Test void onlyANewSuccessfulRequestChangesAfterResubmissionReplacesFeedback() throws Exception {
        var owner = user(UserRole.Requester); var reviewer = user(UserRole.Reviewer); var secondReviewer = user(UserRole.Reviewer); var approver = user(UserRole.Reviewer);
        var created = create(owner); var pending = success(created, "submit", owner, Map.of(), owner);
        sameProposal(created, pending, RequestState.PendingReview, null);
        var returned = success(pending, "request-changes", reviewer, Map.of("explanation", "First feedback"), owner);
        sameProposal(pending, returned, RequestState.ChangesRequested, "First feedback"); decision(pending, returned, reviewer, "RequestChanges");
        assertThat(command(returned, "request-changes", secondReviewer, Map.of("explanation", "Premature replacement")).statusCode()).isEqualTo(409); assertThat(stored(created.parent().request().id())).isEqualTo(returned);
        var revised = success(returned, "edit", owner, Map.of("description", "Revised details"), owner);
        var revisedParent = revised.parent().request(); var old = returned.parent().request();
        assertThat(revisedParent).isEqualTo(new ChangeRequest(old.id(), old.title(), "Revised details", old.reason(), old.requesterId(), RequestState.ChangesRequested, "First feedback", old.createdAt(), revisedParent.updatedAt()));
        assertThat(revised.parent().revision()).isEqualTo(returned.parent().revision() + 1); assertThat(revised.decisions()).isEqualTo(returned.decisions());
        var resubmitted = success(revised, "submit", owner, Map.of(), owner);
        sameProposal(revised, resubmitted, RequestState.PendingReview, "First feedback"); assertThat(resubmitted.decisions()).isEqualTo(revised.decisions());
        assertThat(command(returned, "request-changes", secondReviewer, Map.of("explanation", "Stale replacement")).statusCode()).isEqualTo(412); assertThat(stored(old.id())).isEqualTo(resubmitted);
        for (Object invalid : Arrays.asList(null, "", " \t\n", "x".repeat(2001))) {
            var body = new LinkedHashMap<String, Object>(); body.put("explanation", invalid);
            assertThat(command(resubmitted, "request-changes", secondReviewer, body).statusCode()).isEqualTo(400); assertThat(stored(old.id())).isEqualTo(resubmitted);
        }
        var replaced = success(resubmitted, "request-changes", secondReviewer, Map.of("explanation", "  Second feedback  "), owner);
        sameProposal(resubmitted, replaced, RequestState.ChangesRequested, "Second feedback"); decision(resubmitted, replaced, secondReviewer, "RequestChanges");
        var rows = jdbc.queryForList("SELECT * FROM review_decision WHERE request_id=? ORDER BY resulting_request_version", old.id());
        assertThat(rows).hasSize(2); assertThat(rows.toString()).doesNotContain("First feedback", "Second feedback");
        var columns = jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='REVIEW_DECISION'", String.class);
        assertThat(columns).containsExactlyInAnyOrder("ID", "REQUEST_ID", "REVIEWER_ID", "ACTION", "DECIDED_AT", "RESULTING_REQUEST_VERSION");
        assertThat(call("GET", path(old.id()), owner.value(), null, null, "application/json").body()).doesNotContain("First feedback");
        var resubmittedAgain = success(replaced, "submit", owner, Map.of(), owner);
        sameProposal(replaced, resubmittedAgain, RequestState.PendingReview, "Second feedback"); assertThat(resubmittedAgain.decisions()).isEqualTo(replaced.decisions());
        var approved = success(resubmittedAgain, "approve", approver, Map.of(), owner);
        sameProposal(resubmittedAgain, approved, RequestState.Approved, "Second feedback"); decision(resubmittedAgain, approved, approver, "Approve");
    }
}
