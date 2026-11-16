package com.changeflow.application;
import java.util.Objects;
import java.util.UUID;
/** Server-verified proof; current permissions must be re-read in each operation. */
public record AuthenticatedCaller(UUID credentialId, UUID userId) {
    public AuthenticatedCaller { Objects.requireNonNull(credentialId); Objects.requireNonNull(userId); }
}
