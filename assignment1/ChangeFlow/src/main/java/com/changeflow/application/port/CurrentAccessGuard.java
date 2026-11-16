package com.changeflow.application.port;
import com.changeflow.application.AuthenticatedCaller;
public interface CurrentAccessGuard {
    UserAccessStore.Access current(AuthenticatedCaller caller);
    UserAccessStore.Access lockAndVerify(AuthenticatedCaller caller);
}
