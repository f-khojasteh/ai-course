package com.changeflow.infrastructure.persistence;
import com.changeflow.domain.ReviewAction;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "review_decision", uniqueConstraints = @UniqueConstraint(name = "decision_request_version_unique", columnNames = {"request_id", "resulting_request_version"}))
public class ReviewDecisionEntity {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(name = "request_id", nullable = false, updatable = false) private UUID requestId;
    @Column(name = "reviewer_id", nullable = false, updatable = false) private UUID reviewerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 32) private ReviewAction action;
    @Column(name = "decided_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(9) WITH TIME ZONE") private Instant decidedAt;
    @Column(name = "resulting_request_version", nullable = false, updatable = false) private long resultingRequestVersion;
    protected ReviewDecisionEntity() { }
    public ReviewDecisionEntity(UUID id, UUID requestId, UUID reviewerId, ReviewAction action, Instant at, long version) {
        this.id = id; this.requestId = requestId; this.reviewerId = reviewerId;
        this.action = action; decidedAt = at; resultingRequestVersion = version;
    }
}
