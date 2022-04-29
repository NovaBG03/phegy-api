package tech.phegy.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.phegy.api.model.points.PointsBag;

import java.util.Optional;

@Repository
public interface PointsBagRepository extends JpaRepository<PointsBag, Long> {
    Optional<PointsBag> findByUserUsername(String username);
}
