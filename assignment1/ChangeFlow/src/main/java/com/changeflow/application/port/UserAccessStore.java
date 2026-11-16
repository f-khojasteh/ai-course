package com.changeflow.application.port;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
public interface UserAccessStore {
    record Access(UUID userId, boolean requester, boolean reviewer, Instant updatedAt) { }
    Optional<Access> find(UUID id);
    Optional<Access> lock(UUID id);
    void insert(UUID id, boolean requester, boolean reviewer, Instant at);
    void setRolesLocked(UUID id, boolean requester, boolean reviewer, Instant at);
}
