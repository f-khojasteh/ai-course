package com.changeflow.infrastructure.security;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class OpaqueCredentialCodecTest {
    private final OpaqueCredentialCodec codec = new OpaqueCredentialCodec();
    @Test void random256BitSecretAndDigestOnlyStorageMaterial() throws Exception {
        var issued = codec.issue();
        String[] pieces = issued.value().split("\\.");
        assertThat(pieces.length).isEqualTo(2);
        assertThat(UUID.fromString(pieces[0])).isEqualTo(issued.credentialId());
        byte[] secret = Base64.getUrlDecoder().decode(pieces[1]);
        assertThat(secret.length).isEqualTo(32);
        assertThat(issued.secretHash().length).isEqualTo(32);
        assertThat(MessageDigest.isEqual(issued.secretHash(), MessageDigest.getInstance("SHA-256").digest(secret))).isTrue();
        assertThat(codec.verifies(issued.value(), issued.secretHash())).isTrue();
        byte[] changed = issued.secretHash(); changed[0] ^= 1;
        assertThat(codec.verifies(issued.value(), changed)).isFalse();
        assertThat(codec.verifies(issued.value(), null)).isFalse();
        assertThat(issued.toString().contains(issued.value())).isFalse();
        // Review the security-sensitive API choice as well as functional outcomes.
        assertThat(Files.readString(Path.of("src/main/java/com/changeflow/infrastructure/security/OpaqueCredentialCodec.java")))
                .contains("MessageDigest.isEqual(", "SecureRandom");
    }
    @Test void distinctIssuancesAndDefensiveStorageCopies() {
        Set<UUID> ids = new HashSet<>(); Set<String> values = new HashSet<>();
        for (int i = 0; i < 100; i++) { var issued = codec.issue(); ids.add(issued.credentialId()); values.add(issued.value()); }
        assertThat(ids.size()).isEqualTo(100); assertThat(values.size()).isEqualTo(100);
        var issued = codec.issue(); byte[] copy = issued.secretHash(); Arrays.fill(copy, (byte) 0);
        assertThat(codec.verifies(issued.value(), issued.secretHash())).isTrue();
    }
    @Test void invalidAndNoncanonicalInputsNeverAuthenticate() {
        var valid = codec.issue();
        for (String input : new String[]{null, "", "Bearer ", "not-a-token", valid.value() + "=", " " + valid.value(), valid.value() + ".extra", UUID.randomUUID() + ".AA"}) {
            assertThat(codec.lookupId(input).isEmpty()).isTrue();
            assertThat(codec.verifies(input, valid.secretHash())).isFalse();
        }
    }
}
