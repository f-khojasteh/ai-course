package com.changeflow.api;
import jakarta.servlet.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.*;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class SecurityProblemHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ProblemFactory problems;
    public SecurityProblemHandlers(ProblemFactory problems) { this.problems = problems; }
    @Override public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException error) throws IOException {
        problems.write(request, response, ProblemFactory.Code.AUTHENTICATION_FAILED);
    }
    @Override public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException error) throws IOException {
        problems.write(request, response, ProblemFactory.Code.PERMISSION_DENIED);
    }
}
