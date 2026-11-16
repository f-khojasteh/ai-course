package com.changeflow.configuration;
import com.changeflow.application.LocalAdministrationService;
import java.io.PrintStream;

/** Exactly one offline operation. Only the successfully committed result is disclosed. */
public class LocalAdministrationRunner {
    private final LocalAdministrationService administration;
    public LocalAdministrationRunner(LocalAdministrationService administration) { this.administration = administration; }
    public void execute(AdministrativeModeConfiguration.Command command, PrintStream output) {
        LocalAdministrationService.Provisioned issued = null;
        switch (command.action()) {
            case "provision" -> issued = administration.provision(command.roles());
            case "issue-credential" -> issued = administration.issueCredential(command.userId());
            case "revoke-credential" -> administration.revokeCredential(command.credentialId());
            case "remove-role" -> administration.removeRole(command.userId(), command.role());
            default -> throw new IllegalArgumentException("Invalid administrative action.");
        }
        if (issued != null) {
            output.println("userId=" + issued.userId());
            output.println("credentialId=" + issued.credentialId());
            output.println("credential=" + issued.value());
        } else output.println("Operation committed.");
    }
}
