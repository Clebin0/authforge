package dev.clebin.authforge.service;

import dev.clebin.authforge.domain.RefreshToken;
import dev.clebin.authforge.domain.Role;
import dev.clebin.authforge.domain.UserAccount;
import dev.clebin.authforge.repository.RefreshTokenRepository;
import dev.clebin.authforge.repository.UserRepository;
import dev.clebin.authforge.service.TokenService.RefreshGrant;
import dev.clebin.authforge.web.dto.AuthResponse;
import dev.clebin.authforge.web.dto.LoginRequest;
import dev.clebin.authforge.web.dto.RegisterRequest;
import dev.clebin.authforge.web.dto.RefreshRequest;
import dev.clebin.authforge.web.dto.UserResponse;
import dev.clebin.authforge.web.error.EmailAlreadyUsedException;
import dev.clebin.authforge.web.error.InvalidCredentialsException;
import dev.clebin.authforge.web.error.InvalidRefreshTokenException;
import dev.clebin.authforge.web.error.RefreshTokenReuseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        TokenService tokenService,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException();
        }

        UserAccount user = new UserAccount(
            email,
            request.fullName().trim(),
            passwordEncoder.encode(request.password()),
            Set.of(Role.USER),
            clock.instant()
        );
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled() || user.isLocked()
            || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    @Transactional(noRollbackFor = {
        InvalidRefreshTokenException.class,
        RefreshTokenReuseException.class
    })
    public AuthResponse refresh(RefreshRequest request) {
        String hash = TokenService.sha256(request.refreshToken());
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash)
            .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = clock.instant();
        if (current.getUsedAt() != null) {
            revokeFamily(current.getFamilyId(), now);
            throw new RefreshTokenReuseException();
        }

        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        RefreshGrant replacement = tokenService.createRefreshToken(
            current.getUser(),
            current.getFamilyId()
        );
        current.markUsed(now, replacement.entity().getTokenHash());
        refreshTokenRepository.save(replacement.entity());

        return responseFor(current.getUser(), replacement.rawToken());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        String hash = TokenService.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHashForUpdate(hash).ifPresent(token ->
            revokeFamily(token.getFamilyId(), clock.instant())
        );
    }

    private AuthResponse issueTokens(UserAccount user) {
        UUID familyId = UUID.randomUUID();
        RefreshGrant refresh = tokenService.createRefreshToken(user, familyId);
        refreshTokenRepository.save(refresh.entity());
        return responseFor(user, refresh.rawToken());
    }

    private AuthResponse responseFor(UserAccount user, String rawRefreshToken) {
        return new AuthResponse(
            tokenService.createAccessToken(user),
            rawRefreshToken,
            "Bearer",
            tokenService.accessTokenTtl().toSeconds(),
            UserResponse.from(user)
        );
    }

    private void revokeFamily(UUID familyId, Instant now) {
        refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId)
            .forEach(token -> token.revoke(now));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
