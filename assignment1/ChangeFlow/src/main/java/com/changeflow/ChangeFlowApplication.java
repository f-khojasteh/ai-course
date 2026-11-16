package com.changeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import com.changeflow.configuration.AdministrativeModeConfiguration;
import com.changeflow.configuration.LocalAdministrationRunner;
import java.io.PrintStream;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ChangeFlowApplication {

    public static void main(String[] args) {
        int status = execute(args, System.out, System.err);
        if (status != 0) System.exit(status);
    }

    public static int execute(String[] args, PrintStream output, PrintStream error) {
        try {
            var command = AdministrativeModeConfiguration.parse(args);
            if (command.isEmpty()) {
                SpringApplication.run(ChangeFlowApplication.class, args);
            } else {
                var application = new SpringApplication(ChangeFlowApplication.class);
                application.setWebApplicationType(WebApplicationType.NONE);
                var administrativeArgs = new java.util.ArrayList<>(java.util.Arrays.stream(args)
                        .filter(arg -> !arg.startsWith("--spring.main.web-application-type=")).toList());
                administrativeArgs.add("--spring.main.web-application-type=none");
                try (var context = application.run(administrativeArgs.toArray(String[]::new))) {
                    context.getBean(LocalAdministrationRunner.class).execute(command.get(), output);
                }
            }
            return 0;
        } catch (Exception exception) {
            error.println("ChangeFlow startup or administrative operation failed.");
            return 1;
        }
    }
}
