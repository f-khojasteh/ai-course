package com.changeflow.infrastructure.persistence;
import com.changeflow.domain.RequestState;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "change_request")
public class ChangeRequestEntity {
    @Id @Column(nullable = false, updatable = false) UUID id;
    @Column(nullable = false, length = 400) String title;
    @Lob @Column(nullable = false) String description;
    @Lob @Column(nullable = false) String reason;
    @Column(name = "requester_id", nullable = false, updatable = false) UUID requesterId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) RequestState state;
    @Lob @Column(name = "latest_explanation") String latestExplanation;
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(9) WITH TIME ZONE") Instant createdAt;
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(9) WITH TIME ZONE") Instant updatedAt;
    @Version @Column(nullable = false) private Long version;
    protected ChangeRequestEntity() { }
    public UUID id() { return id; }
    public Long version() { return version; }
}
