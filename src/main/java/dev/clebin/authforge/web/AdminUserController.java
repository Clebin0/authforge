package dev.clebin.authforge.web;

import dev.clebin.authforge.service.AccountService;
import dev.clebin.authforge.web.dto.UserResponse;
import dev.clebin.authforge.web.dto.UserStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AccountService accountService;

    public AdminUserController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return accountService.listUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return accountService.getUser(id);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UserStatusRequest request
    ) {
        return ResponseEntity.ok(accountService.updateStatus(id, request));
    }
}
