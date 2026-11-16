package com.changeflow.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "user_access")
public class UserAccessEntity {
    @Id @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(nullable = false) private boolean requester;
    @Column(nullable = false) private boolean reviewer;
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(9) WITH TIME ZONE") private Instant updatedAt;
    protected UserAccessEntity() { }
    public UserAccessEntity(UUID userId, boolean requester, boolean reviewer, Instant at) {
        this.userId = java.util.Objects.requireNonNull(userId); roles(requester, reviewer, at);
    }
    public void roles(boolean requester, boolean reviewer, Instant at) {
        this.requester = requester; this.reviewer = reviewer; this.updatedAt = java.util.Objects.requireNonNull(at);
    }
    public UUID userId() { return userId; }
    public boolean requester() { return requester; }
    public boolean reviewer() { return reviewer; }
    public Instant updatedAt() { return updatedAt; }
}
