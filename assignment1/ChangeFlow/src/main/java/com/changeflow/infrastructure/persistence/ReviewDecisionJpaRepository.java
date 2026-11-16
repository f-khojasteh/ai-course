package com.changeflow.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ReviewDecisionJpaRepository extends JpaRepository<ReviewDecisionEntity, UUID> {
    long countByRequestId(UUID requestId);
}
