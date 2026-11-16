package com.changeflow.infrastructure.persistence;

import com.changeflow.application.*;
import com.changeflow.application.port.ChangeRequestStore;
import com.changeflow.domain.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.util.*;
import static com.changeflow.application.ApplicationFailure.Code.*;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class JpaChangeRequestStore implements ChangeRequestStore {
    private final EntityManager entityManager;
    private final ChangeRequestMapper mapper;
    public JpaChangeRequestStore(EntityManager entityManager, ChangeRequestMapper mapper) {
        this.entityManager = entityManager; this.mapper = mapper;
    }
    @Override public Optional<RequestSnapshot> find(UUID id) {
        var entity = entityManager.find(ChangeRequestEntity.class, id);
        if (entity == null) return Optional.empty();
        entityManager.refresh(entity);
        return Optional.of(snapshot(entity));
    }
    @Override public List<RequestSnapshot> listVisible(UUID userId, boolean requester, boolean reviewer, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 101) throw new IllegalArgumentException("Invalid page.");
        return entityManager.createQuery(ChangeRequestJpaRepository.VISIBLE_QUERY, ChangeRequestEntity.class)
                .setParameter("userId", userId).setParameter("requester", requester).setParameter("reviewer", reviewer)
                .setParameter("draft", RequestState.Draft).setFirstResult(offset).setMaxResults(limit)
                .getResultList().stream().map(this::snapshot).toList();
    }
    @Override public Optional<RequestSnapshot> findVisible(UUID id, UUID userId, boolean requester, boolean reviewer) {
        // Filtering precedes Domain construction: invalid hidden content must still be NOT_FOUND.
        return entityManager.createQuery(ChangeRequestJpaRepository.VISIBLE_ITEM_QUERY, ChangeRequestEntity.class)
                .setParameter("id", id).setParameter("userId", userId)
                .setParameter("requester", requester).setParameter("reviewer", reviewer)
                .setParameter("draft", RequestState.Draft).getResultList().stream().findFirst()
                .map(entity -> { entityManager.refresh(entity); return snapshot(entity); });
    }
    @Override public RequestSnapshot insert(ChangeRequest request) {
        var entity = mapper.newEntity(request);
        entityManager.persist(entity); entityManager.flush();
        return snapshot(entity);
    }
    private ChangeRequestEntity matching(UUID id, long expected) {
        var entity = entityManager.find(ChangeRequestEntity.class, id);
        if (entity == null) throw new ApplicationFailure(NOT_FOUND);
        if (entity.version() != expected) throw new ApplicationFailure(STALE_VERSION);
        return entity;
    }
    @Override public RequestSnapshot update(ChangeRequest request, long expectedRevision) {
        var entity = matching(request.id(), expectedRevision);
        mapper.apply(request, entity); entityManager.flush();
        return snapshot(entity);
    }
    @Override public RequestSnapshot verifyUnchanged(UUID id, long expectedRevision) {
        var entity = matching(id, expectedRevision);
        entityManager.lock(entity, LockModeType.OPTIMISTIC);
        return snapshot(entity);
    }
    private RequestSnapshot snapshot(ChangeRequestEntity entity) {
        return new RequestSnapshot(mapper.toDomain(entity), entity.version());
    }
}
