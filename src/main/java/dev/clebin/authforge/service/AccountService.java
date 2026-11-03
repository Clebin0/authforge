package dev.clebin.authforge.service;

import dev.clebin.authforge.domain.UserAccount;
import dev.clebin.authforge.repository.RefreshTokenRepository;
import dev.clebin.authforge.repository.UserRepository;
import dev.clebin.authforge.web.dto.UserResponse;
import dev.clebin.authforge.web.dto.UserStatusRequest;
import dev.clebin.authforge.web.error.UserNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public AccountService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        return userRepository.findById(id)
            .map(UserResponse::from)
            .orElseThrow(UserNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .map(UserResponse::from)
            .toList();
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UserStatusRequest request) {
        UserAccount user = userRepository.findById(id)
            .orElseThrow(UserNotFoundException::new);

        user.updateStatus(request.enabled(), request.locked(), clock.instant());

        if (Boolean.FALSE.equals(request.enabled()) || Boolean.TRUE.equals(request.locked())) {
            refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(id)
                .forEach(token -> token.revoke(clock.instant()));
        }

        return UserResponse.from(user);
    }
}
