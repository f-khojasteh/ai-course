package com.changeflow.infrastructure.persistence;
import com.changeflow.application.port.ReviewDecisionLog;
import com.changeflow.domain.ReviewAction;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;

@Repository @Transactional(propagation = Propagation.MANDATORY)
public class JpaReviewDecisionLog implements ReviewDecisionLog {
    private final EntityManager entityManager;
    public JpaReviewDecisionLog(EntityManager entityManager) { this.entityManager = entityManager; }
    @Override public void append(UUID requestId, UUID reviewerId, ReviewAction action, Instant at, long resultingRevision) {
        entityManager.persist(new ReviewDecisionEntity(UUID.randomUUID(), requestId, reviewerId, action, at, resultingRevision));
        entityManager.flush();
    }
}
