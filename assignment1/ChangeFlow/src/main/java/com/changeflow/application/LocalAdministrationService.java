package com.changeflow.application;

import com.changeflow.application.port.*;
import com.changeflow.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.util.*;
import static com.changeflow.application.ApplicationFailure.Code.NOT_FOUND;

/** Offline operator boundary. Its caller may disclose a returned value after proxy commit. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10, rollbackFor = Exception.class)
public class LocalAdministrationService {
    public record Provisioned(UUID userId, UUID credentialId, String value) {
        @Override public String toString() { return "Provisioned[redacted]"; }
    }
    private final UserAccessStore access;
    private final CredentialStore credentials;
    private final CredentialCodec codec;
    private final Clock clock;
    public LocalAdministrationService(UserAccessStore access, CredentialStore credentials, CredentialCodec codec, Clock clock) {
        this.access = access; this.credentials = credentials; this.codec = codec; this.clock = clock;
    }
    public Provisioned provision(Set<UserRole> roles) {
        roles = Set.copyOf(roles);
        UUID id = UUID.randomUUID();
        access.insert(id, roles.contains(UserRole.Requester), roles.contains(UserRole.Reviewer), clock.instant());
        return issueFor(id);
    }
    public Provisioned issueCredential(UUID userId) {
        access.lock(userId).orElseThrow(() -> new ApplicationFailure(NOT_FOUND));
        return issueFor(userId); // inserts a NEW credential; never locks an existing credential after access
    }
    private Provisioned issueFor(UUID id) {
        var issued = codec.issue();
        credentials.insert(issued.credentialId(), id, issued.secretHash(), clock.instant());
        return new Provisioned(id, issued.credentialId(), issued.value());
    }
    public void revokeCredential(UUID credentialId) {
        var credential = credentials.lock(credentialId).orElseThrow(() -> new ApplicationFailure(NOT_FOUND));
        access.lock(credential.userId()).orElseThrow(() -> new ApplicationFailure(NOT_FOUND));
        credentials.revokeLocked(credentialId, clock.instant());
    }
    public void removeRole(UUID userId, UserRole role) {
        Objects.requireNonNull(role);
        var current = access.lock(userId).orElseThrow(() -> new ApplicationFailure(NOT_FOUND));
        access.setRolesLocked(userId, role == UserRole.Requester ? false : current.requester(),
                role == UserRole.Reviewer ? false : current.reviewer(), clock.instant());
    }
}
