package tech.phegy.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.phegy.api.model.token.RefreshToken;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> getByHashedToken(String hashedToken);

    int countAllByUserUsername(String username);

    Collection<RefreshToken> getAllByUserUsername(String username);
}
