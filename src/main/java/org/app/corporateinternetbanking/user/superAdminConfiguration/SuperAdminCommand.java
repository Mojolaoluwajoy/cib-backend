package org.app.corporateinternetbanking.user.superAdminConfiguration;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.app.corporateinternetbanking.CorporateInternetBankingApplication;
import org.app.corporateinternetbanking.user.domain.entity.User;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

// One-off operator command, run instead of the server:
//
//   java -jar app.jar create-super-admin --email=you@example.com [--first-name=Ada] [--last-name=Lovelace]
//
// The password is typed at a prompt (or piped on stdin when there is no terminal), so it never appears in
// shell history, environment variables or logs. The app starts without listening on any port or running
// scheduled jobs, creates the account in the configured database and exits.
public final class SuperAdminCommand {

    public static final String NAME = "create-super-admin";
    static final String PROFILE = "cli";

    private SuperAdminCommand() {}

    public static int run(String[] args) {
        Map<String, String> options = parseOptions(args);
        String email = options.get("email");
        if (email == null) {
            System.err.println("Usage: java -jar app.jar " + NAME
                    + " --email=you@example.com [--first-name=Ada] [--last-name=Lovelace]");
            return 2;
        }

        char[] password = null;
        try {
            password = readPassword();
            if (password == null) {
                System.err.println("Passwords did not match.");
                return 1;
            }

            // server.port=-1 keeps the full (web + security) configuration but opens no HTTP port.
            System.setProperty("server.port", "-1");
            // Keep the console readable: no banner, SQL echo or debug report from the normal dev settings.
            System.setProperty("spring.jpa.show-sql", "false");
            System.setProperty("debug", "false");
            System.setProperty("logging.level.root", "WARN");

            SpringApplication app = new SpringApplication(CorporateInternetBankingApplication.class);
            app.setAdditionalProfiles(PROFILE);
            app.setBannerMode(Banner.Mode.OFF);
            app.setLogStartupInfo(false);
            try (ConfigurableApplicationContext context = app.run()) {
                SuperAdminBootstrap bootstrap = context.getBean(SuperAdminBootstrap.class);
                if (bootstrap.superAdminExists()) {
                    System.out.println("Note: a super admin already exists; adding another one.");
                }
                User created = bootstrap.create(email, options.get("first-name"), options.get("last-name"), password);
                System.out.println("Created SUPER_ADMIN " + created.getEmail() + ". You can now log in.");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("Could not create super admin: " + e.getMessage());
            return 1;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    // Prompts twice on a terminal; otherwise reads one line from stdin, e.g. `... < password.txt`.
    private static char[] readPassword() throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] first = console.readPassword("Password for the new super admin: ");
            char[] second = console.readPassword("Repeat password: ");
            boolean match = Arrays.equals(first, second);
            Arrays.fill(second, '\0');
            if (!match) {
                Arrays.fill(first, '\0');
                return null;
            }
            return first;
        }
        String line = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        return line == null ? new char[0] : line.toCharArray();
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                int split = arg.indexOf('=');
                options.put(arg.substring(2, split), arg.substring(split + 1));
            }
        }
        return options;
    }
}
