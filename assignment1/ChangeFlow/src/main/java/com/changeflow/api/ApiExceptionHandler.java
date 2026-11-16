package com.changeflow.api;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.*;
import static com.changeflow.api.ProblemFactory.Code.*;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Set<String> CONTROLLED = Set.of("requesterId", "requesterIdentity", "roles", "state", "id", "revision", "createdAt", "updatedAt", "latestExplanation");
    private final ProblemFactory problems;
    public ApiExceptionHandler(ProblemFactory problems) { this.problems = problems; }
    @Override protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException error, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnrecognizedPropertyException unknown) {
                return response(CONTROLLED.contains(unknown.getPropertyName()) ? FORBIDDEN_FIELD : UNKNOWN_FIELD, headers, request, null);
            }
        }
        return response(INVALID_JSON, headers, request, null);
    }
    @Override protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var fields = error.getBindingResult().getFieldErrors().stream()
                .map(field -> Map.of("field", field.getField(), "message", "Must contain valid content." )).distinct().toList();
        return response(INVALID_CONTENT, headers, request, fields);
    }
    @Override protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var code = switch (status.value()) {
            case 400 -> INVALID_PARAMETER;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> INTERNAL_ERROR;
        };
        return response(code, headers, request, null);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> failure(Exception error, WebRequest request) {
        return response(problems.classify(error), HttpHeaders.EMPTY, request, null);
    }
    private ResponseEntity<Object> response(ProblemFactory.Code code, HttpHeaders original, WebRequest request, Object fieldErrors) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        var body = problems.create(code, path);
        if (fieldErrors != null) body.setProperty("fieldErrors", fieldErrors);
        var headers = new HttpHeaders(); headers.putAll(original);
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON); headers.setCacheControl("no-store");
        if (code == AUTHENTICATION_FAILED) headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return new ResponseEntity<>(body, headers, code.status());
    }
}
