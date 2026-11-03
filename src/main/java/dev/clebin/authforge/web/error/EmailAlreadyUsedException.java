package dev.clebin.authforge.web.error;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("An account already exists for this email address.");
    }
}
