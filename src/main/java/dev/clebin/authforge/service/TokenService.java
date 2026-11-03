package dev.clebin.authforge.service;

import dev.clebin.authforge.domain.RefreshToken;
import dev.clebin.authforge.domain.UserAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
        JwtEncoder jwtEncoder,
        Clock clock,
        @Value("${app.jwt.issuer}") String issuer,
        @Value("${app.jwt.access-token-ttl:PT10M}") Duration accessTokenTtl,
        @Value("${app.jwt.refresh-token-ttl:P7D}") Duration refreshTokenTtl
    ) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;

        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()
            || refreshTokenTtl.compareTo(accessTokenTtl) <= 0) {
            throw new IllegalArgumentException("JWT token lifetimes are not configured safely.");
        }
    }

    public String createAccessToken(UserAccount user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenTtl))
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList())
            .claim("token_use", "access")
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public RefreshGrant createRefreshToken(UserAccount user, UUID familyId) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant now = clock.instant();
        RefreshToken entity = new RefreshToken(
            user,
            familyId,
            sha256(rawToken),
            now,
            now.plus(refreshTokenTtl)
        );
        return new RefreshGrant(rawToken, entity);
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public record RefreshGrant(String rawToken, RefreshToken entity) {
    }
}
