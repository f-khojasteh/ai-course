package com.changeflow.configuration;

import com.changeflow.application.LocalAdministrationService;
import com.changeflow.domain.UserRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import java.util.*;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "changeflow.mode", havingValue = "admin")
public class AdministrativeModeConfiguration {
    @Bean LocalAdministrationRunner localAdministrationRunner(LocalAdministrationService service) {
        return new LocalAdministrationRunner(service);
    }
    public record Command(String action, UUID userId, UUID credentialId, Set<UserRole> roles, UserRole role) {
        public Command { roles = Set.copyOf(roles); }
    }
    /** Validate mode and all command fields before creating any application context. */
    public static Optional<Command> parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--changeflow.")) continue;
            int equals = arg.indexOf('=');
            if (equals < 0 || options.putIfAbsent(arg.substring(2, equals), arg.substring(equals + 1)) != null) throw invalid();
        }
        if (options.isEmpty()) return Optional.empty();
        if (!"admin".equals(options.remove("changeflow.mode"))) throw invalid();
        String action = options.remove("changeflow.action");
        if (action == null) throw invalid();
        UUID user = null, credential = null;
        Set<UserRole> roles = Set.of(); UserRole role = null;
        try {
            switch (action) {
                case "provision" -> {
                    String selected = options.remove("changeflow.roles");
                    if (selected == null) throw invalid();
                    if (!selected.equals("None")) {
                        var parsed = EnumSet.noneOf(UserRole.class);
                        for (String name : selected.split(",", -1)) if (!parsed.add(UserRole.valueOf(name))) throw invalid();
                        roles = Set.copyOf(parsed);
                    }
                }
                case "issue-credential" -> user = uuid(options.remove("changeflow.user-id"));
                case "revoke-credential" -> credential = uuid(options.remove("changeflow.credential-id"));
                case "remove-role" -> {
                    user = uuid(options.remove("changeflow.user-id"));
                    role = UserRole.valueOf(options.remove("changeflow.role"));
                }
                default -> throw invalid();
            }
        } catch (IllegalArgumentException | NullPointerException exception) { throw invalid(); }
        if (!options.isEmpty()) throw invalid();
        return Optional.of(new Command(action, user, credential, roles, role));
    }
    private static UUID uuid(String value) {
        UUID id = UUID.fromString(value);
        if (!id.toString().equals(value)) throw invalid();
        return id;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid administrative arguments."); }
}
