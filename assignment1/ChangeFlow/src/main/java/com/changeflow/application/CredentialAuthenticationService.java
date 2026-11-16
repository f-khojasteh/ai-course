package com.changeflow.application;
import com.changeflow.application.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.changeflow.application.ApplicationFailure.Code.AUTHENTICATION_FAILED;

@Service
public class CredentialAuthenticationService {
    private final CredentialCodec codec;
    private final CredentialStore credentials;
    private final CurrentAccessGuard access;
    public CredentialAuthenticationService(CredentialCodec codec, CredentialStore credentials, CurrentAccessGuard access) {
        this.codec = codec; this.credentials = credentials; this.access = access;
    }
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED, timeout = 10, rollbackFor = Exception.class)
    public AuthenticatedCaller authenticate(String value) {
        var id = codec.lookupId(value).orElseThrow(() -> new ApplicationFailure(AUTHENTICATION_FAILED));
        var stored = credentials.find(id).orElseThrow(() -> new ApplicationFailure(AUTHENTICATION_FAILED));
        if (stored.revokedAt() != null || !codec.verifies(value, stored.secretHash())) throw new ApplicationFailure(AUTHENTICATION_FAILED);
        var caller = new AuthenticatedCaller(id, stored.userId());
        access.current(caller);
        return caller;
    }
}
