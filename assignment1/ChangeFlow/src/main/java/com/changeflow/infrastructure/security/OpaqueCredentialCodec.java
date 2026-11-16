package com.changeflow.infrastructure.security;

import com.changeflow.application.port.CredentialCodec;
import org.springframework.stereotype.Component;
import java.security.*;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class OpaqueCredentialCodec implements CredentialCodec {
    private static final Pattern FORMAT = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[A-Za-z0-9_-]{43}");
    private final SecureRandom random = new SecureRandom();
    @Override public Issued issue() {
        UUID id = UUID.randomUUID();
        byte[] secret = new byte[32]; random.nextBytes(secret);
        try {
            return new Issued(id, id + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(secret), digest(secret));
        } finally { Arrays.fill(secret, (byte) 0); }
    }
    @Override public Optional<UUID> lookupId(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) return Optional.empty();
        byte[] secret = Base64.getUrlDecoder().decode(value.substring(37));
        try {
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(secret).equals(value.substring(37))) return Optional.empty();
            return Optional.of(UUID.fromString(value.substring(0, 36)));
        } finally { Arrays.fill(secret, (byte) 0); }
    }
    @Override public boolean verifies(String value, byte[] expectedHash) {
        if (lookupId(value).isEmpty() || expectedHash == null || expectedHash.length != 32) return false;
        byte[] secret = Base64.getUrlDecoder().decode(value.substring(37));
        try { return MessageDigest.isEqual(digest(secret), expectedHash); }
        finally { Arrays.fill(secret, (byte) 0); }
    }
    private static byte[] digest(byte[] secret) {
        try { return MessageDigest.getInstance("SHA-256").digest(secret); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("Required digest is unavailable."); }
    }
}
