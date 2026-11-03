package dev.clebin.authforge.web.error;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("The refresh token is invalid, expired, or revoked.");
    }
}
