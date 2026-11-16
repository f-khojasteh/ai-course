package com.changeflow.application.port;
import java.util.Optional;
import java.util.UUID;
public interface CredentialCodec {
    record Issued(UUID credentialId, String value, byte[] secretHash) {
        public Issued { secretHash = secretHash.clone(); }
        @Override public byte[] secretHash() { return secretHash.clone(); }
        @Override public String toString() { return "Issued[redacted]"; }
    }
    Issued issue();
    Optional<UUID> lookupId(String value);
    boolean verifies(String value, byte[] expectedHash);
}
