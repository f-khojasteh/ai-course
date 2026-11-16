package com.changeflow.infrastructure.persistence;

import com.changeflow.domain.ChangeRequest;
import com.changeflow.application.ApplicationFailure;
import org.springframework.stereotype.Component;

@Component
public class ChangeRequestMapper {
    public ChangeRequest toDomain(ChangeRequestEntity entity) {
        try {
            return new ChangeRequest(entity.id, entity.title, entity.description, entity.reason,
                    entity.requesterId, entity.state, entity.latestExplanation, entity.createdAt, entity.updatedAt);
        } catch (IllegalArgumentException invalidContent) {
            // Preserve the Domain validation outcome before repository exception translation.
            // In particular, submission must reject invalid legacy text without repairing it.
            throw new ApplicationFailure(ApplicationFailure.Code.INVALID_CONTENT);
        }
    }
    public ChangeRequestEntity newEntity(ChangeRequest value) {
        var entity = new ChangeRequestEntity();
        entity.id = value.id(); entity.requesterId = value.requesterId(); entity.createdAt = value.createdAt();
        apply(value, entity);
        return entity;
    }
    public void apply(ChangeRequest value, ChangeRequestEntity entity) {
        if (!value.id().equals(entity.id) || !value.requesterId().equals(entity.requesterId)
                || !value.createdAt().equals(entity.createdAt)) {
            throw new IllegalArgumentException("Immutable request fields cannot change.");
        }
        entity.title = value.title(); entity.description = value.description(); entity.reason = value.reason();
        entity.state = value.state(); entity.latestExplanation = value.latestExplanation(); entity.updatedAt = value.updatedAt();
        // @Version belongs exclusively to Hibernate, including on insertion.
    }
}
