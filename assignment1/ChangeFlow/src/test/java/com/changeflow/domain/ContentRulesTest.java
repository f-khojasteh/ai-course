package com.changeflow.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.function.UnaryOperator;
import static org.assertj.core.api.Assertions.*;

class ContentRulesTest {
    private UnaryOperator<String> rule(String field) {
        return switch (field) {
            case "title" -> ContentRules::title;
            case "description" -> ContentRules::description;
            case "reason" -> ContentRules::reason;
            default -> ContentRules::explanation;
        };
    }
    @ParameterizedTest @CsvSource({"title,200", "description,5000", "reason,2000", "explanation,2000"})
    void inclusiveNormalizedCodePointLimits(String field, int limit) {
        var validate = rule(field);
        String supplementary = "\uD83D\uDE80".repeat(limit);
        assertThat(validate.apply(" \t" + supplementary + "\n")).isEqualTo(supplementary);
        assertThat(validate.apply("x".repeat(limit))).hasSize(limit);
        assertThatIllegalArgumentException().isThrownBy(() -> validate.apply(supplementary + "x"));
        assertThatIllegalArgumentException().isThrownBy(() -> validate.apply("x".repeat(limit + 1)));
    }
    @ParameterizedTest @CsvSource({"title", "description", "reason", "explanation"})
    void nullBlankAndStripSemantics(String field) {
        var validate = rule(field);
        for (String invalid : new String[]{null, "", " \t\n\u2003"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> validate.apply(invalid));
        }
        assertThat(validate.apply("\u2003 A  B\n C \u2003")).isEqualTo("A  B\n C");
        // NBSP is not Character.isWhitespace: Java strip must preserve it.
        assertThat(validate.apply("\u00a0x\u00a0")).isEqualTo("\u00a0x\u00a0");
    }
    @Test void closedEnumSets() {
        assertThat(RequestState.values()).extracting(Enum::name).containsExactly("Draft", "PendingReview", "ChangesRequested", "Approved");
        assertThat(UserRole.values()).extracting(Enum::name).containsExactly("Requester", "Reviewer");
        assertThat(ReviewAction.values()).extracting(Enum::name).containsExactly("Approve", "RequestChanges");
    }

    @Test void rehydrationPreservesIndependentStoredValuesAndRejectsInvalidContent() {
        var id = java.util.UUID.randomUUID(); var owner = java.util.UUID.randomUUID();
        var at = java.time.Instant.parse("2026-11-16T10:00:00.123456789Z");
        var stored = new ChangeRequest(id, "Title", "Description", "Reason", owner, RequestState.ChangesRequested, "Feedback", at, at);
        assertThat(stored.id()).isEqualTo(id); assertThat(stored.requesterId()).isEqualTo(owner);
        assertThat(stored.latestExplanation()).isEqualTo("Feedback"); assertThat(stored.createdAt()).isEqualTo(at);
        assertThatIllegalArgumentException().isThrownBy(() -> new ChangeRequest(id, " Title ", "Description", "Reason", owner, RequestState.Draft, null, at, at));
        assertThatIllegalArgumentException().isThrownBy(() -> new ChangeRequest(id, "Title", "Description", null, owner, RequestState.Draft, null, at, at));
        assertThatIllegalArgumentException().isThrownBy(() -> new ChangeRequest(id, "Title", "Description", "Reason", owner, RequestState.ChangesRequested, null, at, at));
        assertThatIllegalArgumentException().isThrownBy(() -> new ChangeRequest(id, "Title", "Description", "Reason", owner, RequestState.Approved, " ", at, at));
    }
}
