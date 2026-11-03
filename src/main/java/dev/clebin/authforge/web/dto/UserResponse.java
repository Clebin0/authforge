package dev.clebin.authforge.web.dto;

import dev.clebin.authforge.domain.Role;
import dev.clebin.authforge.domain.UserAccount;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String fullName,
    Set<Role> roles,
    boolean enabled,
    boolean locked,
    Instant createdAt
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRoles(),
            user.isEnabled(),
            user.isLocked(),
            user.getCreatedAt()
        );
    }
}
