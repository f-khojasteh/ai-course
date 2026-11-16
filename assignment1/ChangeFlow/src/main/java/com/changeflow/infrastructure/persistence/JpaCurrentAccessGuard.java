package com.changeflow.infrastructure.persistence;
import com.changeflow.application.*;
import com.changeflow.application.port.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import static com.changeflow.application.ApplicationFailure.Code.AUTHENTICATION_FAILED;

@Component @Transactional(propagation = Propagation.MANDATORY)
public class JpaCurrentAccessGuard implements CurrentAccessGuard {
    private final CredentialStore credentials;
    private final UserAccessStore access;
    public JpaCurrentAccessGuard(CredentialStore credentials, UserAccessStore access) { this.credentials = credentials; this.access = access; }
    @Override public UserAccessStore.Access current(AuthenticatedCaller caller) {
        verify(caller, credentials.find(caller.credentialId()).orElseThrow(() -> new ApplicationFailure(AUTHENTICATION_FAILED)));
        return access.find(caller.userId()).orElseThrow(() -> new ApplicationFailure(AUTHENTICATION_FAILED));
    }
    @Override public UserAccessStore.Access lockAndVerify(AuthenticatedCaller caller) {
        // The order is global: Credential, then UserAccess. Refresh after both locks.
        var credential = credentials.lock(caller.credentialId()).orElseThrow(() -> new ApplicationFailure(AUTHENTICATION_FAILED));
        verify(caller, credential);
        return access.lock(caller.userId()).orElseThrow(() -> new ApplicationFailure(AUTHENTICATION_FAILED));
    }
    private void verify(AuthenticatedCaller caller, CredentialStore.Credential credential) {
        if (!caller.userId().equals(credential.userId()) || credential.revokedAt() != null) throw new ApplicationFailure(AUTHENTICATION_FAILED);
    }
}
