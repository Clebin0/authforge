package dev.clebin.authforge.config;

import dev.clebin.authforge.domain.Role;
import dev.clebin.authforge.domain.UserAccount;
import dev.clebin.authforge.repository.UserRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String password;

    public BootstrapAdminRunner(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        Clock clock,
        @Value("${app.bootstrap-admin.email:}") String email,
        @Value("${app.bootstrap-admin.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hasEmail = StringUtils.hasText(email);
        boolean hasPassword = StringUtils.hasText(password);

        if (!hasEmail && !hasPassword) {
            return;
        }
        if (!hasEmail || !hasPassword || password.length() < 16
            || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalStateException(
                "Set a valid BOOTSTRAP_ADMIN_EMAIL and a BOOTSTRAP_ADMIN_PASSWORD of at least 16 characters."
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(existing -> {
            if (!existing.getRoles().contains(Role.ADMIN)) {
                throw new IllegalStateException(
                    "The bootstrap admin email already belongs to a non-admin account."
                );
            }
        }, () -> userRepository.save(new UserAccount(
            normalizedEmail,
            "System Administrator",
            passwordEncoder.encode(password),
            Set.of(Role.ADMIN),
            clock.instant()
        )));
    }
}
