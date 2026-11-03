package dev.clebin.authforge.web.error;

public class RefreshTokenReuseException extends RuntimeException {
    public RefreshTokenReuseException() {
        super("Refresh token reuse was detected. The token family has been revoked.");
    }
}
