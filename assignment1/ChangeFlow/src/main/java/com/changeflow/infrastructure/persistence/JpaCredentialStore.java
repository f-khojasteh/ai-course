package com.changeflow.infrastructure.persistence;
import com.changeflow.application.port.CredentialStore;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;

@Repository @Transactional(propagation = Propagation.MANDATORY)
public class JpaCredentialStore implements CredentialStore {
    private final EntityManager entityManager;
    private final CredentialJpaRepository repository;
    public JpaCredentialStore(EntityManager entityManager, CredentialJpaRepository repository) {
        this.entityManager = entityManager; this.repository = repository;
    }
    @Override public Optional<Credential> find(UUID id) {
        var entity = entityManager.find(CredentialEntity.class, id);
        if (entity == null) return Optional.empty();
        entityManager.refresh(entity);
        return Optional.of(value(entity));
    }
    @Override public Optional<Credential> lock(UUID id) {
        return repository.findLocked(id).map(entity -> {
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return value(entity);
        });
    }
    @Override public void insert(UUID id, UUID userId, byte[] hash, Instant at) {
        entityManager.persist(new CredentialEntity(id, userId, hash, at));
    }
    @Override public void revokeLocked(UUID id, Instant at) {
        entityManager.find(CredentialEntity.class, id).revoke(at);
    }
    private Credential value(CredentialEntity entity) {
        return new Credential(entity.credentialId(), entity.userId(), entity.secretHash(), entity.createdAt(), entity.revokedAt());
    }
}
