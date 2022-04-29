package tech.phegy.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.phegy.api.model.user.PhegyRole;
import tech.phegy.api.model.user.PhegyRoleLevel;

@Repository
public interface PhegyRoleRepository extends JpaRepository<PhegyRole, Integer> {
    PhegyRole getByLevel(PhegyRoleLevel level);
}
