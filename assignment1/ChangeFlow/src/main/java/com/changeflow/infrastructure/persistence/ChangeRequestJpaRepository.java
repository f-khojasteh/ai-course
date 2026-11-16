package com.changeflow.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ChangeRequestJpaRepository extends JpaRepository<ChangeRequestEntity, UUID> {
    String VISIBLE_PREDICATE = "(:requester = true and r.requesterId = :userId) or "
            + "(:reviewer = true and r.state <> :draft)";
    String VISIBLE_QUERY = "select r from ChangeRequestEntity r where " + VISIBLE_PREDICATE + " order by r.createdAt, r.id";
    String VISIBLE_ITEM_QUERY = "select r from ChangeRequestEntity r where r.id = :id and (" + VISIBLE_PREDICATE + ")";
}
