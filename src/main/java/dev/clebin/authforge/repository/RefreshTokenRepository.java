package dev.clebin.authforge.repository;

import dev.clebin.authforge.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token join fetch token.user where token.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("hash") String hash);

    List<RefreshToken> findAllByFamilyIdAndRevokedAtIsNull(UUID familyId);

    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNull(UUID userId);

    Optional<RefreshToken> findByTokenHash(String hash);
}
