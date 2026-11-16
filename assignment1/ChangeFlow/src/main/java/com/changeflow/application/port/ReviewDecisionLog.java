package com.changeflow.application.port;
import com.changeflow.domain.ReviewAction;
import java.time.Instant;
import java.util.UUID;
public interface ReviewDecisionLog {
    void append(UUID requestId, UUID reviewerId, ReviewAction action, Instant at, long resultingRevision);
}
