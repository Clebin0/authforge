package dev.clebin.authforge.web.error;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email or password is incorrect.");
    }
}
