package com.changeflow.application;
/** Transport-independent stable failure categories; messages never contain client data. */
public class ApplicationFailure extends RuntimeException {
    public enum Code {
        AUTHENTICATION_FAILED, PERMISSION_DENIED, NOT_FOUND, INVALID_STATE, STALE_VERSION,
        INVALID_CONTENT, FORBIDDEN_FIELD, UNKNOWN_FIELD, INVALID_PARAMETER,
        INVALID_IF_MATCH, UNSUPPORTED_IF_MATCH, PRECONDITION_REQUIRED
    }
    private final Code code;
    public ApplicationFailure(Code code) { super(code.name()); this.code = code; }
    public Code code() { return code; }
}
