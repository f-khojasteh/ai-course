package com.changeflow.application.port;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
public interface CredentialStore {
    record Credential(UUID credentialId, UUID userId, byte[] secretHash, Instant createdAt, Instant revokedAt) {
        public Credential { secretHash = secretHash.clone(); }
        @Override public byte[] secretHash() { return secretHash.clone(); }
        @Override public String toString() { return "Credential[redacted]"; }
    }
    Optional<Credential> find(UUID id);
    Optional<Credential> lock(UUID id);
    void insert(UUID id, UUID userId, byte[] hash, Instant at);
    void revokeLocked(UUID id, Instant at);
}
