package com.changeflow.application.port;
import com.changeflow.application.RequestSnapshot;
import com.changeflow.domain.ChangeRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface ChangeRequestStore {
    Optional<RequestSnapshot> find(UUID id);
    /** Exclude inaccessible rows before rehydrating/validating their proposal content. */
    Optional<RequestSnapshot> findVisible(UUID id, UUID userId, boolean requester, boolean reviewer);
    List<RequestSnapshot> listVisible(UUID userId, boolean requester, boolean reviewer, int offset, int limit);
    RequestSnapshot insert(ChangeRequest request);
    RequestSnapshot update(ChangeRequest request, long expectedRevision);
    RequestSnapshot verifyUnchanged(UUID id, long expectedRevision);
}
