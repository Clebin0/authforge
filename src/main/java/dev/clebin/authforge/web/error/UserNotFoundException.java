package dev.clebin.authforge.web.error;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("The requested user does not exist.");
    }
}
