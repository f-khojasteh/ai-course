package com.changeflow.application;
import com.changeflow.domain.ChangeRequest;
import java.util.Objects;
public record RequestSnapshot(ChangeRequest request, long revision) {
    public RequestSnapshot {
        Objects.requireNonNull(request);
        if (revision < 0) throw new IllegalArgumentException("Invalid revision.");
    }
}
