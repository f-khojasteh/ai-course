package com.changeflow.infrastructure.persistence;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "credential")
public class CredentialEntity {
    @Id @Column(name = "credential_id", nullable = false, updatable = false) private UUID credentialId;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "secret_hash", nullable = false, unique = true, updatable = false, columnDefinition = "BINARY(32)") private byte[] secretHash;
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(9) WITH TIME ZONE") private Instant createdAt;
    @Column(name = "revoked_at", columnDefinition = "TIMESTAMP(9) WITH TIME ZONE") private Instant revokedAt;
    protected CredentialEntity() { }
    public CredentialEntity(UUID id, UUID userId, byte[] hash, Instant at) {
        if (hash == null || hash.length != 32) throw new IllegalArgumentException("Invalid digest length.");
        credentialId = java.util.Objects.requireNonNull(id); this.userId = java.util.Objects.requireNonNull(userId);
        secretHash = hash.clone(); createdAt = java.util.Objects.requireNonNull(at);
    }
    public void revoke(Instant at) { if (revokedAt == null) revokedAt = java.util.Objects.requireNonNull(at); }
    public UUID credentialId() { return credentialId; }
    public UUID userId() { return userId; }
    public byte[] secretHash() { return secretHash.clone(); }
    public Instant createdAt() { return createdAt; }
    public Instant revokedAt() { return revokedAt; }
}
