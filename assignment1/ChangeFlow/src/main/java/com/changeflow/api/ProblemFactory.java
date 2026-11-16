package com.changeflow.api;

import com.changeflow.application.ApplicationFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/** The same safe shape is used in MVC, security handlers, and filter failures. */
@Component
public class ProblemFactory {
    public enum Code {
        INVALID_JSON(400), INVALID_CONTENT(400), FORBIDDEN_FIELD(400), UNKNOWN_FIELD(400), INVALID_PARAMETER(400),
        INVALID_IF_MATCH(400), UNSUPPORTED_IF_MATCH(400), AUTHENTICATION_FAILED(401), PERMISSION_DENIED(403),
        NOT_FOUND(404), METHOD_NOT_ALLOWED(405), NOT_ACCEPTABLE(406), INVALID_STATE(409), STALE_VERSION(412),
        UNSUPPORTED_MEDIA_TYPE(415), PRECONDITION_REQUIRED(428), STORAGE_BUSY(503), STORAGE_UNAVAILABLE(503), INTERNAL_ERROR(500);
        private final int status;
        Code(int status) { this.status = status; }
        public int status() { return status; }
    }
    private final ObjectMapper json;
    public ProblemFactory(ObjectMapper json) { this.json = json; }
    public ProblemDetail create(Code code, String path) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(code.status()), detail(code));
        problem.setType(URI.create("urn:changeflow:problem:" + code.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(HttpStatus.valueOf(code.status()).getReasonPhrase());
        problem.setInstance(URI.create(path)); problem.setProperty("code", code.name());
        return problem;
    }
    public Code classify(Throwable error) {
        var causes = new ArrayList<Throwable>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) causes.add(cause);
        for (var cause : causes) {
            if (cause instanceof ApplicationFailure failure) return Code.valueOf(failure.code().name());
            if (cause instanceof org.springframework.security.access.AccessDeniedException) return Code.PERMISSION_DENIED;
            if (cause instanceof org.springframework.security.core.AuthenticationException) return Code.AUTHENTICATION_FAILED;
            if (cause instanceof org.springframework.dao.OptimisticLockingFailureException
                    || cause instanceof jakarta.persistence.OptimisticLockException
                    || cause instanceof org.hibernate.StaleStateException) return Code.STALE_VERSION;
        }
        for (var cause : causes) {
            if (cause instanceof org.springframework.dao.PessimisticLockingFailureException
                    || cause instanceof org.springframework.dao.QueryTimeoutException
                    || cause instanceof org.springframework.transaction.TransactionTimedOutException
                    || cause instanceof jakarta.persistence.LockTimeoutException
                    || cause instanceof jakarta.persistence.PessimisticLockException
                    || cause instanceof jakarta.persistence.QueryTimeoutException
                    || (cause instanceof org.hibernate.TransactionException && "transaction timeout expired".equals(cause.getMessage()))
                    || cause instanceof java.sql.SQLTimeoutException
                    || cause instanceof java.sql.SQLTransientConnectionException) return Code.STORAGE_BUSY;
        }
        for (var cause : causes) {
            if (cause instanceof org.springframework.dao.DataAccessResourceFailureException
                    || cause instanceof org.springframework.transaction.CannotCreateTransactionException
                    || cause instanceof org.springframework.transaction.TransactionSystemException
                    || cause instanceof org.hibernate.exception.JDBCConnectionException
                    || cause instanceof java.sql.SQLNonTransientConnectionException) return Code.STORAGE_UNAVAILABLE;
        }
        if (error instanceof IllegalArgumentException) return Code.INVALID_CONTENT;
        return Code.INTERNAL_ERROR;
    }
    public void write(HttpServletRequest request, HttpServletResponse response, Code code) throws IOException {
        response.setStatus(code.status()); response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (code == Code.AUTHENTICATION_FAILED) response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        json.writeValue(response.getOutputStream(), create(code, request.getRequestURI()));
    }
    private String detail(Code code) {
        return switch (code) {
            case AUTHENTICATION_FAILED -> "A valid bearer credential is required.";
            case PERMISSION_DENIED -> "The operation is not permitted.";
            case NOT_FOUND -> "The requested resource was not found.";
            case INVALID_STATE -> "The operation is not allowed in the current state.";
            case STALE_VERSION -> "Refresh the request before retrying.";
            case PRECONDITION_REQUIRED -> "Supply the observed request version.";
            case STORAGE_BUSY -> "Storage is busy. Try again after checking current data.";
            case STORAGE_UNAVAILABLE -> "Storage did not return a confirmed result. Check current data before retrying.";
            case INTERNAL_ERROR -> "The operation could not be completed.";
            default -> "The request does not satisfy the API contract.";
        };
    }
}
