package tech.phegy.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.phegy.api.model.user.PhegyUser;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhegyUserRepository extends JpaRepository<PhegyUser, UUID> {
    Optional<PhegyUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
