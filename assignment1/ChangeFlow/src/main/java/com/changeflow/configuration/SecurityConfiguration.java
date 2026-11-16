package com.changeflow.configuration;
import com.changeflow.api.*;
import com.changeflow.application.CredentialAuthenticationService;
import com.changeflow.infrastructure.security.BearerAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, CredentialAuthenticationService authentication,
                                                 ProblemFactory problems, SecurityProblemHandlers handlers) throws Exception {
        // Construct directly in this chain: no servlet Filter bean registration and no double execution.
        var bearer = new BearerAuthenticationFilter(authentication,
                (request, response, error) -> problems.write(request, response, problems.classify(error)));
        return http.csrf(AbstractHttpConfigurer::disable) // Authorization header only; no ambient browser credentials
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class).build();
    }
}
