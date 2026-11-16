package com.changeflow.infrastructure.security;
import com.changeflow.application.CredentialAuthenticationService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

/** Header parsing and trusted principal integration; no resource/business decisions. */
public class BearerAuthenticationFilter extends OncePerRequestFilter {
    /** API supplies the shared writer through composition, keeping Infrastructure independent of API. */
    @FunctionalInterface public interface FailureWriter { void write(HttpServletRequest request, HttpServletResponse response, Exception error) throws IOException; }
    private final CredentialAuthenticationService authentication;
    private final FailureWriter failures;
    public BearerAuthenticationFilter(CredentialAuthenticationService authentication, FailureWriter failures) {
        this.authentication = authentication; this.failures = failures;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            var headers = Collections.list(request.getHeaders("Authorization"));
            if (headers.size() != 1) throw denied();
            String header = headers.get(0);
            if (header.length() < 8 || !header.regionMatches(true, 0, "Bearer ", 0, 7)
                    || header.substring(7).isBlank() || header.substring(7).contains(" ")) throw denied();
            var caller = authentication.authenticate(header.substring(7));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(caller, null, List.of()));
            SecurityContextHolder.setContext(context);
        } catch (Exception error) {
            SecurityContextHolder.clearContext(); failures.write(request, response, error); return;
        }
        // Downstream MVC errors belong to the MVC handler, never to authentication.
        chain.doFilter(request, response);
    }
    private com.changeflow.application.ApplicationFailure denied() {
        return new com.changeflow.application.ApplicationFailure(com.changeflow.application.ApplicationFailure.Code.AUTHENTICATION_FAILED);
    }
}
