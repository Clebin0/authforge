package dev.clebin.authforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.clebin.authforge.service.TokenService;
import org.junit.jupiter.api.Test;

class TokenServiceTests {

    @Test
    void refreshTokenHashIsStableSha256Hex() {
        String hash = TokenService.sha256("opaque-refresh-token");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(hash, TokenService.sha256("opaque-refresh-token"));
        assertNotEquals("opaque-refresh-token", hash);
    }
}
