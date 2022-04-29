package tech.phegy.api.service.points;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.phegy.api.model.points.PointsBag;
import tech.phegy.api.repository.PointsBagRepository;

import java.util.List;


@Service
public class PointsBagService {
    private final PointsBagRepository pointsBagRepository;

    public PointsBagService(PointsBagRepository pointsBagRepository) {
        this.pointsBagRepository = pointsBagRepository;
    }

    public PointsBag getPointsBag(String username) {
        return pointsBagRepository.findByUserUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Transactional
    public void transferPoints(String fromUsername, String toUsername, Double points) {
        PointsBag fromPointsBag = this.getPointsBag(fromUsername);
        PointsBag toPointsBag = this.getPointsBag(toUsername);
        fromPointsBag.removePoints(points);
        toPointsBag.addPoints(points);
        pointsBagRepository.saveAll(List.of(fromPointsBag, toPointsBag));
    }
}
