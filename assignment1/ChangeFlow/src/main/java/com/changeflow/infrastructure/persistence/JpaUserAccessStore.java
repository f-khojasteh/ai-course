package com.changeflow.infrastructure.persistence;
import com.changeflow.application.port.UserAccessStore;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;

@Repository @Transactional(propagation = Propagation.MANDATORY)
public class JpaUserAccessStore implements UserAccessStore {
    private final EntityManager entityManager;
    private final UserAccessJpaRepository repository;
    public JpaUserAccessStore(EntityManager entityManager, UserAccessJpaRepository repository) {
        this.entityManager = entityManager; this.repository = repository;
    }
    @Override public Optional<Access> find(UUID id) {
        var entity = entityManager.find(UserAccessEntity.class, id);
        if (entity == null) return Optional.empty();
        entityManager.refresh(entity);
        return Optional.of(value(entity));
    }
    @Override public Optional<Access> lock(UUID id) {
        return repository.findLocked(id).map(entity -> {
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return value(entity);
        });
    }
    @Override public void insert(UUID id, boolean requester, boolean reviewer, Instant at) {
        entityManager.persist(new UserAccessEntity(id, requester, reviewer, at));
    }
    @Override public void setRolesLocked(UUID id, boolean requester, boolean reviewer, Instant at) {
        entityManager.find(UserAccessEntity.class, id).roles(requester, reviewer, at);
    }
    private Access value(UserAccessEntity entity) { return new Access(entity.userId(), entity.requester(), entity.reviewer(), entity.updatedAt()); }
}
