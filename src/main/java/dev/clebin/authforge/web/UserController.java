package dev.clebin.authforge.web;

import dev.clebin.authforge.service.AccountService;
import dev.clebin.authforge.web.dto.UserResponse;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AccountService accountService;

    public UserController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getUser(UUID.fromString(jwt.getSubject()));
    }
}
